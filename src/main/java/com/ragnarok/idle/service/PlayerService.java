package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Hero;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.domain.Saga;
import com.ragnarok.idle.domain.SagaRank;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.PlayerHeroView;
import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.economy.BulkPurchase;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.economy.PurchaseMode;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.HeroRepository;
import com.ragnarok.idle.repository.PlayerHeroRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {

    /** Потолок офлайн-дохода (GDD §12.5). */
    private static final long OFFLINE_CAP_SECONDS = 12 * 3600;

    /**
     * Пауза ≤ этого порога считается "игрок в игре" (фронт поллит /player/me раз в 1с):
     * DPS героев наносится как урон и двигает уровни. Дольше — офлайн: только золото.
     */
    private static final long ONLINE_TICK_MAX_SECONDS = 60;

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;
    private final HeroRepository heroRepository;
    private final PlayerHeroRepository playerHeroRepository;
    private final HeroService heroService;
    private final CombatEngine combatEngine;

    public PlayerService(PlayerRepository playerRepository, AvatarRepository avatarRepository,
                          HeroRepository heroRepository, PlayerHeroRepository playerHeroRepository,
                          HeroService heroService, CombatEngine combatEngine) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
        this.heroRepository = heroRepository;
        this.playerHeroRepository = playerHeroRepository;
        this.heroService = heroService;
        this.combatEngine = combatEngine;
    }

    @Transactional
    public PlayerStateResponse getState(String username) {
        return getState(username, PurchaseMode.X1, BigNum.ZERO);
    }

    /** @param mode режим мультипокупки для расчёта цены пачки на кнопках апгрейда (герои + тап Аватара). */
    @Transactional
    public PlayerStateResponse getState(String username, PurchaseMode mode) {
        return getState(username, mode, BigNum.ZERO);
    }

    /** @param extraAsh Слава, дропнутый ВНЕ этого тика (напр. большой батч {@link #skipTime}) — добавляется к ashDropCollected. */
    private PlayerStateResponse getState(String username, PurchaseMode mode, BigNum extraAsh) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));

        TickResult tick = applyPassiveDps(player);
        BigNum offlineGold = tick.offlineGold();
        BigNum ashDropCollected = tick.ashGained().add(extraAsh);
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));

        Map<Long, PlayerHero> owned = playerHeroRepository.findByPlayerId(player.getId()).stream()
                .collect(Collectors.toMap(PlayerHero::getHeroId, Function.identity()));
        Map<Long, BigNum> dpsByHero = heroService.passiveDpsByHero(player.getId());

        List<PlayerHeroView> heroViews = heroRepository.findAll().stream()
                .sorted(Comparator.comparing(Hero::getId))
                .map(hero -> {
                    PlayerHero playerHero = owned.get(hero.getId());
                    // некупленным показываем базовый DPS 1-го уровня — видно, что поздние герои сильнее
                    BigNum heroDps = playerHero != null
                            ? dpsByHero.getOrDefault(hero.getId(), BigNum.ZERO)
                            : hero.getBaseDps();
                    long level = playerHero != null ? playerHero.getLevel() : 0L;

                    // Сага (GDD §3.8): у некупленных — стартовый ранг 1 «Обычный» I для предпросмотра.
                    int sagaRank = playerHero != null ? playerHero.getSagaRank() : Saga.FIRST_RANK;
                    int sagaSubStep = playerHero != null ? playerHero.getSagaSubStep() : Saga.FIRST_SUB_STEP;
                    SagaRank rank = SagaRank.byRank(sagaRank);
                    boolean atLevelCap = playerHero != null && level >= rank.getLevelCap();

                    // Цена пачки под выбранный режим — считаем только для купленных (у остальных кнопка «Купить»).
                    BulkView bulk = playerHero != null
                            ? bulkView(lvl -> HeroService.upgradeCostFrom(hero, lvl),
                                    level, mode, player.getGold(), rank.getLevelCap())
                            : BulkView.NONE;

                    return new PlayerHeroView(
                            hero.getId(),
                            hero.getName(),
                            hero.getType().name(),
                            playerHero != null,
                            level,
                            BigNumDto.from(hero.getPrice()),
                            BigNumDto.from(HeroService.upgradeCostFrom(hero, Math.max(level, 1))),
                            BigNumDto.from(heroDps),
                            sagaRank,
                            rank.getDisplayName(),
                            sagaSubStep,
                            Saga.toRoman(sagaSubStep),
                            rank.getColor(),
                            rank.getLevelCap(),
                            atLevelCap,
                            bulk.levels(),
                            BigNumDto.from(bulk.cost()),
                            bulk.affordable()
                    );
                })
                .toList();

        long hero15Level = owned.containsKey(RebirthService.REBIRTH_GATE_HERO_ID)
                ? owned.get(RebirthService.REBIRTH_GATE_HERO_ID).getLevel()
                : 0L;
        boolean rebirthReady = hero15Level >= RebirthService.REBIRTH_GATE_HERO_LEVEL;
        String rebirthHint = rebirthReady ? null
                : "Нужен Ледяной ётун %d ур. (сейчас %d)".formatted(RebirthService.REBIRTH_GATE_HERO_LEVEL, hero15Level);

        // Цена пачки улучшений тапа Аватара под выбранный режим (у Аватара нет потолка уровня).
        BulkView tapBulk = bulkView(AvatarService::upgradeCostFrom,
                avatar.getTapDamageLevel(), mode, player.getGold(), Long.MAX_VALUE);

        return new PlayerStateResponse(
                player.getCurrentLevel(),
                player.getMaxLevel(),
                player.getCurrentSubLevel(),
                BigNumDto.from(player.getCurrentMobHp()),
                BigNumDto.from(player.getGold()),
                BigNumDto.from(player.getAsh()),
                BigNumDto.from(offlineGold),
                avatar.getTapDamageLevel(),
                avatar.getAutotapLevel(),
                BigNumDto.from(BattleService.tapDamage(avatar)),
                BigNumDto.from(AvatarService.upgradeCostFrom(avatar.getTapDamageLevel())),
                player.isAutoAdvance(),
                bossTimeLeftSeconds(player),
                heroViews,
                rebirthReady,
                rebirthHint,
                BigNumDto.from(ashDropCollected),
                tapBulk.levels(),
                BigNumDto.from(tapBulk.cost()),
                tapBulk.affordable()
        );
    }

    /**
     * Считает цену пачки под режим для показа на кнопке (сервер — единственный источник цены, античит).
     * Доступность: уровней &gt; 0, золота хватает на всю пачку и она не пробивает потолок (стену Саги).
     */
    private BulkView bulkView(LongFunction<BigNum> costOf, long level, PurchaseMode mode, BigNum gold, long cap) {
        BulkPurchase.Quote quote = BulkPurchase.quote(costOf, level, mode, gold, cap);
        boolean affordable = quote.levels() > 0
                && gold.gte(quote.totalCost())
                && level + quote.levels() <= cap;
        return new BulkView(quote.levels(), quote.totalCost(), affordable);
    }

    /** Цена пачки для UI: сколько уровней, суммарная цена и доступна ли покупка целиком. */
    private record BulkView(long levels, BigNum cost, boolean affordable) {
        static final BulkView NONE = new BulkView(0, BigNum.ZERO, false);
    }

    /** Остаток таймера босса для UI; null — игрок не на боссовом уровне. */
    private Long bossTimeLeftSeconds(Player player) {
        if (!CombatEngine.isBossLevel(player.getCurrentLevel()) || player.getBossStartedAt() == null) {
            return null;
        }
        long elapsed = Duration.between(player.getBossStartedAt(), LocalDateTime.now()).getSeconds();
        return Math.max(0, CombatEngine.BOSS_TIME_LIMIT_SECONDS - elapsed);
    }

    /**
     * ТЕСТОВАЯ прокрутка времени: мгновенно начисляет idle-прогресс, как будто прошло hours часов
     * (с потолком офлайна 12ч, GDD §12.5). DPS прогоняется ударами через CombatEngine — золото
     * начисляется И уровни проходятся. Позже станет игровой механикой за Яблоки Идунн.
     */
    @Transactional
    public PlayerStateResponse skipTime(String username, int hours) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));

        combatEngine.syncBossTimer(player, LocalDateTime.now());
        BigNum totalPassiveDps = heroService.totalPassiveDps(player.getId());
        long cappedSeconds = Math.min(hours * 3600L, OFFLINE_CAP_SECONDS);
        BigNum ashGained = BigNum.ZERO;
        if (!totalPassiveDps.isZero()) {
            // каждая секунда DPS — отдельный удар; таймер босса идёт внутри симуляции
            ashGained = combatEngine.applyHits(player, totalPassiveDps, cappedSeconds, true).ashGained();
        }
        player.setLastCollectedAt(LocalDateTime.now());
        playerRepository.save(player);

        return getState(username, PurchaseMode.X1, ashGained);
    }

    /** Результат тика пассивного DPS: офлайн-золото и Слава, дропнутый (если) во время онлайн-тика. */
    private record TickResult(BigNum offlineGold, BigNum ashGained) {
        static final TickResult NONE = new TickResult(BigNum.ZERO, BigNum.ZERO);
    }

    /**
     * Пассивный DPS героев (GDD §3.5) за время с прошлого обращения:
     * - короткая пауза (≤{@link #ONLINE_TICK_MAX_SECONDS}) — DPS наносится уроном через
     *   {@link CombatEngine}: убивает мобов/боссов и двигает уровни, как тапы (в т.ч. может
     *   дропнуть Слава с обычных мобов, если {@code rebirthCount >= 1});
     * - длинная пауза (офлайн) — только золото по текущему уровню с потолком 12ч, уровни офлайн
     *   не двигаются (GDD §12.5); дискретных убийств там нет, поэтому дроп Славы не работает.
     */
    private TickResult applyPassiveDps(Player player) {
        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = Duration.between(player.getLastCollectedAt(), now).getSeconds();
        player.setLastCollectedAt(now);
        combatEngine.syncBossTimer(player, now);

        if (elapsedSeconds <= 0) {
            return TickResult.NONE;
        }

        BigNum totalPassiveDps = heroService.totalPassiveDps(player.getId());
        if (totalPassiveDps.isZero()) {
            return TickResult.NONE;
        }

        if (elapsedSeconds <= ONLINE_TICK_MAX_SECONDS) {
            // каждая секунда DPS — отдельный удар (без переноса урона между мобами)
            BigNum ashGained = combatEngine.applyHits(player, totalPassiveDps, elapsedSeconds, true).ashGained();
            return new TickResult(BigNum.ZERO, ashGained);
        }

        long cappedSeconds = Math.min(elapsedSeconds, OFFLINE_CAP_SECONDS);
        long level = player.getCurrentLevel();

        // Офлайн DPS "добивает" мобов текущего уровня без продвижения:
        // золото/сек = DPS × (золото за моба / HP моба) на замороженном уровне.
        BigNum goldPerSecond = totalPassiveDps
                .multiply(EconomyCurves.goldPerMob(level))
                .divide(EconomyCurves.mobHp(level));
        BigNum offlineGold = goldPerSecond.multiply(cappedSeconds).floor();

        player.setGold(player.getGold().add(offlineGold));
        return new TickResult(offlineGold, BigNum.ZERO);
    }
}
