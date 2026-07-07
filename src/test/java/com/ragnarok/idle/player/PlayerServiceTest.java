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

        // 5 DPS x 30с = 150 урона: 10 мобов x10 HP убиты (100), остаток 50 уходит в босса (180 HP).
        assertEquals(1L, state.currentLevel());
        assertEquals(11, state.currentSubLevel(), "без единого тапа дошли до босса уровня");
        assertEquals("50", state.gold().display(), "золото за 10 убитых мобов x5");
        assertEquals("0", state.offlineGoldCollected().display(), "онлайн-тик — не офлайн-доход");
        assertEquals("130", state.currentMobHp().display(), "босс 180 HP минус 50 перенесённого урона");
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

    private static double toPlain(double mantissa, long exponent) {
        return mantissa * Math.pow(10, exponent);
    }
}
