package com.ragnarok.idle.simulator;

import com.ragnarok.idle.math.BigNum;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Тесты симулятора баланса: детекция стены и ускорение прохождения Славой. */
class BalanceSimulatorTest {

    /**
     * Детекция стены на искусственно недостижимом боссе: frontier застыл на этаже 5 (босс не пробивается),
     * стена должна зафиксироваться сразу после превышения порога и закрыться при продвижении.
     */
    @Test
    void detectsWallOnStuckFrontier() {
        WallTracker tracker = new WallTracker(600);

        assertNull(tracker.onTick(0, 5), "попадание на этаж 5 — не стена");
        for (long t = 1; t <= 600; t++) {
            assertNull(tracker.onTick(t, 5), "в пределах порога стены ещё нет");
        }

        WallTracker.Wall wall = tracker.onTick(601, 5); // застой > 600с
        assertNotNull(wall, "застой дольше порога => стена");
        assertEquals(5, wall.floor);
        assertEquals(1, tracker.walls().size());

        // продвижение на этаж 6 закрывает стену; длительность = всё время застоя на этаже 5
        assertNull(tracker.onTick(900, 6));
        assertEquals(900, wall.durationSeconds(900));
    }

    /**
     * Ненулевая Слава ускоряет прохождение: она множит DPS (HeroService.ASH_DPS_STEP), поэтому в режиме,
     * где урон — бутылочное горлышко, тот же рубеж достигается за меньшее игровое время. Это и есть
     * механизм, из-за которого 2-е прохождение (со стартовой Славой) быстрее 1-го.
     */
    @Test
    void ashSpeedsUpProgression() {
        long maxSeconds = 200_000;
        long floor = 60;
        long withoutAsh = BalanceSimulator.secondsToReachFloor(5, BigNum.ZERO, floor, maxSeconds);
        long withAsh = BalanceSimulator.secondsToReachFloor(5, BigNum.of(1_000_000), floor, maxSeconds);

        assertTrue(withoutAsh > 0, "без Славы рубеж достигнут: " + withoutAsh);
        assertTrue(withAsh > 0, "с Славой рубеж достигнут: " + withAsh);
        assertTrue(withAsh < withoutAsh,
                "с Славой должно быть быстрее: сСлавой=%d, безСлавы=%d".formatted(withAsh, withoutAsh));
    }
}
