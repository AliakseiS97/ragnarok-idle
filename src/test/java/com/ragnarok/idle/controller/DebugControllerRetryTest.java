package com.ragnarok.idle.controller;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ретрай тайм-тревела на конфликт @Version (гонка с поллингом /player/me, п.2 правки ТЗ).
 * Без Spring и без реальной конкурентности — фейковый счётчик бросает конфликт N раз подряд.
 */
class DebugControllerRetryTest {

    @Test
    void succeedsAfterTransientConflicts() {
        AtomicInteger calls = new AtomicInteger(0);

        String result = DebugController.withOptimisticRetry(3, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new ObjectOptimisticLockingFailureException("Player", 1L);
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, calls.get(), "успех на 3-й попытке — ровно 3 вызова");
    }

    @Test
    void succeedsImmediatelyWithoutConflict() {
        AtomicInteger calls = new AtomicInteger(0);

        String result = DebugController.withOptimisticRetry(3, () -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(1, calls.get(), "нет конфликта — ни одного лишнего повтора");
    }

    @Test
    void throwsAfterExhaustingAttempts() {
        AtomicInteger calls = new AtomicInteger(0);

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                DebugController.withOptimisticRetry(3, () -> {
                    calls.incrementAndGet();
                    throw new ObjectOptimisticLockingFailureException("Player", 1L);
                }));

        assertEquals(3, calls.get(), "должно быть ровно maxAttempts попыток, не больше и не меньше");
    }
}
