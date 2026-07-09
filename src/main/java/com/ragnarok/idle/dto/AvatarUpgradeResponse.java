package com.ragnarok.idle.dto;

public record AvatarUpgradeResponse(
        Long tapDamageLevel,
        Long autotapLevel,
        Long levelsBought,
        BigNumDto goldSpent,
        BigNumDto goldRemaining
) {
}
