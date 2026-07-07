package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.RebirthResponse;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.PlayerHeroRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private AvatarRepository avatarRepository;

    @Test
    void rebirthBelowMinLevelIsRejected() {
        authService.register("rebirth_low", "password123");
        // свежий игрок: maxLevel=1, порог из GDD §3.9 — 100.

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> rebirthService.rebirth("rebirth_low"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void rebirthResetsEverythingGrantsBoostedAshAndKeepsMaxLevelRecord() {
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

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(12L);
        avatar.setAutotapLevel(3L);
        avatarRepository.save(avatar);

        RebirthResponse response = rebirthService.rebirth("rebirth_ok");

        // floor(2.5 × mobHP(5000)^0.0009) = floor(2.5 × 1.04) = 2 (буст Пепла ×2.5)
        assertEquals("2", response.ashGained().display());
        assertEquals("2", response.totalAsh().display());

        Player after = playerRepository.findByUsername("rebirth_ok").orElseThrow();
        assertEquals(1L, after.getCurrentLevel());
        assertEquals(5000L, after.getMaxLevel(), "maxLevel — исторический рекорд, не сбрасывается");
        assertEquals("0", BigNumDto.from(after.getGold()).display());

        assertTrue(playerHeroRepository.findByPlayerIdAndHeroId(player.getId(), 1L).isEmpty(),
                "герои теряются полностью — нанимать заново (ур. 0)");

        Avatar avatarAfter = avatarRepository.findById(player.getId()).orElseThrow();
        assertEquals(1L, avatarAfter.getTapDamageLevel(), "тап Аватара — на стартовый ур.1");
        assertEquals(0L, avatarAfter.getAutotapLevel());
    }
}
