package com.ragnarok.idle.dto;

public record PlayerHeroResponse(
        Long heroId,
        String heroName,
        Long level,
        BigNumDto goldSpent,
        BigNumDto goldRemaining
) {
}
