package com.ragnarok.idle.dto;

/** Ответ на повышение Саги героя: новое состояние ранга/под-ступени для UI. */
public record SagaPromoteResponse(
        Long heroId,
        String heroName,
        int sagaRank,
        String sagaRankName,
        int sagaSubStep,
        String sagaSubStepRoman,
        long sagaLevelCap
) {
}
