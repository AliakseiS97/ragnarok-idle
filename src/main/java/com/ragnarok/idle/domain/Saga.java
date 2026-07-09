package com.ragnarok.idle.domain;

import com.ragnarok.idle.math.BigNum;

/**
 * Механика Саги: под-ступени, формула DPS-множителя и хелперы отображения (GDD §3.8).
 * Ранги-конфиг (названия/потолки) — в {@link SagaRank}, здесь — числа и формулы.
 *
 * <p>Структура прогресса героя: <b>ранг</b> (1..N, {@link SagaRank}) × <b>под-ступень</b> (I..X внутри ранга).
 * Свежекупленный герой — ранг 1 («Обычный»), под-ступень I.
 *
 * <p><b>DPS-множитель Саги</b> (только этот герой):
 * <pre>
 *   sagaMult = 1 + SUB_STEP_DPS_BONUS × (subStep − 1) + RANK_UP_DPS_BONUS × (rank − 1)
 * </pre>
 * — под-ступени внутри ранга: I = база (0%), каждая следующая +25% (X = +225%);
 * — переход на новый ранг: разово +500% за каждый пройденный ранг.
 * Множитель монотонно растёт даже при сбросе под-ступени на I при ранг-апе
 * (напр. Обычный X = ×3.25 → Редкий I = ×6.0).
 */
public final class Saga {

    private Saga() {
    }

    /** Под-ступеней в одном ранге: I..X. */
    public static final int SUB_STEPS_PER_RANK = 10;

    /** Стартовые значения для только что купленного героя. */
    public static final int FIRST_RANK = 1;
    public static final int FIRST_SUB_STEP = 1;

    /** +25% к DPS героя за каждую пройденную под-ступень внутри ранга (I→X). */
    public static final double SUB_STEP_DPS_BONUS = 0.25;

    /** +500% к DPS героя разово за каждый пройденный ранг (смена Обычный→Редкий и т.д.). */
    public static final double RANK_UP_DPS_BONUS = 5.0;

    /** Личный DPS-множитель героя от его Саги. rank ∈ [1..N], subStep ∈ [1..{@link #SUB_STEPS_PER_RANK}]. */
    public static BigNum dpsMultiplier(int rank, int subStep) {
        double mult = 1.0
                + SUB_STEP_DPS_BONUS * (subStep - 1)
                + RANK_UP_DPS_BONUS * (rank - 1);
        return BigNum.of(mult);
    }

    /** Потолок уровня («стена») для ранга. */
    public static long levelCap(int rank) {
        return SagaRank.byRank(rank).getLevelCap();
    }

    /** true, если Сага на максимуме (последний ранг, последняя под-ступень) — повышать некуда. */
    public static boolean isMaxed(int rank, int subStep) {
        return rank >= SagaRank.maxRank() && subStep >= SUB_STEPS_PER_RANK;
    }

    /** Римская запись под-ступени 1..10 для UI (I..X). */
    public static String toRoman(int subStep) {
        return switch (subStep) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> throw new IllegalArgumentException("Под-ступень вне диапазона 1..10: " + subStep);
        };
    }
}
