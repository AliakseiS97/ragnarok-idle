package com.ragnarok.idle.dto;

import com.ragnarok.idle.domain.Futhark;

/** Публичное представление руны. */
public record RuneDto(
        Long id,
        String runeKey,
        String symbol,
        String name,
        Futhark futhark,
        int tier,
        String meaning,
        String effectType,
        int effectValue,
        String effectDescription,
        String flavorText
) {
}
