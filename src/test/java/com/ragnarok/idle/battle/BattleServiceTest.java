package com.ragnarok.idle.battle;

import com.ragnarok.idle.auth.AuthService;
import com.ragnarok.idle.battle.dto.TapResponse;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.player.Avatar;
import com.ragnarok.idle.player.AvatarRepository;
import com.ragnarok.idle.player.Player;
import com.ragnarok.idle.player.PlayerRepository;
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

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Test
    void partialDamageDoesNotKillMob() {
        authService.register("battle_partial", "password123");

        // урон/тап = уровень тапа (старт ур.1 -> 1), mobHP(1)=10 -> одного тапа мало.
        TapResponse response = battleService.tap("battle_partial", 1);

        assertEquals(0, response.mobsKilled());
        assertFalse(response.leveledUp());
        assertEquals(1, response.currentSubLevel());
        assertEquals("9", response.currentMobHpRemaining().display());
    }

    @Test
    void killDoesNotCarryDamageToNextMob() {
        authService.register("battle_kill", "password123");

        // 12 тапов x1: 10-й тап убивает моба, тапы 11-12 бьют СЛЕДУЮЩЕГО с полным HP (10-2=8).
        TapResponse response = battleService.tap("battle_kill", 12);

        assertEquals(1, response.mobsKilled());
        assertEquals(1L, response.currentLevel());
        assertEquals(2, response.currentSubLevel());
        assertEquals("8", response.currentMobHpRemaining().display());
        assertEquals("5", response.goldGained().display()); // goldPerMob(1) = 5
    }

    @Test
    void overkillHitIsDiscardedNextMobLoadsAtFullHp() {
        authService.register("battle_overkill", "password123");
        Player player = playerRepository.findByUsername("battle_overkill").orElseThrow();
        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(1000L); // урон = floor(1000 x 1.1) = 1100 >> 10 HP
        avatarRepository.save(avatar);

        TapResponse response = battleService.tap("battle_overkill", 1);

        assertEquals(1, response.mobsKilled(), "один удар = максимум один труп");
        assertEquals(2, response.currentSubLevel());
        assertEquals("10", response.currentMobHpRemaining().display(), "следующий моб загружен с полным HP");
    }

    @Test
    void bossOnTenthSlotOfFifthLevelGrantsLevelUp() {
        authService.register("battle_boss", "password123");
        Player player = playerRepository.findByUsername("battle_boss").orElseThrow();
        // ставим игрока прямо на босса: уровень 5, 10-я подлокация, HP босса = mobHp(5) x 12 = 1116
        player.setCurrentLevel(5L);
        player.setMaxLevel(5L);
        player.setCurrentSubLevel(10);
        player.setCurrentMobHp(EconomyCurves.bossHp(5));
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(10_000L); // урон 11000 - убивает босса одним тапом
        avatarRepository.save(avatar);

        TapResponse response = battleService.tap("battle_boss", 1);

        assertTrue(response.bossDefeated());
        assertTrue(response.leveledUp());
        assertEquals(0, response.mobsKilled(), "босс не считается обычным мобом");
        assertEquals(6L, response.currentLevel());
        assertEquals(1, response.currentSubLevel());
        assertEquals(EconomyCurves.mobHp(6).toDisplayString(), response.currentMobHpRemaining().display(),
                "уровень 6 начинается с полного HP моба (кривая не падает после босса)");
    }

    @Test
    void regularLevelHasNoBossOnTenthSlot() {
        authService.register("battle_no_boss", "password123");
        Player player = playerRepository.findByUsername("battle_no_boss").orElseThrow();
        player.setCurrentSubLevel(10); // уровень 1 (не кратен 5) — 10-я подлокация обычный моб
        playerRepository.save(player);

        TapResponse response = battleService.tap("battle_no_boss", 10); // 10 x1 урона = ровно 10 HP

        assertFalse(response.bossDefeated());
        assertTrue(response.leveledUp(), "10-й моб добит — уровень пройден и без босса");
        assertEquals(1, response.mobsKilled());
        assertEquals(2L, response.currentLevel());
    }
}
