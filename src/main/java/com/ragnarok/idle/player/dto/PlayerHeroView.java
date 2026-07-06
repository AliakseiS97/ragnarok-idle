package com.ragnarok.idle.player.dto;

import com.ragnarok.idle.math.BigNumDto;

public record PlayerHeroView(
        Long heroId,
        String name,
        String type,
        boolean owned,
        Long level,
        BigNumDto price,
        BigNumDto dps
) {
}
