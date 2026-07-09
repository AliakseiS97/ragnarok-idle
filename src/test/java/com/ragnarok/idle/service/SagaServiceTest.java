package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.domain.Saga;
import com.ragnarok.idle.domain.SagaRank;
import com.ragnarok.idle.dto.SagaPromoteResponse;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Механика Саги: стена (потолок), повышение-заглушка, множитель DPS (GDD §3.8). */
@SpringBootTest
class SagaServiceTest {

    private static final long TRELL = 1L; // Трэлл, base_dps 5, цена найма 50

    @Autowired
    private HeroService heroService;

    @Autowired
    private SagaService sagaService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerHeroRepository playerHeroRepository;

    private PlayerHero buyTrellWithGold(String username) {
        authService.register(username, "password123");
        Player player = playerRepository.findByUsername(username).orElseThrow();
        player.setGold(BigNum.of(1e20)); // хватит даже на апгрейд у потолка (500-й уровень)
        playerRepository.save(player);
        heroService.buy(username, TRELL);
        return playerHeroRepository.findByPlayerIdAndHeroId(player.getId(), TRELL).orElseThrow();
    }

    private PlayerHero reload(PlayerHero ph) {
        return playerHeroRepository.findById(ph.getId()).orElseThrow();
    }

    @Test
    void levelCapBlocksUpgrade() {
        PlayerHero ph = buyTrellWithGold("saga_cap");
        ph.setLevel(SagaRank.COMMON.getLevelCap()); // ур.500 — у стены Обычного
        playerHeroRepository.save(ph);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> heroService.upgrade("saga_cap", TRELL, 1));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("потолок"), "ошибка должна объяснять про стену Саги");
    }

    @Test
    void promotingSagaLiftsTheWall() {
        PlayerHero ph = buyTrellWithGold("saga_wall");
        ph.setLevel(SagaRank.COMMON.getLevelCap()); // ур.500, у стены
        ph.setSagaSubStep(Saga.SUB_STEPS_PER_RANK);  // ступень X — следующий промоушен = новый ранг
        playerHeroRepository.save(ph);

        // до повышения — апгрейд заблокирован
        assertThrows(ResponseStatusException.class, () -> heroService.upgrade("saga_wall", TRELL, 1));

        SagaPromoteResponse promoted = sagaService.promote("saga_wall", TRELL); // X → Редкий I
        assertEquals(2, promoted.sagaRank());
        assertEquals(1, promoted.sagaSubStep());
        assertEquals(SagaRank.RARE.getLevelCap(), promoted.sagaLevelCap());

        // стена поднялась — апгрейд проходит
        heroService.upgrade("saga_wall", TRELL, 1);
        assertEquals(SagaRank.COMMON.getLevelCap() + 1, reload(ph).getLevel());
    }

    @Test
    void promoteAdvancesSubStepThenRollsIntoNextRank() {
        PlayerHero ph = buyTrellWithGold("saga_promote");
        ph.setSagaSubStep(Saga.SUB_STEPS_PER_RANK - 1); // ступень IX
        playerHeroRepository.save(ph);

        sagaService.promote("saga_promote", TRELL); // IX → X, ранг тот же
        PlayerHero afterX = reload(ph);
        assertEquals(1, afterX.getSagaRank());
        assertEquals(Saga.SUB_STEPS_PER_RANK, afterX.getSagaSubStep());

        sagaService.promote("saga_promote", TRELL); // X → новый ранг, ступень в I
        PlayerHero afterRankUp = reload(ph);
        assertEquals(2, afterRankUp.getSagaRank());
        assertEquals(Saga.FIRST_SUB_STEP, afterRankUp.getSagaSubStep());
    }

    @Test
    void sagaMultiplierIsAppliedToHeroDps() {
        PlayerHero ph = buyTrellWithGold("saga_dps");
        long playerId = ph.getPlayerId();

        // Обычный I: множитель ×1 → базовый DPS Трэлла = 5
        assertEquals("5", heroService.passiveDpsByHero(playerId).get(TRELL).toDisplayString());

        // Обычный II: ×1.25 → 5×1.25 = 6.25 → округление к ближайшему = 6
        sagaService.promote("saga_dps", TRELL);
        assertEquals("6", heroService.passiveDpsByHero(playerId).get(TRELL).toDisplayString());

        // Редкий I: ×6.0 (+500%) → 5×6 = 30
        ph.setSagaRank(2);
        ph.setSagaSubStep(1);
        playerHeroRepository.save(ph);
        assertEquals("30", heroService.passiveDpsByHero(playerId).get(TRELL).toDisplayString());

        // Редкий III: ×6.5 → 5×6.5 = 32.5 → округление к ближайшему = 33
        ph.setSagaSubStep(3);
        playerHeroRepository.save(ph);
        assertEquals("33", heroService.passiveDpsByHero(playerId).get(TRELL).toDisplayString());
    }

    @Test
    void promoteRequiresOwnedHero() {
        authService.register("saga_unowned", "password123");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sagaService.promote("saga_unowned", TRELL));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void promoteRejectedAtMaxSaga() {
        PlayerHero ph = buyTrellWithGold("saga_max");
        ph.setSagaRank(SagaRank.maxRank());
        ph.setSagaSubStep(Saga.SUB_STEPS_PER_RANK);
        playerHeroRepository.save(ph);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sagaService.promote("saga_max", TRELL));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
