package com.ragnarok.idle.simulator;

import com.ragnarok.idle.domain.Saga;
import com.ragnarok.idle.domain.SagaRank;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.service.RebirthService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Стратегия покупок виртуального игрока (вызывается раз в N тиков):
 * <ol>
 *   <li>пробить стены Саги у всех купленных героев (как только доступно);</li>
 *   <li>приоритет ребёрта — купить героя 15 и довести до 125 уровня;</li>
 *   <li>жадно тратить остаток золота на лучший по метрике (прирост DPS / цена) апгрейд, max-покупкой.</li>
 * </ol>
 */
public final class PurchaseStrategy {

    private static final int REBIRTH_HERO = (int) RebirthService.REBIRTH_GATE_HERO_ID;      // 15
    private static final long REBIRTH_HERO_LEVEL = RebirthService.REBIRTH_GATE_HERO_LEVEL;  // 125
    private static final int MAX_PURCHASES_PER_CYCLE = 500;

    private PurchaseStrategy() {
    }

    /** Действие покупки для журнала. */
    public record Action(SimEvent.Kind kind, String text) {
    }

    public static List<Action> runCycle(SimState s) {
        List<Action> actions = new ArrayList<>();
        promoteSagaWalls(s, actions);
        pushRebirthGoal(s, actions);
        greedyDpsSpend(s, actions);
        return actions;
    }

    /** Пробиваем стены Саги: у героя на потолке ранга повышаем ранг (бесплатно, как заглушка SagaService). */
    private static void promoteSagaWalls(SimState s, List<Action> actions) {
        List<Integer> ids = new ArrayList<>();
        for (SimState.Owned o : s.ownedHeroes()) {
            ids.add(o.hero.id());
        }
        for (int id : ids) {
            int newRank;
            while ((newRank = s.promoteSagaIfAtCap(id)) > 0) {
                actions.add(new Action(SimEvent.Kind.SAGA_RANK,
                        "Сага → %s (%s), потолок %d".formatted(
                                SagaRank.byRank(newRank).getDisplayName(), SimHero.byId(id).name(),
                                Saga.levelCap(newRank))));
            }
        }
    }

    /** Купить героя 15 как только по карману и довести до 125 — это открывает ребёрт. */
    private static void pushRebirthGoal(SimState s, List<Action> actions) {
        SimHero hero15 = SimHero.byId(REBIRTH_HERO);
        if (!s.owns(REBIRTH_HERO) && s.buyHero(hero15)) {
            actions.add(new Action(SimEvent.Kind.BUY_HERO, "Куплен " + hero15.name() + " (ключ ребёрта)"));
        }
        if (s.owns(REBIRTH_HERO) && s.heroLevel(REBIRTH_HERO) < REBIRTH_HERO_LEVEL) {
            long got = s.upgradeHero(REBIRTH_HERO, REBIRTH_HERO_LEVEL - s.heroLevel(REBIRTH_HERO));
            if (got > 0) {
                actions.add(new Action(SimEvent.Kind.UPGRADE_HERO,
                        "%s +%d → ур.%d (к ребёрту 125)".formatted(hero15.name(), got, s.heroLevel(REBIRTH_HERO))));
            }
        }
    }

    /** Жадная трата золота: каждый шаг — лучший (прирост DPS / цена), исполняется max-покупкой. */
    private static void greedyDpsSpend(SimState s, List<Action> actions) {
        for (int i = 0; i < MAX_PURCHASES_PER_CYCLE; i++) {
            Candidate best = bestCandidate(s);
            if (best == null) {
                return;
            }
            actions.add(best.exec.get());
        }
    }

    private record Candidate(BigNum metric, Supplier<Action> exec) {
    }

    /** Выбирает апгрейд с максимальным (прирост DPS / цена) среди доступных по золоту; null — нечего купить. */
    private static Candidate bestCandidate(SimState s) {
        BigNum gold = s.gold();
        BigNum curDps = s.totalDps();
        List<Candidate> candidates = new ArrayList<>();

        // покупка нового героя (единичная цена)
        for (SimHero hero : SimHero.CATALOG) {
            if (s.owns(hero.id()) || gold.lt(hero.price())) {
                continue;
            }
            BigNum gain = s.dpsIfHeroAdded(hero).subtract(curDps);
            if (gain.compareTo(BigNum.ZERO) > 0) {
                candidates.add(new Candidate(gain.divide(hero.price()), () -> {
                    s.buyHero(hero);
                    return new Action(SimEvent.Kind.BUY_HERO, "Куплен " + hero.name());
                }));
            }
        }

        // апгрейд купленного героя: метрику берём по СЛЕДУЮЩЕМУ уровню, исполняем max-покупкой
        List<Integer> ownedIds = new ArrayList<>();
        for (SimState.Owned o : s.ownedHeroes()) {
            ownedIds.add(o.hero.id());
        }
        for (int id : ownedIds) {
            long level = s.heroLevel(id);
            if (level >= Saga.levelCap(sagaRankOf(s, id))) {
                continue; // упёрлись в стену Саги (макс. ранг) — апгрейдить некуда
            }
            BigNum cost = s.heroNextCost(id);
            if (gold.lt(cost)) {
                continue;
            }
            BigNum gain = s.dpsIfHeroAtLevel(id, level + 1).subtract(curDps);
            if (gain.compareTo(BigNum.ZERO) > 0) {
                candidates.add(new Candidate(gain.divide(cost), () -> {
                    long got = s.maxUpgradeHero(id);
                    return new Action(SimEvent.Kind.UPGRADE_HERO,
                            "%s +%d → ур.%d".formatted(SimHero.byId(id).name(), got, s.heroLevel(id)));
                }));
            }
        }

        // апгрейд тапа Аватара: эффективный DPS = урон тапа × клики/сек
        BigNum tapCost = s.tapNextCost();
        if (gold.gte(tapCost)) {
            BigNum gain = s.tapDamageAtLevel(s.tapLevel() + 1).subtract(s.tapDamage())
                    .multiply(s.clicksPerSecond());
            if (gain.compareTo(BigNum.ZERO) > 0) {
                candidates.add(new Candidate(gain.divide(tapCost), () -> {
                    long got = s.maxUpgradeTap();
                    return new Action(SimEvent.Kind.UPGRADE_TAP,
                            "Тап +%d → ур.%d".formatted(got, s.tapLevel()));
                }));
            }
        }

        return candidates.stream().max(Comparator.comparing(Candidate::metric)).orElse(null);
    }

    private static int sagaRankOf(SimState s, int heroId) {
        for (SimState.Owned o : s.ownedHeroes()) {
            if (o.hero.id() == heroId) {
                return o.sagaRank;
            }
        }
        return Saga.FIRST_RANK;
    }
}
