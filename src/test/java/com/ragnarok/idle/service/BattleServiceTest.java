package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.dto.TapResponse;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void bossTimerExpiryRestoresBossAndDropsPlayerToMobs() {
        authService.register("battle_timeout", "password123");
        Player player = playerRepository.findByUsername("battle_timeout").orElseThrow();
        player.setCurrentLevel(5L);
        player.setMaxLevel(5L);
        player.setCurrentSubLevel(10);
        player.setCurrentMobHp(EconomyCurves.bossHp(5));
        player.setBossStartedAt(LocalDateTime.now().minusSeconds(31)); // 30с истекли
        playerRepository.save(player);

        TapResponse response = battleService.tap("battle_timeout", 1);

        assertFalse(response.bossDefeated());
        assertFalse(response.leveledUp());
        assertEquals(5L, response.currentLevel(), "уровень не пройден");
        assertEquals(1, response.currentSubLevel(), "игрок отброшен на мобов уровня");
        // тап уже пошёл по 1-му мобу (93 HP - 1)
        assertEquals("92", response.currentMobHpRemaining().display());
    }

    @Test
    void farmModeLoopsMobsWithoutAdvancing() {
        authService.register("battle_farm", "password123");
        Player player = playerRepository.findByUsername("battle_farm").orElseThrow();
        player.setCurrentSubLevel(10); // последний моб уровня 1
        player.setAutoAdvance(false);  // флаг автоперехода ВЫКЛ — фарм
        playerRepository.save(player);

        TapResponse response = battleService.tap("battle_farm", 10); // ровно 10 HP моба

        assertEquals(1, response.mobsKilled());
        assertFalse(response.leveledUp(), "фарм-цикл не двигает уровень");
        assertEquals(1L, response.currentLevel());
        assertEquals(1, response.currentSubLevel(), "мобы уровня пошли по кругу");
        assertEquals("10", response.currentMobHpRemaining().display());
    }

    @Test
    void goToBossStartsBossFightOnlyOnBossLevels() {
        authService.register("battle_goto", "password123");
        Player player = playerRepository.findByUsername("battle_goto").orElseThrow();

        // уровень 4 — босса нет
        player.setCurrentLevel(4L);
        player.setMaxLevel(5L);
        playerRepository.save(player);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> battleService.goToBoss("battle_goto"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        // уровень 5 — на босса, таймер пошёл
        player.setCurrentLevel(5L);
        player.setCurrentSubLevel(3);
        player.setCurrentMobHp(EconomyCurves.mobHp(5));
        playerRepository.save(player);
        PlayerStateResponse state = battleService.goToBoss("battle_goto");

        assertEquals(10, state.currentSubLevel());
        assertEquals(EconomyCurves.bossHp(5).toDisplayString(), state.currentMobHp().display());
        assertTrue(state.bossTimeLeftSeconds() != null && state.bossTimeLeftSeconds() <= 30);
    }

    @Test
    void levelNavigationRespectsBounds() {
        authService.register("battle_nav", "password123");
        Player player = playerRepository.findByUsername("battle_nav").orElseThrow();
        player.setCurrentLevel(3L);
        player.setMaxLevel(3L);
        playerRepository.save(player);

        PlayerStateResponse back = battleService.changeLevel("battle_nav", -1);
        assertEquals(2L, back.currentLevel());
        assertEquals(1, back.currentSubLevel());
        assertEquals(EconomyCurves.mobHp(2).toDisplayString(), back.currentMobHp().display());

        PlayerStateResponse forward = battleService.changeLevel("battle_nav", 1);
        assertEquals(3L, forward.currentLevel());

        // выше достигнутого maxLevel нельзя
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> battleService.changeLevel("battle_nav", 1));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
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
