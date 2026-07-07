package com.ragnarok.idle.player;

import com.ragnarok.idle.auth.AuthService;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.player.dto.AvatarUpgradeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AvatarServiceTest {

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Test
    void upgradeTapDamageWithoutEnoughGoldIsRejected() {
        authService.register("tap_poor", "password123");
        // свежий игрок стартует с 0 золота, апгрейд стоит 5.

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> avatarService.upgradeTapDamage("tap_poor", 1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void upgradeTapDamageSpendsGoldByHeroStyleCurveAndRaisesLevel() {
        authService.register("tap_rich", "password123");
        Player player = playerRepository.findByUsername("tap_rich").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        // цена 1-го уровня = 5 * 1.07^0 = 5, 2-го = ceil(5 * 1.07) = 6 -> итого 11 за 2 уровня (цены целые).
        AvatarUpgradeResponse response = avatarService.upgradeTapDamage("tap_rich", 2);

        assertEquals(2L, response.tapDamageLevel());
        assertEquals(0L, response.autotapLevel());
        assertEquals("11", response.goldSpent().display());
        assertEquals("989", response.goldRemaining().display());

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        assertEquals(2L, avatar.getTapDamageLevel());
    }

    @Test
    void upgradeAutotapIsIndependentFromTapDamageLevel() {
        authService.register("autotap_rich", "password123");
        Player player = playerRepository.findByUsername("autotap_rich").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        avatarService.upgradeTapDamage("autotap_rich", 3);
        AvatarUpgradeResponse response = avatarService.upgradeAutotap("autotap_rich", 1);

        assertEquals(3L, response.tapDamageLevel());
        assertEquals(1L, response.autotapLevel());

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        assertEquals(3L, avatar.getTapDamageLevel());
        assertEquals(1L, avatar.getAutotapLevel());
    }
}
