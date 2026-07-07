package com.ragnarok.idle.player;

import com.ragnarok.idle.auth.AuthService;
import com.ragnarok.idle.hero.PlayerHero;
import com.ragnarok.idle.hero.PlayerHeroRepository;
import com.ragnarok.idle.player.dto.PlayerStateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PlayerServiceTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerHeroRepository playerHeroRepository;

    @Test
    void offlineIncomeIsCappedAtTwelveHours() {
        authService.register("afk_cap", "password123");
        Player player = playerRepository.findByUsername("afk_cap").orElseThrow();

        // Герой №1 (Трэлл, baseDps=5) ур.1, без баферов -> totalPassiveDps=5.
        PlayerHero hero = new PlayerHero();
        hero.setPlayerId(player.getId());
        hero.setHeroId(1L);
        hero.setLevel(1L);
        hero.setActivated(true);
        playerHeroRepository.save(hero);

        // "Заходил" 100 часов назад -> должно упереться в потолок 12ч (GDD §12.5).
        player.setLastCollectedAt(LocalDateTime.now().minusHours(100));
        playerRepository.save(player);

        PlayerStateResponse state = playerService.getState("afk_cap");

        // goldPerSecond = totalPassiveDps(5) * goldPerMob(1)/mobHp(1) = 5 * (5/10) = 2.5 золота/сек.
        // Потолок 12ч = 43200 сек -> ожидаем 108000 золота, а не 100ч-эквивалент.
        double offlineGold = toPlain(state.offlineGoldCollected().mantissa(), state.offlineGoldCollected().exponent());
        assertEquals(108000.0, offlineGold, 5.0);
    }

    @Test
    void passiveDpsKillsMobsAndAdvancesProgressWhileOnline() {
        authService.register("idle_online", "password123");
        Player player = playerRepository.findByUsername("idle_online").orElseThrow();

        // Герой №1 (baseDps=5) ур.1 -> totalPassiveDps=5.
        PlayerHero hero = new PlayerHero();
        hero.setPlayerId(player.getId());
        hero.setHeroId(1L);
        hero.setLevel(1L);
        hero.setActivated(true);
        playerHeroRepository.save(hero);

        // Пауза 30с (<= онлайн-порога): DPS должен бить мобов, а не капать золотом по формуле офлайна.
        player.setLastCollectedAt(LocalDateTime.now().minusSeconds(30));
        playerRepository.save(player);

        PlayerStateResponse state = playerService.getState("idle_online");

        // 30 ударов по 5 DPS: ур.1 (мобы 10 HP, 2 удара/моб) — 10 мобов за 20 ударов -> уровень 2.
        // Ур.2 (мобы 26 HP): 6 ударов добивают 1-го моба (излишек сгорает), 4 удара x5 во 2-го -> 26-20=6.
        assertEquals(2L, state.currentLevel(), "без единого тапа пройден уровень");
        assertEquals(2, state.currentSubLevel());
        assertEquals("62", state.gold().display(), "10 мобов ур.1 x5 + 1 моб ур.2 x12");
        assertEquals("0", state.offlineGoldCollected().display(), "онлайн-тик — не офлайн-доход");
        assertEquals("6", state.currentMobHp().display());
    }

    @Test
    void noOwnedHeroesMeansNoOfflineIncome() {
        authService.register("afk_none", "password123");
        Player player = playerRepository.findByUsername("afk_none").orElseThrow();
        player.setLastCollectedAt(LocalDateTime.now().minusHours(2));
        playerRepository.save(player);

        PlayerStateResponse state = playerService.getState("afk_none");

        assertEquals("0", state.offlineGoldCollected().display());
    }

    @Test
    void skipTimeAdvancesLevelsAndIsCappedAtTwelveHours() {
        PlayerStateResponse after50h = registerWithHeroAndSkip("skip_50", 50);
        PlayerStateResponse after12h = registerWithHeroAndSkip("skip_12", 12);
        PlayerStateResponse after5h = registerWithHeroAndSkip("skip_5", 5);

        // прокрутка проносит уровни, а не только капает золотом
        assertTrue(after5h.currentLevel() > 1, "за 5ч DPS должен пройти уровни, был " + after5h.currentLevel());
        // 50ч упирается в потолок 12ч (GDD §12.5): прогресс идентичен 12-часовому
        assertEquals(after12h.currentLevel(), after50h.currentLevel());
        assertEquals(after12h.currentSubLevel(), after50h.currentSubLevel());
        assertEquals(after12h.gold().display(), after50h.gold().display());
        // а 5ч — строго меньше 12ч
        assertTrue(after5h.currentLevel() < after12h.currentLevel());
    }

    private PlayerStateResponse registerWithHeroAndSkip(String username, int hours) {
        authService.register(username, "password123");
        Player player = playerRepository.findByUsername(username).orElseThrow();
        PlayerHero hero = new PlayerHero();
        hero.setPlayerId(player.getId());
        hero.setHeroId(1L);
        hero.setLevel(1L);
        hero.setActivated(true);
        playerHeroRepository.save(hero);
        return playerService.skipTime(username, hours);
    }

    private static double toPlain(double mantissa, long exponent) {
        return mantissa * Math.pow(10, exponent);
    }
}
