package com.ragnarok.idle.dto;

public record PlayerHeroView(
        Long heroId,
        String name,
        String type,
        boolean owned,
        Long level,
        BigNumDto price,
        BigNumDto upgradeCost,
        BigNumDto dps,
        // --- Сага (GDD §3.8) ---
        int sagaRank,
        String sagaRankName,
        int sagaSubStep,
        String sagaSubStepRoman,
        String sagaColor,
        Long sagaLevelCap,
        boolean atLevelCap,
        // --- Мультипокупка: цена пачки под текущий режим (сервер, античит) ---
        Long bulkLevels,
        BigNumDto bulkCost,
        boolean bulkAffordable
) {
}
