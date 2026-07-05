package com.ragnarok.idle.economy;

import com.ragnarok.idle.math.BigNum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Сверка полосной HP-кривой с контрольными точками из economy_constants.md ("Кривая до 50k"). */
class EconomyCurvesTest {

    @Test
    void mobHpMatchesReferenceCheckpoints() {
        assertEquals(1.0, EconomyCurves.mobHp(1).log10(), 0.01);
        assertEquals(27.0, EconomyCurves.mobHp(10_000).log10(), 0.1);
        assertEquals(113.4, EconomyCurves.mobHp(30_000).log10(), 0.1);
        assertEquals(348.4, EconomyCurves.mobHp(50_000).log10(), 0.1);
    }

    @Test
    void bossHpIsMobHpTimes25() {
        BigNum ratio = EconomyCurves.bossHp(500).divide(EconomyCurves.mobHp(500));
        assertEquals(25.0, toPlain(ratio), 0.0001);
    }

    @Test
    void goldPerMobAtLevel1MatchesBaseGold() {
        // economy_constants.md: "Золото за моба ур.1 | 5"
        assertEquals(5.0, toPlain(EconomyCurves.goldPerMob(1)), 0.0001);
    }

    @Test
    void goldGrowsSlowerThanHp() {
        // "Отставание золота от HP: 0.97" -> золото должно расти медленнее HP
        double hpGrowthLog = EconomyCurves.mobHp(1000).log10() - EconomyCurves.mobHp(1).log10();
        double goldGrowthLog = EconomyCurves.goldPerMob(1000).log10() - EconomyCurves.goldPerMob(1).log10();
        assertTrue(goldGrowthLog < hpGrowthLog);
    }

    private static double toPlain(BigNum value) {
        return value.getMantissa() * Math.pow(10, value.getExponent());
    }
}
