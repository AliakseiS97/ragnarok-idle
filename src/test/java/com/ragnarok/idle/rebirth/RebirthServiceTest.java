package com.ragnarok.idle.rebirth;

import com.ragnarok.idle.auth.AuthService;
import com.ragnarok.idle.hero.PlayerHero;
import com.ragnarok.idle.hero.PlayerHeroRepository;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.player.Player;
import com.ragnarok.idle.player.PlayerRepository;
import com.ragnarok.idle.rebirth.dto.RebirthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RebirthServiceTest {

    @Autowired
    private RebirthService rebirthService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerHeroRepository playerHeroRepository;

    @Test
    void rebirthBelowMinLevelIsRejected() {
        authService.register("rebirth_low", "password123");
        // свежий игрок: maxLevel=1, порог из GDD §3.9 — 100.

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> rebirthService.rebirth("rebirth_low"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void rebirthResetsProgressGrantsAshAndKeepsMaxLevelRecord() {
        authService.register("rebirth_ok", "password123");
        Player player = playerRepository.findByUsername("rebirth_ok").orElseThrow();
        player.setMaxLevel(5000L);
        player.setCurrentLevel(5000L);
        player.setGold(BigNum.of(999));
        playerRepository.save(player);

        PlayerHero hero = new PlayerHero();
        hero.setPlayerId(player.getId());
        hero.setHeroId(1L);
        hero.setLevel(50L);
        hero.setActivated(true);
        playerHeroRepository.save(hero);

        RebirthResponse response = rebirthService.rebirth("rebirth_ok");

        // mobHP(5000)^0.0009, floor -> 1 (см. §3.9, ashExp=0.0009; проверено отдельным расчётом).
        assertEquals("1", response.ashGained().display());
        assertEquals("1", response.totalAsh().display());

        Player after = playerRepository.findByUsername("rebirth_ok").orElseThrow();
        assertEquals(1L, after.getCurrentLevel());
        assertEquals(5000L, after.getMaxLevel(), "maxLevel — исторический рекорд, не сбрасывается");
        assertEquals("0", com.ragnarok.idle.math.BigNumDto.from(after.getGold()).display());

        PlayerHero heroAfter = playerHeroRepository.findByPlayerIdAndHeroId(player.getId(), 1L).orElseThrow();
        assertEquals(1L, heroAfter.getLevel());
    }
}
