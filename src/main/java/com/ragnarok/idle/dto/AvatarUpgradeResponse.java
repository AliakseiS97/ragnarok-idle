package com.ragnarok.idle.dto;

public record AvatarUpgradeResponse(
        Long tapDamageLevel,
        Long autotapLevel,
        BigNumDto goldSpent,
        BigNumDto goldRemaining
) {
}
