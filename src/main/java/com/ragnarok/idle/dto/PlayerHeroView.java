package com.ragnarok.idle.dto;

public record PlayerHeroView(
        Long heroId,
        String name,
        String type,
        boolean owned,
        Long level,
        BigNumDto price,
        BigNumDto upgradeCost,
        BigNumDto dps
) {
}
