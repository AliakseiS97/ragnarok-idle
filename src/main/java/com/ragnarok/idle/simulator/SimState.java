package com.ragnarok.idle.simulator;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.HeroType;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.Saga;
import com.ragnarok.idle.domain.SagaRank;
import com.ragnarok.idle.economy.BulkPurchase;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.economy.PurchaseMode;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.service.AvatarService;
import com.ragnarok.idle.service.BattleService;
import com.ragnarok.idle.service.CombatEngine;
import com.ragnarok.idle.service.RebirthService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Состояние виртуального игрока и вся игровая механика симулятора.
 *
 * <p>ПЕРЕИСПОЛЬЗУЕТ реальные классы проекта напрямую (чистая математика, без Spring/БД):
 * {@link CombatEngine} (бой/золото/переходы уровней), {@link EconomyCurves} (HP/золото),
 * {@link BattleService#tapDamage} (урон тапа), {@link AvatarService#upgradeCostFrom} (цена тапа),
 * {@link Saga}/{@link SagaRank} (множители и стены), {@link BulkPurchase} (max-покупка).
 *
 * <p>ЗЕРКАЛИТ (с ссылками на источник) только то, что заперто в БД/private/instance-методах:
 * агрегацию DPS героев, цену апгрейда героя, формулу Славы и сид героев.
 */
public class SimState {

    // --- Зеркала констант HeroService (HeroService.java) — синхронизировать при смене баланса ---
    /** mirrors HeroService.DPS_GROWTH (HeroService.java L31): +10%/уровень героя. */
    private static final double DPS_GROWTH = 1.10;
    /** mirrors HeroService.BAFFER_MILESTONE_LEVEL (L33). */
    private static final long BAFFER_MILESTONE_LEVEL = 25;
    /** mirrors HeroService.BAFFER_MILESTONE_BONUS (L35). */
    private static final double BAFFER_MILESTONE_BONUS = 0.25;
    /** mirrors HeroService.SPECIAL_BAFFER_MULT (L36). */
    private static final double SPECIAL_BAFFER_MULT = 3.0;
    /** mirrors HeroService.ASH_DPS_STEP (L38): +0.3% общего DPS за единицу Славы. */
    private static final double ASH_DPS_STEP = 0.003;
    /** mirrors HeroService.UPGRADE_COST_GROWTH (L40): цена апгрейда героя ×1.07/уровень. */
    private static final double UPGRADE_COST_GROWTH = 1.07;

    // --- Зеркала констант RebirthService (RebirthService.java) ---
    /** mirrors RebirthService.ASH_EXPONENT (L27). */
    private static final double ASH_EXPONENT = 0.0009;
    /** mirrors RebirthService.ASH_BOOST (L30). */
    private static final double ASH_BOOST = 2.5;

    /** Купленный герой: уровень + ранг/под-ступень Саги. */
    static final class Owned {
        final SimHero hero;
        long level;
        int sagaRank = Saga.FIRST_RANK;
        int sagaSubStep = Saga.FIRST_SUB_STEP;

        Owned(SimHero hero, long level) {
            this.hero = hero;
            this.level = level;
        }

        Owned copy() {
            Owned c = new Owned(hero, level);
            c.sagaRank = sagaRank;
            c.sagaSubStep = sagaSubStep;
            return c;
        }
    }

    private final Player player = new Player();
    private final Avatar avatar = new Avatar();
    private final Map<Integer, Owned> owned = new LinkedHashMap<>();
    private final CombatEngine engine;
    private final int clicksPerSecond;

    public SimState(int clicksPerSecond, java.util.function.DoubleSupplier ashDropRoll) {
        this.clicksPerSecond = clicksPerSecond;
        this.engine = new CombatEngine(ashDropRoll);   // реальный боевой движок, без Spring
        // стартовое состояние = как у нового игрока (AuthService.register): ур.1, 0 золота, тап ур.1
        player.setCurrentLevel(1L);
        player.setMaxLevel(1L);
        player.setGold(BigNum.ZERO);
        player.setAsh(BigNum.ZERO);
        player.setCurrentSubLevel(1);
        player.setCurrentMobHp(EconomyCurves.mobHp(1));
        player.setRebirthCount(0L);
        player.setAutoAdvance(true);
        player.setBossStartedAt(null);
        player.setLastCollectedAt(LocalDateTime.now());
        avatar.setTapDamageLevel(1L);
        avatar.setAutotapLevel(0L);
    }

    // ---------- Показатели ----------

    public BigNum gold() {
        return player.getGold();
    }

    public BigNum ash() {
        return player.getAsh();
    }

    public long currentLevel() {
        return player.getCurrentLevel();
    }

    public long maxLevel() {
        return player.getMaxLevel();
    }

    /** Задать стартовую Славу (для сравнения прохождений в тесте); в норме копится через ребёрты. */
    public void injectAsh(BigNum ash) {
        player.setAsh(ash);
    }

    public long rebirthCount() {
        return player.getRebirthCount();
    }

    public boolean autoAdvance() {
        return player.isAutoAdvance();
    }

    public int clicksPerSecond() {
        return clicksPerSecond;
    }

    /** Урон одного тапа — реальный {@link BattleService#tapDamage}. */
    public BigNum tapDamage() {
        return BattleService.tapDamage(avatar);
    }

    /** Урон тапа при гипотетическом уровне (для метрики апгрейда), без мутации — reuse BattleService. */
    public BigNum tapDamageAtLevel(long level) {
        Avatar probe = new Avatar();
        probe.setTapDamageLevel(level);
        return BattleService.tapDamage(probe);
    }

    /** Суммарный пассивный DPS команды. mirrors HeroService.passiveDpsByHero (HeroService.java L59-104). */
    public BigNum totalDps() {
        return totalDpsFor(owned.values(), player.getAsh());
    }

    private BigNum totalDpsFor(Collection<Owned> heroes, BigNum ashAmount) {
        if (heroes.isEmpty()) {
            return BigNum.ZERO;
        }
        double bafferBonus = 0;
        boolean specialActive = false;
        List<BigNum> rawDps = new ArrayList<>();
        for (Owned o : heroes) {
            // heroDPS = baseDPS × 1.10^(level-1) × sagaMult (HeroService L77-80)
            BigNum sagaMult = Saga.dpsMultiplier(o.sagaRank, o.sagaSubStep);
            rawDps.add(o.hero.baseDps().multiply(BigNum.of(DPS_GROWTH).pow(o.level - 1)).multiply(sagaMult));
            if (o.hero.type() == HeroType.BAFFER) {
                if (o.level >= BAFFER_MILESTONE_LEVEL) {
                    bafferBonus += BAFFER_MILESTONE_BONUS;                 // HeroService L83-85
                }
                if (o.hero.specialBaffer()) {
                    specialActive = true;                                  // HeroService L87-89
                }
            }
        }
        if (specialActive) {
            bafferBonus *= SPECIAL_BAFFER_MULT;                            // HeroService L93-95
        }
        double teamMult = 1 + bafferBonus;                                 // HeroService L96
        BigNum globalMult = BigNum.ONE.add(ashAmount.multiply(ASH_DPS_STEP)); // HeroService L98-99
        BigNum total = BigNum.ZERO;
        for (BigNum d : rawDps) {
            // per-hero округление до ближайшего целого (add 0.5, floor) — HeroService L103
            total = total.add(d.multiply(teamMult).multiply(globalMult).add(BigNum.of(0.5)).floor());
        }
        return total;
    }

    /** Эффективный урон в секунду = пассивный DPS + клики×урон_тапа (для оценки босса/апгрейдов). */
    public BigNum perSecondDamage() {
        return totalDps().add(tapDamage().multiply(clicksPerSecond));
    }

    // ---------- Боевой тик (1 игровая секунда) ----------

    /** Прогоняет 1 секунду боя через реальный {@link CombatEngine}: клики + пассивный DPS. */
    public void stepOneSecond() {
        BigNum tapDmg = tapDamage();
        BigNum dps = totalDps();
        // клики — как тапы (не двигают таймер босса): BattleService.tap -> applyHits(..., false)
        if (clicksPerSecond > 0 && !tapDmg.isZero()) {
            engine.applyHits(player, tapDmg, clicksPerSecond, false);
        }
        // 1 секунда пассивного DPS — как онлайн-тик PlayerService.applyPassiveDps (L251)
        if (!dps.isZero()) {
            engine.applyHits(player, dps, 1, true);
        }
    }

    /** Переход на уровень через реальный {@link CombatEngine#enterLevel} (ставит HP/таймер/maxLevel). */
    public void enterLevel(long level, boolean autoAdvance) {
        player.setAutoAdvance(autoAdvance);
        engine.enterLevel(player, level);
    }

    /** Сброс HP босса на полное — как {@link CombatEngine} после провала таймера (failBossFight, L83-86). */
    public void resetBoss(long level) {
        player.setCurrentMobHp(EconomyCurves.bossHp(level));
    }

    // ---------- Покупки ----------

    public boolean owns(int heroId) {
        return owned.containsKey(heroId);
    }

    public long heroLevel(int heroId) {
        Owned o = owned.get(heroId);
        return o == null ? 0 : o.level;
    }

    /** Цена апгрейда героя с уровня level. mirrors HeroService.upgradeCostFrom (HeroService.java L134-135). */
    private BigNum heroUpgradeCost(SimHero hero, long level) {
        return hero.price().multiply(BigNum.of(UPGRADE_COST_GROWTH).pow(level - 1)).ceil();
    }

    /** Покупка нового героя за золото (если хватает). Возвращает успех. */
    public boolean buyHero(SimHero hero) {
        if (owned.containsKey(hero.id()) || player.getGold().lt(hero.price())) {
            return false;
        }
        player.setGold(player.getGold().subtract(hero.price()));
        owned.put(hero.id(), new Owned(hero, 1));
        return true;
    }

    /** Max-покупка апгрейдов героя в пределах стены Саги (reuse BulkPurchase). Возвращает купленные уровни. */
    public long maxUpgradeHero(int heroId) {
        Owned o = owned.get(heroId);
        if (o == null) {
            return 0;
        }
        long cap = Saga.levelCap(o.sagaRank);
        BulkPurchase.Quote q = BulkPurchase.quote(lvl -> heroUpgradeCost(o.hero, lvl),
                o.level, PurchaseMode.MAX, player.getGold(), cap);
        if (q.levels() == 0) {
            return 0;
        }
        player.setGold(player.getGold().subtract(q.totalCost()));
        o.level += q.levels();
        return q.levels();
    }

    /** Апгрейд героя ровно на N уровней (для целевых покупок, напр. добить героя 15 до 125). */
    public long upgradeHero(int heroId, long levels) {
        Owned o = owned.get(heroId);
        if (o == null || levels <= 0) {
            return 0;
        }
        long cap = Saga.levelCap(o.sagaRank);
        levels = Math.min(levels, cap - o.level);
        BigNum cost = BigNum.ZERO;
        for (long i = 0; i < levels; i++) {
            cost = cost.add(heroUpgradeCost(o.hero, o.level + i));
        }
        if (levels <= 0 || player.getGold().lt(cost)) {
            return 0;
        }
        player.setGold(player.getGold().subtract(cost));
        o.level += levels;
        return levels;
    }

    /** Max-покупка апгрейда тапа Аватара (reuse AvatarService.upgradeCostFrom + BulkPurchase). */
    public long maxUpgradeTap() {
        BulkPurchase.Quote q = BulkPurchase.quote(AvatarService::upgradeCostFrom,
                avatar.getTapDamageLevel(), PurchaseMode.MAX, player.getGold(), Long.MAX_VALUE);
        if (q.levels() == 0) {
            return 0;
        }
        player.setGold(player.getGold().subtract(q.totalCost()));
        avatar.setTapDamageLevel(avatar.getTapDamageLevel() + q.levels());
        return q.levels();
    }

    /**
     * Пробивает стену Саги, если герой упёрся в потолок ранга: до следующего ранга (под-ступень→I).
     * mirrors SagaService.promote (SagaService.java) + Saga.levelCap-стена. Возвращает новый ранг или 0.
     */
    public int promoteSagaIfAtCap(int heroId) {
        Owned o = owned.get(heroId);
        if (o == null || o.level < Saga.levelCap(o.sagaRank)) {
            return 0;
        }
        SagaRank rank = SagaRank.byRank(o.sagaRank);
        if (rank.isMax()) {
            return 0; // ранг 7 — дальше стена жёсткая
        }
        o.sagaRank = rank.next().rank();
        o.sagaSubStep = Saga.FIRST_SUB_STEP;
        return o.sagaRank;
    }

    // ---------- Проекции DPS (для метрики «прирост DPS / цена») ----------

    /** Каким стал бы totalDps, если героя heroId прокачать до level (без мутации состояния). */
    public BigNum dpsIfHeroAtLevel(int heroId, long level) {
        List<Owned> projected = new ArrayList<>();
        for (Owned o : owned.values()) {
            if (o.hero.id() == heroId) {
                Owned c = o.copy();
                c.level = level;
                projected.add(c);
            } else {
                projected.add(o);
            }
        }
        return totalDpsFor(projected, player.getAsh());
    }

    /** Каким стал бы totalDps при покупке нового героя (ур.1). */
    public BigNum dpsIfHeroAdded(SimHero hero) {
        List<Owned> projected = new ArrayList<>(owned.values());
        projected.add(new Owned(hero, 1));
        return totalDpsFor(projected, player.getAsh());
    }

    public BigNum heroNextCost(int heroId) {
        Owned o = owned.get(heroId);
        return o == null ? BigNum.ZERO : heroUpgradeCost(o.hero, o.level);
    }

    public BigNum tapNextCost() {
        return AvatarService.upgradeCostFrom(avatar.getTapDamageLevel());
    }

    public long tapLevel() {
        return avatar.getTapDamageLevel();
    }

    public Collection<Owned> ownedHeroes() {
        return owned.values();
    }

    // ---------- Перерождение ----------

    /** Готов ли ребёрт: герой 15 куплен и прокачан до 125 (reuse RebirthService constants). */
    public boolean rebirthReady() {
        return heroLevel((int) RebirthService.REBIRTH_GATE_HERO_ID) >= RebirthService.REBIRTH_GATE_HERO_LEVEL;
    }

    /**
     * Перерождение: Слава += floor(mobHp(maxLevel)^0.0009 × 2.5), сброс забега (золото→0, ур.→1,
     * герои теряются, тап→1), maxLevel сохраняется. mirrors RebirthService.rebirth (L48-92).
     * Возвращает полученный Слава.
     */
    public BigNum rebirth() {
        BigNum ashGained = EconomyCurves.mobHp(player.getMaxLevel())
                .pow(ASH_EXPONENT).multiply(ASH_BOOST).floor();          // RebirthService L62-65
        player.setAsh(player.getAsh().add(ashGained));
        player.setRebirthCount(player.getRebirthCount() + 1);
        player.setGold(BigNum.ZERO);
        player.setCurrentLevel(1L);
        player.setCurrentSubLevel(1);
        player.setCurrentMobHp(EconomyCurves.mobHp(1));
        player.setBossStartedAt(null);
        player.setAutoAdvance(true);
        owned.clear();                                                    // герои теряются (L83)
        avatar.setTapDamageLevel(1L);                                     // L87
        avatar.setAutotapLevel(0L);
        return ashGained;
    }
}
