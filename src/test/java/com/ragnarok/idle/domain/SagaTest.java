package com.ragnarok.idle.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Чистые тесты формул Саги без Spring (GDD §3.8). */
class SagaTest {

    @Test
    void dpsMultiplierCountsSubStepsAndRanks() {
        // 1 + 0.25×(subStep−1) + 5.0×(rank−1)
        assertEquals("1", Saga.dpsMultiplier(1, 1).toDisplayString(), "Обычный I — база, без бонуса");
        assertEquals("1.25", Saga.dpsMultiplier(1, 2).toDisplayString(), "одна под-ступень = +25%");
        assertEquals("3.25", Saga.dpsMultiplier(1, 10).toDisplayString(), "9 под-ступеней = +225%");
        assertEquals("6", Saga.dpsMultiplier(2, 1).toDisplayString(), "новый ранг = +500% разово");
        assertEquals("6.50", Saga.dpsMultiplier(2, 3).toDisplayString(), "ранг +500% и 2 под-ступени +50%");
        assertEquals("33.25", Saga.dpsMultiplier(7, 10).toDisplayString(), "макс: +225% + 6×500%");
    }

    @Test
    void rankUpIsMonotonicDespiteSubStepReset() {
        // при ранг-апе под-ступень сбрасывается на I, но множитель всё равно растёт
        double atRankXTop = Saga.dpsMultiplier(1, 10).getMantissa() * Math.pow(10, Saga.dpsMultiplier(1, 10).getExponent());
        double atNextRankI = Saga.dpsMultiplier(2, 1).getMantissa() * Math.pow(10, Saga.dpsMultiplier(2, 1).getExponent());
        assertTrue(atNextRankI > atRankXTop, "Редкий I должен быть сильнее Обычного X");
    }

    @Test
    void romanNumeralsForSubSteps() {
        assertEquals("I", Saga.toRoman(1));
        assertEquals("IV", Saga.toRoman(4));
        assertEquals("IX", Saga.toRoman(9));
        assertEquals("X", Saga.toRoman(10));
        assertThrows(IllegalArgumentException.class, () -> Saga.toRoman(0));
        assertThrows(IllegalArgumentException.class, () -> Saga.toRoman(11));
    }

    @Test
    void levelCapsMatchRankLadder() {
        assertEquals(500, Saga.levelCap(1), "Обычный");
        assertEquals(30_000, Saga.levelCap(SagaRank.maxRank()), "последний ранг");
    }

    @Test
    void rankLadderIsExtensibleAndNavigable() {
        assertEquals(7, SagaRank.maxRank(), "пока 7 рангов (расширяемо новыми константами)");
        assertEquals(SagaRank.COMMON, SagaRank.byRank(1));
        assertEquals(SagaRank.RARE, SagaRank.COMMON.next());
        assertTrue(SagaRank.PRIMORDIAL.isMax());
        assertThrows(IllegalStateException.class, SagaRank.PRIMORDIAL::next);
        assertThrows(IllegalArgumentException.class, () -> SagaRank.byRank(SagaRank.maxRank() + 1));
    }

    @Test
    void isMaxedOnlyAtLastRankLastSubStep() {
        assertTrue(Saga.isMaxed(SagaRank.maxRank(), Saga.SUB_STEPS_PER_RANK));
        assertFalse(Saga.isMaxed(SagaRank.maxRank(), Saga.SUB_STEPS_PER_RANK - 1));
        assertFalse(Saga.isMaxed(1, Saga.SUB_STEPS_PER_RANK), "X внутри 1-го ранга — не максимум");
    }
}
