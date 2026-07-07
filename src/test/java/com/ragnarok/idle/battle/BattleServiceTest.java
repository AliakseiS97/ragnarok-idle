package com.ragnarok.idle.battle;

import com.ragnarok.idle.auth.AuthService;
import com.ragnarok.idle.battle.dto.TapResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BattleServiceTest {

    @Autowired
    private BattleService battleService;

    @Autowired
    private AuthService authService;

    @Test
    void partialDamageDoesNotKillMob() {
        authService.register("battle_partial", "password123");

        // baseTap=1, mobHP(1)=10 -> одного тапа недостаточно, чтобы убить.
        TapResponse response = battleService.tap("battle_partial", 1);

        assertEquals(0, response.mobsKilled());
        assertFalse(response.leveledUp());
        assertEquals(1, response.currentSubLevel());
        assertEquals("9", response.currentMobHpRemaining().display());
    }

    @Test
    void lethalTapsKillMobAndCarryOverDamage() {
        authService.register("battle_kill", "password123");

        // 12 тапов x1 = 12 урона: убивает 1-го моба (10 HP) и переносит 2 урона на следующего.
        TapResponse response = battleService.tap("battle_kill", 12);

        assertEquals(1, response.mobsKilled());
        assertEquals(1L, response.currentLevel());
        assertEquals(2, response.currentSubLevel());
        assertEquals("5", response.goldGained().display()); // goldPerMob(1) = 5
        assertEquals("5", response.goldTotal().display());
    }

    @Test
    void enoughDamageDefeatsBossAndLevelsUp() {
        authService.register("battle_boss", "password123");

        // Уровень 1: 10 мобов x10 HP + босс (mobHP*18=180) = 280 HP. 300 тапов x1 = 300 урона - с запасом.
        TapResponse response = battleService.tap("battle_boss", 300);

        assertTrue(response.bossDefeated());
        assertTrue(response.leveledUp());
        assertEquals(2L, response.currentLevel());
    }
}
