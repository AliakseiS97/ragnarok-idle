package com.ragnarok.idle.dto;

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
        Boolean autoAdvance,
        Long bossTimeLeftSeconds,
        List<PlayerHeroView> heroes,
        Boolean rebirthReady,
        String rebirthHint,
        BigNumDto ashDropCollected
) {
}
