package com.ragnarok.idle.battle.dto;

import com.ragnarok.idle.math.BigNumDto;

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
