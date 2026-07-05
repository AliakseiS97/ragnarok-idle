package com.ragnarok.idle.hero.dto;

import com.ragnarok.idle.math.BigNumDto;

public record PlayerHeroResponse(
        Long heroId,
        String heroName,
        Long level,
        BigNumDto goldSpent,
        BigNumDto goldRemaining
) {
}
