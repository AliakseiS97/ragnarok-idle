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
