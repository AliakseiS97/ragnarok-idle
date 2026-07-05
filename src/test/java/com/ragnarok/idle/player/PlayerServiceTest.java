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

        // Герой №1 (Трэлл, baseDps=2) ур.1, без баферов -> totalPassiveDps=2.
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

        // goldPerSecond = totalPassiveDps(2) * goldPerMob(1)/mobHp(1) = 2 * (5/10) = 1 золото/сек.
        // Потолок 12ч = 43200 сек -> ожидаем 43200 золота, а не 100ч-эквивалент.
        double offlineGold = toPlain(state.offlineGoldCollected().mantissa(), state.offlineGoldCollected().exponent());
        assertEquals(43200.0, offlineGold, 1.0);
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
