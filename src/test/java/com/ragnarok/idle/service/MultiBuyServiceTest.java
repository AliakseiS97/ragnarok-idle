package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.domain.SagaRank;
import com.ragnarok.idle.dto.PlayerHeroResponse;
import com.ragnarok.idle.economy.PurchaseMode;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.PlayerHeroRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Мультипокупка апгрейдов (серверный расчёт пачки, GDD §12.7).
 * Кривая Трэлла (heroId 1, цена найма 50, рост ×1.07, целые цены):
 * ур.1→2=50, 2→3=54, 3→4=58, 4→5=62, 5→6=66 → сумма x5 = 290 (а не 50×5=250).
 */
@SpringBootTest
class MultiBuyServiceTest {

    private static final long TRELL = 1L;

    @Autowired
    private HeroService heroService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerHeroRepository playerHeroRepository;

    /** Регистрирует игрока, кладёт стартовое золото и покупает Трэлла (−50). */
    private Player buyTrell(String username, double startGold) {
        authService.register(username, "password123");
        Player player = playerRepository.findByUsername(username).orElseThrow();
        player.setGold(BigNum.of(startGold));
        playerRepository.save(player);
        heroService.buy(username, TRELL);
        return playerRepository.findByUsername(username).orElseThrow();
    }

    private void setGold(String username, double gold) {
        Player player = playerRepository.findByUsername(username).orElseThrow();
        player.setGold(BigNum.of(gold));
        playerRepository.save(player);
    }

    private PlayerHero reloadHero(Long playerId) {
        return playerHeroRepository.findByPlayerIdAndHeroId(playerId, TRELL).orElseThrow();
    }

    @Test
    void fixedModeChargesSumOfProgression() {
        buyTrell("mb_x5", 1000); // после покупки Трэлла остаётся 950 золота

        PlayerHeroResponse response = heroService.upgrade("mb_x5", TRELL, PurchaseMode.X5);

        assertEquals(5L, response.levelsBought());
        assertEquals(6L, response.level());
        assertEquals("290", response.goldSpent().display(), "сумма прогрессии, а не 50×5=250");
        assertEquals("660", response.goldRemaining().display()); // 950 − 290
    }

    @Test
    void fixedModeBlocksWhenGoldShortOfFullBatchAndBuysNothing() {
        Player player = buyTrell("mb_block", 1000);
        setGold("mb_block", 289); // на 1 меньше полной цены x5 (290)

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> heroService.upgrade("mb_block", TRELL, PurchaseMode.X5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        // частичной покупки нет: ни уровень, ни золото не изменились
        assertEquals(1L, reloadHero(player.getId()).getLevel());
        assertEquals("289", playerRepository.findById(player.getId()).orElseThrow().getGold().toDisplayString());
    }

    @Test
    void maxBuysExactlyWhatGoldAllows() {
        buyTrell("mb_max_exact", 1000);
        setGold("mb_max_exact", 290); // впритык на 5 уровней (6-й стоит 71)

        PlayerHeroResponse response = heroService.upgrade("mb_max_exact", TRELL, PurchaseMode.MAX);

        assertEquals(5L, response.levelsBought());
        assertEquals(6L, response.level());
        assertEquals("290", response.goldSpent().display());
        assertEquals("0", response.goldRemaining().display());
    }

    @Test
    void maxRejectedWhenGoldBelowFirstLevel() {
        Player player = buyTrell("mb_max_zero", 1000);
        setGold("mb_max_zero", 49); // меньше цены первого уровня (50)

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> heroService.upgrade("mb_max_zero", TRELL, PurchaseMode.MAX));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        assertEquals(1L, reloadHero(player.getId()).getLevel());
        assertEquals("49", playerRepository.findById(player.getId()).orElseThrow().getGold().toDisplayString());
    }

    @Test
    void maxStopsAtSagaWall() {
        Player player = buyTrell("mb_max_wall", 1e20); // золото не ограничивает — упрёмся в стену
        PlayerHero ph = reloadHero(player.getId());
        ph.setLevel(SagaRank.COMMON.getLevelCap() - 2); // ур.498, потолок Обычного = 500
        playerHeroRepository.save(ph);

        PlayerHeroResponse response = heroService.upgrade("mb_max_wall", TRELL, PurchaseMode.MAX);

        assertEquals(2L, response.levelsBought());
        assertEquals(SagaRank.COMMON.getLevelCap(), response.level()); // ровно у стены, не за неё
    }
}
