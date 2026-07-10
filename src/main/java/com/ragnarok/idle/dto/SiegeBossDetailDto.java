package com.ragnarok.idle.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Полное представление осадного босса: скиллы, найм-умение, защитники и рассчитанный
 * текущий HP (base_hp × hp_multiplier). Используется для /today и /bosses/{key}.
 */
public record SiegeBossDetailDto(
        String bossKey,
        String nameRu,
        String nameOriginal,
        int rotationOrder,
        int minRecommendedTier,
        BigDecimal spawnChance,
        Integer enrageSeconds,
        BigNumDto currentHp,
        String skill1Name,
        String skill1Description,
        String skill2Name,
        String skill2Description,
        String hireSkillName,
        String hireSkillDescription,
        List<SiegeDefenderDto> defenders
) {
}
