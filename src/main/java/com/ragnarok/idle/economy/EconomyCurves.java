package com.ragnarok.idle.economy;

import com.ragnarok.idle.math.BigNum;

/**
 * Полосная HP-кривая и золото (GDD §3.2/§3.3, числа — economy_constants.md).
 * Общий пул для кликера, AFK-дохода и перерождения — не дублируем формулы по сервисам.
 */
public final class EconomyCurves {

    private EconomyCurves() {
    }

    public static final int MOBS_PER_LEVEL = 10;

    private static final BigNum BASE_HP = BigNum.of(10);
    private static final BigNum BASE_GOLD = BigNum.of(5);

    /** золото = HP-множитель^0.97 (отставание золота от HP, economy_constants.md). */
    private static final double GOLD_LAG_EXPONENT = 0.97;

    /** HP таймер-босса = mobHP × 18 (было 25 — снижено ~28% балансировкой Спринта 1; щит-босс ×25 остаётся, §3.6). */
    private static final double BOSS_HP_MULT = 18;

    /** Бонус золота с босса — из GDD §3.3, в economy_constants.md отдельно не указан. */
    private static final double BOSS_GOLD_BONUS = 10;

    private record Band(long start, long end, double mult) {
        long capacity() {
            return end - start + 1;
        }
    }

    private static final Band[] BANDS = {
            new Band(1, 10_000, 1.006),
            new Band(10_001, 30_000, 1.01),
            new Band(30_001, 45_000, 1.02),
            new Band(45_001, 50_000, 1.05),
    };

    /** HP одного моба на уровне level (все 10 мобов уровня одинаковы). Целое — дробных HP в игре нет. */
    public static BigNum mobHp(long level) {
        if (level <= 1) return BASE_HP;
        long remaining = level - 1;
        BigNum result = BASE_HP;
        for (Band band : BANDS) {
            if (remaining <= 0) break;
            long steps = Math.min(remaining, band.capacity());
            result = result.multiply(BigNum.of(band.mult()).pow(steps));
            remaining -= steps;
        }
        if (remaining > 0) {
            // за "стеной" 50 000 — защитно продолжаем последней полосой, а не падаем
            double lastMult = BANDS[BANDS.length - 1].mult();
            result = result.multiply(BigNum.of(lastMult).pow(remaining));
        }
        return result.floor();
    }

    /** HP таймер-босса в конце уровня level (целое: целый mobHp × целый множитель). */
    public static BigNum bossHp(long level) {
        return mobHp(level).multiply(BOSS_HP_MULT);
    }

    /** Золото за одного убитого моба на уровне level. Целое — золото не дробится. */
    public static BigNum goldPerMob(long level) {
        BigNum ratio = mobHp(level).divide(BASE_HP);
        return BASE_GOLD.multiply(ratio.pow(GOLD_LAG_EXPONENT)).floor();
    }

    /** Золото за убитого таймер-босса на уровне level (целое: целое goldPerMob × целый бонус). */
    public static BigNum bossGold(long level) {
        return goldPerMob(level).multiply(BOSS_GOLD_BONUS);
    }
}
