package com.ragnarok.idle.dto;

public record PlayerHeroResponse(
        Long heroId,
        String heroName,
        Long level,
        Long levelsBought,
        BigNumDto goldSpent,
        BigNumDto goldRemaining
) {
}
