package com.ragnarok.idle.dto;

import java.math.BigDecimal;

/**
 * Краткое представление осадного босса для расписания (без защитников и скиллов).
 * {@code currentHp} = base_hp × hp_multiplier (внутренний множитель в API не выносим).
 */
public record SiegeBossSummaryDto(
        String bossKey,
        String nameRu,
        String nameOriginal,
        int rotationOrder,
        int minRecommendedTier,
        BigDecimal spawnChance,
        Integer enrageSeconds,
        BigNumDto currentHp
) {
}
