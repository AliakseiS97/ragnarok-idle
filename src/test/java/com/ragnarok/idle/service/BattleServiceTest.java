package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.dto.TapResponse;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
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
        assertEquals("2", response.goldGained().display()); // goldPerMob(1) = BASE_GOLD = 2 (баланс)
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
    void killingLastMobOfRegularLevelEntersBossLevelDirectly() {
        authService.register("battle_enter_boss", "password123");
        Player player = playerRepository.findByUsername("battle_enter_boss").orElseThrow();
        // последний (10-й) моб обычного уровня 4; следующий уровень (5) — целиком боссовый
        player.setCurrentLevel(4L);
        player.setMaxLevel(4L);
        player.setCurrentSubLevel(10);
        player.setCurrentMobHp(EconomyCurves.mobHp(4));
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(1000L); // урон 1100 >> mobHp(4)=68 — гарантированно убивает
        avatarRepository.save(avatar);

        TapResponse response = battleService.tap("battle_enter_boss", 1);

        assertFalse(response.bossDefeated(), "только вошли на боссовый уровень, ещё не убили");
        assertTrue(response.leveledUp());
        assertEquals(5L, response.currentLevel());
        assertEquals(1, response.currentSubLevel());
        assertEquals(EconomyCurves.bossHp(5).toDisplayString(), response.currentMobHpRemaining().display(),
                "уровень 5 целиком боссовый — сразу полное HP босса, без обычных мобов");
    }

    @Test
    void killingBossAdvancesToNextRegularLevelWithContinuousCurve() {
        authService.register("battle_boss_defeat", "password123");
        Player player = playerRepository.findByUsername("battle_boss_defeat").orElseThrow();
        player.setCurrentLevel(5L);
        player.setMaxLevel(5L);
        player.setCurrentMobHp(EconomyCurves.bossHp(5));
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(10_000L); // урон 11000 - убивает босса одним тапом
        avatarRepository.save(avatar);

        TapResponse response = battleService.tap("battle_boss_defeat", 1);

        assertTrue(response.bossDefeated());
        assertTrue(response.leveledUp());
        assertEquals(0, response.mobsKilled(), "босс не считается обычным мобом");
        assertEquals(6L, response.currentLevel());
        assertEquals(1, response.currentSubLevel());
        assertEquals(EconomyCurves.mobHp(6).toDisplayString(), response.currentMobHpRemaining().display(),
                "уровень 6 начинается с полного HP моба (кривая не падает после босса)");
    }

    @Test
    void bossTimerExpiryResetsBossHpButKeepsPlayerOnBossLevel() {
        authService.register("battle_timeout", "password123");
        Player player = playerRepository.findByUsername("battle_timeout").orElseThrow();
        player.setCurrentLevel(5L);
        player.setMaxLevel(5L);
        player.setCurrentMobHp(EconomyCurves.bossHp(5));
        player.setBossStartedAt(LocalDateTime.now().minusSeconds(31)); // 30с истекли
        playerRepository.save(player);

        TapResponse response = battleService.tap("battle_timeout", 1);

        assertFalse(response.bossDefeated());
        assertFalse(response.leveledUp());
        assertEquals(5L, response.currentLevel(), "уровень не пройден — остаёмся на боссе, откатываться некуда");
        assertEquals(1, response.currentSubLevel());
        // таймер истёк ДО тапа -> сброс на полное bossHp(5), тап (урон 1) пошёл уже по свежему боссу
        assertEquals(EconomyCurves.bossHp(5).subtract(BigNum.of(1)).toDisplayString(),
                response.currentMobHpRemaining().display());
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
    void farmModeAlsoBlocksEnteringBossLevel() {
        authService.register("battle_farm_boss", "password123");
        Player player = playerRepository.findByUsername("battle_farm_boss").orElseThrow();
        player.setCurrentLevel(4L);
        player.setMaxLevel(4L);
        player.setCurrentSubLevel(10); // последний моб уровня 4
        player.setCurrentMobHp(EconomyCurves.mobHp(4));
        player.setAutoAdvance(false); // фарм
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(1000L); // урон 1100 >> mobHp(4)=68 — гарантированно убивает
        avatarRepository.save(avatar);

        TapResponse response = battleService.tap("battle_farm_boss", 1);

        assertEquals(1, response.mobsKilled());
        assertFalse(response.leveledUp(), "фарм-режим не пускает даже на боссовый уровень");
        assertEquals(4L, response.currentLevel());
        assertEquals(1, response.currentSubLevel(), "мобы уровня 4 пошли по кругу");
        assertEquals(EconomyCurves.mobHp(4).toDisplayString(), response.currentMobHpRemaining().display());
    }

    @Test
    void goToBossJumpsDirectlyToPendingBossLevel() {
        authService.register("battle_goto", "password123");
        Player player = playerRepository.findByUsername("battle_goto").orElseThrow();
        // застрял на боссе 5-го уровня, ушёл фармить золото на 2-й
        player.setCurrentLevel(2L);
        player.setMaxLevel(5L);
        playerRepository.save(player);

        PlayerStateResponse state = battleService.goToBoss("battle_goto");

        assertEquals(5L, state.currentLevel());
        assertEquals(1, state.currentSubLevel());
        assertEquals(EconomyCurves.bossHp(5).toDisplayString(), state.currentMobHp().display());
        assertTrue(state.bossTimeLeftSeconds() != null && state.bossTimeLeftSeconds() <= 30);
    }

    @Test
    void goToBossRejectedWhenNoPendingBoss() {
        authService.register("battle_goto_none", "password123");
        Player player = playerRepository.findByUsername("battle_goto_none").orElseThrow();
        // maxLevel=7 — прогресс МЕЖДУ боссами (5-й уже пройден, 10-й ещё не достигнут): застревать негде
        player.setCurrentLevel(6L);
        player.setMaxLevel(7L);
        playerRepository.save(player);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> battleService.goToBoss("battle_goto_none"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void goToBossRejectedWhenAlreadyThere() {
        authService.register("battle_goto_already", "password123");
        Player player = playerRepository.findByUsername("battle_goto_already").orElseThrow();
        player.setCurrentLevel(5L);
        player.setMaxLevel(5L);
        playerRepository.save(player);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> battleService.goToBoss("battle_goto_already"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
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
    void navigatingDirectlyOntoBossLevelStartsTheFight() {
        authService.register("battle_nav_boss", "password123");
        Player player = playerRepository.findByUsername("battle_nav_boss").orElseThrow();
        player.setCurrentLevel(4L);
        player.setMaxLevel(5L);
        playerRepository.save(player);

        PlayerStateResponse state = battleService.changeLevel("battle_nav_boss", 1);

        assertEquals(5L, state.currentLevel());
        assertEquals(1, state.currentSubLevel());
        assertEquals(EconomyCurves.bossHp(5).toDisplayString(), state.currentMobHp().display());
        assertTrue(state.bossTimeLeftSeconds() != null, "переход на боссовый уровень сразу ставит таймер");
    }

    @Test
    void regularLevelHasNoBossOnTenthSlot() {
        authService.register("battle_no_boss", "password123");
        Player player = playerRepository.findByUsername("battle_no_boss").orElseThrow();
        player.setCurrentSubLevel(10); // уровень 1 (не кратен 5) — 10-й обычный моб, боссовым не станет
        playerRepository.save(player);

        TapResponse response = battleService.tap("battle_no_boss", 10); // 10 x1 урона = ровно 10 HP

        assertFalse(response.bossDefeated());
        assertTrue(response.leveledUp(), "10-й моб добит — уровень пройден, следующий (2) обычный");
        assertEquals(1, response.mobsKilled());
        assertEquals(2L, response.currentLevel());
    }
}
