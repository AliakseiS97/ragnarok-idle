package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Hero;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.PlayerHeroView;
import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.economy.EconomyCurves;
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
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));

        BigNum offlineGold = applyPassiveDps(player);
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
                    return new PlayerHeroView(
                            hero.getId(),
                            hero.getName(),
                            hero.getType().name(),
                            playerHero != null,
                            level,
                            BigNumDto.from(hero.getPrice()),
                            BigNumDto.from(HeroService.upgradeCostFrom(hero, Math.max(level, 1))),
                            BigNumDto.from(heroDps)
                    );
                })
                .toList();

        long hero15Level = owned.containsKey(RebirthService.REBIRTH_GATE_HERO_ID)
                ? owned.get(RebirthService.REBIRTH_GATE_HERO_ID).getLevel()
                : 0L;
        boolean rebirthReady = hero15Level >= RebirthService.REBIRTH_GATE_HERO_LEVEL;
        String rebirthHint = rebirthReady ? null
                : "Нужен Ледяной ётун %d ур. (сейчас %d)".formatted(RebirthService.REBIRTH_GATE_HERO_LEVEL, hero15Level);

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
                rebirthHint
        );
    }

    /** Остаток таймера босса для UI; null — игрок не на боссе. */
    private Long bossTimeLeftSeconds(Player player) {
        if (!CombatEngine.isBossSlot(player.getCurrentLevel(), player.getCurrentSubLevel())
                || player.getBossStartedAt() == null) {
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
        if (!totalPassiveDps.isZero()) {
            // каждая секунда DPS — отдельный удар; таймер босса идёт внутри симуляции
            combatEngine.applyHits(player, totalPassiveDps, cappedSeconds, true);
        }
        player.setLastCollectedAt(LocalDateTime.now());
        playerRepository.save(player);

        return getState(username);
    }

    /**
     * Пассивный DPS героев (GDD §3.5) за время с прошлого обращения:
     * - короткая пауза (≤{@link #ONLINE_TICK_MAX_SECONDS}) — DPS наносится уроном через
     *   {@link CombatEngine}: убивает мобов/боссов и двигает уровни, как тапы;
     * - длинная пауза (офлайн) — только золото по текущему уровню с потолком 12ч,
     *   уровни офлайн не двигаются (GDD §12.5).
     * Возвращает начисленное офлайн-золото (0 для онлайн-тика — его золото уже в player.gold).
     */
    private BigNum applyPassiveDps(Player player) {
        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = Duration.between(player.getLastCollectedAt(), now).getSeconds();
        player.setLastCollectedAt(now);
        combatEngine.syncBossTimer(player, now);

        if (elapsedSeconds <= 0) {
            return BigNum.ZERO;
        }

        BigNum totalPassiveDps = heroService.totalPassiveDps(player.getId());
        if (totalPassiveDps.isZero()) {
            return BigNum.ZERO;
        }

        if (elapsedSeconds <= ONLINE_TICK_MAX_SECONDS) {
            // каждая секунда DPS — отдельный удар (без переноса урона между мобами)
            combatEngine.applyHits(player, totalPassiveDps, elapsedSeconds, true);
            return BigNum.ZERO;
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
        return offlineGold;
    }
}
