package com.ragnarok.idle.dto;

import java.math.BigDecimal;

/** Защитник осадного босса. */
public record SiegeDefenderDto(
        String nameRu,
        int position,
        BigDecimal hpPercentOfBoss,
        String skillDescription
) {
}
