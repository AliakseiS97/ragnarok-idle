package com.ragnarok.idle.economy;

import com.ragnarok.idle.math.BigNum;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Сверка непрерывной HP-кривой с ориентирами плейтеста и контрольными точками (economy_constants.md). */
class EconomyCurvesTest {

    @Test
    void mobHpMatchesPlaytestAnchors() {
        // Ориентиры: ур.1=10, ур.2=26, ур.3=45, ур.4=68, ур.5=93 (floor(10 × L^1.375 × 1.006^(L-1)))
        assertEquals(10.0, toPlain(EconomyCurves.mobHp(1)), 0.0001);
        assertEquals(26.0, toPlain(EconomyCurves.mobHp(2)), 0.0001);
        assertEquals(45.0, toPlain(EconomyCurves.mobHp(3)), 0.0001);
        assertEquals(68.0, toPlain(EconomyCurves.mobHp(4)), 0.0001);
        assertEquals(93.0, toPlain(EconomyCurves.mobHp(5)), 0.0001);
    }

    @Test
    void mobHpMatchesReferenceCheckpoints() {
        // Кривая до 50k (economy_constants.md): log10(HP) = 1 + 1.375×log10(L) + вклад полос
        assertEquals(32.5, EconomyCurves.mobHp(10_000).log10(), 0.1);
        assertEquals(119.6, EconomyCurves.mobHp(30_000).log10(), 0.1);
        assertEquals(354.8, EconomyCurves.mobHp(50_000).log10(), 0.1);
    }

    @Test
    void mobHpNeverDropsBetweenLevels() {
        // Непрерывный рост: после босса (каждый 5-й уровень) HP следующего уровня не падает.
        for (long level = 1; level < 30; level++) {
            assertTrue(EconomyCurves.mobHp(level + 1).gte(EconomyCurves.mobHp(level)),
                    "HP упало между ур." + level + " и " + (level + 1));
        }
    }

    @Test
    void bossHpIsMobHpTimes12() {
        // Босс каждого 5-го уровня: ×12 к обычному мобу (ориентир ×10-15)
        BigNum ratio = EconomyCurves.bossHp(5).divide(EconomyCurves.mobHp(5));
        assertEquals(12.0, toPlain(ratio), 0.0001);
    }

    @Test
    void goldPerMobAtLevel1MatchesBaseGold() {
        // Баланс прогрессии (симулятор): золото за моба ур.1 = BASE_GOLD = 2 — замедляет приток
        // золота, чтобы обычный игрок проходил 100 этажей за ~55 мин, а не за ~21.
        assertEquals(2.0, toPlain(EconomyCurves.goldPerMob(1)), 0.0001);
    }

    @Test
    void goldGrowsSlowerThanHp() {
        // "Отставание золота от HP: 0.97" -> золото должно расти медленнее HP
        double hpGrowthLog = EconomyCurves.mobHp(1000).log10() - EconomyCurves.mobHp(1).log10();
        double goldGrowthLog = EconomyCurves.goldPerMob(1000).log10() - EconomyCurves.goldPerMob(1).log10();
        assertTrue(goldGrowthLog < hpGrowthLog);
    }

    @Test
    void curveValuesAreWholeNumbers() {
        // Дробных чисел в игре нет: HP и золото — целые на любом уровне.
        for (long level : new long[]{2, 7, 100, 500}) {
            assertEquals(EconomyCurves.mobHp(level), EconomyCurves.mobHp(level).floor(), "mobHp ур." + level);
            assertEquals(EconomyCurves.goldPerMob(level), EconomyCurves.goldPerMob(level).floor(), "goldPerMob ур." + level);
        }
    }

    private static double toPlain(BigNum value) {
        return value.getMantissa() * Math.pow(10, value.getExponent());
    }
}
