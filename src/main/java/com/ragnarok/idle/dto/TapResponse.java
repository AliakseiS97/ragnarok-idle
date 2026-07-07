package com.ragnarok.idle.dto;

public record TapResponse(
        BigNumDto damageDealt,
        int mobsKilled,
        boolean bossDefeated,
        boolean leveledUp,
        Long currentLevel,
        Integer currentSubLevel,
        BigNumDto currentMobHpRemaining,
        BigNumDto goldGained,
        BigNumDto goldTotal
) {
}
