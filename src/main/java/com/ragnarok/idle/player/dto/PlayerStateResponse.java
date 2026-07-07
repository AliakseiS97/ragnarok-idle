package com.ragnarok.idle.player.dto;

import com.ragnarok.idle.math.BigNumDto;

import java.util.List;

public record PlayerStateResponse(
        Long currentLevel,
        Long maxLevel,
        Integer currentSubLevel,
        BigNumDto currentMobHp,
        BigNumDto gold,
        BigNumDto ash,
        BigNumDto offlineGoldCollected,
        Long tapDamageLevel,
        Long autotapLevel,
        BigNumDto tapDamage,
        BigNumDto tapUpgradeCost,
        List<PlayerHeroView> heroes
) {
}
