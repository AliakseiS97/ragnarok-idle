package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.dto.PlayerHeroResponse;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class HeroServiceTest {

    @Autowired
    private HeroService heroService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void upgradeCostGrowsSevenPercentPerLevel() {
        authService.register("hero_upgrader", "password123");
        Player player = playerRepository.findByUsername("hero_upgrader").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        heroService.buy("hero_upgrader", 1L); // Трэлл, цена найма 50

        // ур.1->2 = 50 * 1.07^0 = 50; ур.2->3 = ceil(50 * 1.07) = 54 — цена обязана расти и быть целой.
        PlayerHeroResponse first = heroService.upgrade("hero_upgrader", 1L, 1);
        PlayerHeroResponse second = heroService.upgrade("hero_upgrader", 1L, 1);

        assertEquals("50", first.goldSpent().display());
        assertEquals("54", second.goldSpent().display());
        assertEquals(3L, second.level());
    }

    @Test
    void dpsGrowthIsVisibleFromFirstUpgrade() {
        authService.register("hero_dps", "password123");
        Player player = playerRepository.findByUsername("hero_dps").orElseThrow();
        player.setGold(BigNum.of(1000));
        playerRepository.save(player);

        heroService.buy("hero_dps", 1L);
        // ур.1: DPS 5; ур.2: round(5 × 1.1) = 6 — прирост +10% виден сразу (раньше floor съедал его)
        assertEquals("5", heroService.passiveDpsByHero(player.getId()).get(1L).toDisplayString());

        heroService.upgrade("hero_dps", 1L, 1);
        assertEquals("6", heroService.passiveDpsByHero(player.getId()).get(1L).toDisplayString());
    }

    @Test
    void bafferBonusIsZeroBelowMilestoneLevel() {
        authService.register("baffer_below", "password123");
        Player player = playerRepository.findByUsername("baffer_below").orElseThrow();
        player.setGold(BigNum.of(1_000_000));
        playerRepository.save(player);

        heroService.buy("baffer_below", 1L); // Трэлл (ACTIVE), dps 5
        heroService.buy("baffer_below", 3L); // Скальд (BAFFER), ур.1
        heroService.upgrade("baffer_below", 3L, 23); // Скальд -> ур.24, ниже вехи 25

        // ниже вехи бафер не даёт общего бонуса — DPS Трэлла остаётся базовым (5)
        assertEquals("5", heroService.passiveDpsByHero(player.getId()).get(1L).toDisplayString());
    }

    @Test
    void bafferMilestoneGrantsOneTimeTwentyFivePercentAndDoesNotGrowFurther() {
        authService.register("baffer_milestone", "password123");
        Player player = playerRepository.findByUsername("baffer_milestone").orElseThrow();
        player.setGold(BigNum.of(1_000_000));
        playerRepository.save(player);

        heroService.buy("baffer_milestone", 1L); // Трэлл (ACTIVE), dps 5
        heroService.buy("baffer_milestone", 3L); // Скальд (BAFFER), ур.1
        heroService.upgrade("baffer_milestone", 3L, 24); // Скальд -> ур.25, веха достигнута

        // Трэлл: 5 x (1 + 0.25) = 6.25 -> round к ближайшему целому = 6
        assertEquals("6", heroService.passiveDpsByHero(player.getId()).get(1L).toDisplayString());

        heroService.upgrade("baffer_milestone", 3L, 25); // Скальд -> ур.50, веха уже была учтена

        // общий бонус команды не растёт дальше — Трэлл всё ещё +25%, а не больше
        assertEquals("6", heroService.passiveDpsByHero(player.getId()).get(1L).toDisplayString());
    }

    @Test
    void rebalancedSeedMatchesReferenceCurve() {
        authService.register("hero_pricing", "password123");
        Player player = playerRepository.findByUsername("hero_pricing").orElseThrow();
        player.setGold(BigNum.of(1_000_000));
        playerRepository.save(player);

        // Кривая из economy_constants.md: цена 50 x5^(id-1), 2-й герой = 250.
        PlayerHeroResponse first = heroService.buy("hero_pricing", 1L);
        PlayerHeroResponse second = heroService.buy("hero_pricing", 2L);

        assertEquals("50", first.goldSpent().display());
        assertEquals("250", second.goldSpent().display());
    }
}
