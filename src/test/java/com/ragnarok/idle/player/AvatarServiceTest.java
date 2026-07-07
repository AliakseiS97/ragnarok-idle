package com.ragnarok.idle.player;

import com.ragnarok.idle.auth.AuthService;
import com.ragnarok.idle.battle.BattleService;
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
    void upgradeTapDamageSpendsGoldByGenreCurveAndRaisesLevel() {
        authService.register("tap_rich", "password123");
        Player player = playerRepository.findByUsername("tap_rich").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        // старт ур.1; цена ур.1->2 = 5, ур.2->3 = ceil(5 x 1.15) = 6 -> итого 11 (кривая ×1.15, целые).
        AvatarUpgradeResponse response = avatarService.upgradeTapDamage("tap_rich", 2);

        assertEquals(3L, response.tapDamageLevel());
        assertEquals(0L, response.autotapLevel());
        assertEquals("11", response.goldSpent().display());
        assertEquals("989", response.goldRemaining().display());

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        assertEquals(3L, avatar.getTapDamageLevel());
        assertEquals("3", BattleService.tapDamage(avatar).toDisplayString(), "урон/тап = уровень тапа");
    }

    @Test
    void tenthUpgradeCostsFixed109AndUnlocksClickSkill() {
        authService.register("tap_skill", "password123");
        Player player = playerRepository.findByUsername("tap_skill").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(9L);
        avatarRepository.save(avatar);

        // 10-е улучшение (ур.9 -> 10): фикс 109 золота, открывает умение +10% к клику.
        AvatarUpgradeResponse response = avatarService.upgradeTapDamage("tap_skill", 1);

        assertEquals(10L, response.tapDamageLevel());
        assertEquals("109", response.goldSpent().display());

        Avatar after = avatarRepository.findById(player.getId()).orElseThrow();
        assertEquals("11", BattleService.tapDamage(after).toDisplayString(),
                "с умением урон = floor(10 x 1.1) = 11");
    }

    @Test
    void upgradeAutotapIsIndependentFromTapDamageLevel() {
        authService.register("autotap_rich", "password123");
        Player player = playerRepository.findByUsername("autotap_rich").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        avatarService.upgradeTapDamage("autotap_rich", 3);
        AvatarUpgradeResponse response = avatarService.upgradeAutotap("autotap_rich", 1);

        assertEquals(4L, response.tapDamageLevel());
        assertEquals(1L, response.autotapLevel());

        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        assertEquals(4L, avatar.getTapDamageLevel());
        assertEquals(1L, avatar.getAutotapLevel());
    }
}
