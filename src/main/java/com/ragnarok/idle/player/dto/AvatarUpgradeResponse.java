package com.ragnarok.idle.player.dto;

import com.ragnarok.idle.math.BigNumDto;

public record AvatarUpgradeResponse(
        Long tapDamageLevel,
        Long autotapLevel,
        BigNumDto goldSpent,
        BigNumDto goldRemaining
) {
}
