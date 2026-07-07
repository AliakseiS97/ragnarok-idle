package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.dto.TapResponse;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Дроп Пепла с обычных мобов (правка ТЗ п.4): только после 1-го перерождения, 2% шанс, не с боссов.
 * Источник случайности подменяется НЕ Mockito-мокой (JDK-функциональные интерфейсы не инструментируются
 * inline mock maker на этой JDK), а обычным Spring-бином из {@link Config} с приоритетом {@code @Primary}.
 */
@SpringBootTest
class AshDropTest {

    @Autowired
    private BattleService battleService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private FakeAshDropRoll ashDropRoll;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeAshDropRoll fakeAshDropRoll() {
            return new FakeAshDropRoll();
        }
    }

    static class FakeAshDropRoll implements DoubleSupplier {
        private double next = 1.0; // по умолчанию — гарантированный промах

        void setNext(double next) {
            this.next = next;
        }

        @Override
        public double getAsDouble() {
            return next;
        }
    }

    private void giveOneShotKillDamage(String username) {
        Player player = playerRepository.findByUsername(username).orElseThrow();
        Avatar avatar = avatarRepository.findById(player.getId()).orElseThrow();
        avatar.setTapDamageLevel(10_000L); // урон 11000 — убивает моба/босса ур.1-5 одним тапом
        avatarRepository.save(avatar);
    }

    @Test
    void noDropBeforeFirstRebirthEvenOnGuaranteedRoll() {
        authService.register("ash_no_rebirth", "password123");
        giveOneShotKillDamage("ash_no_rebirth");
        ashDropRoll.setNext(0.0); // гарантированный бросок

        TapResponse response = battleService.tap("ash_no_rebirth", 1);

        assertEquals("0", response.ashGained().display(), "до 1-го перерождения дроп выключен");
    }

    @Test
    void dropsOnGuaranteedRollAfterFirstRebirth() {
        authService.register("ash_after_rebirth", "password123");
        Player player = playerRepository.findByUsername("ash_after_rebirth").orElseThrow();
        player.setRebirthCount(1L);
        playerRepository.save(player);
        giveOneShotKillDamage("ash_after_rebirth");
        ashDropRoll.setNext(0.0); // < 2% — гарантированный дроп

        TapResponse response = battleService.tap("ash_after_rebirth", 1);

        // ур.1: floor(1 + 1/100) = 1
        assertEquals("1", response.ashGained().display());
    }

    @Test
    void noDropWhenRollMissesChance() {
        authService.register("ash_miss", "password123");
        Player player = playerRepository.findByUsername("ash_miss").orElseThrow();
        player.setRebirthCount(1L);
        playerRepository.save(player);
        giveOneShotKillDamage("ash_miss");
        ashDropRoll.setNext(0.5); // >= 2% — промах

        TapResponse response = battleService.tap("ash_miss", 1);

        assertEquals("0", response.ashGained().display());
    }

    @Test
    void bossKillNeverDropsAsh() {
        authService.register("ash_boss", "password123");
        Player player = playerRepository.findByUsername("ash_boss").orElseThrow();
        player.setRebirthCount(1L);
        player.setCurrentLevel(5L);
        player.setMaxLevel(5L);
        player.setCurrentSubLevel(10);
        player.setCurrentMobHp(EconomyCurves.bossHp(5));
        playerRepository.save(player);
        giveOneShotKillDamage("ash_boss");
        ashDropRoll.setNext(0.0); // гарантированный бросок, но босс не роняет Пепел

        TapResponse response = battleService.tap("ash_boss", 1);

        assertTrue(response.bossDefeated());
        assertEquals("0", response.ashGained().display());
    }
}
