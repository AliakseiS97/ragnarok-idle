package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.SiegeBoss;
import com.ragnarok.idle.mapper.SiegeBossMapper;
import com.ragnarok.idle.mapper.SiegeBossMapperImpl;
import com.ragnarok.idle.repository.SiegeBossRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Юнит-тест ротации и формулы HP. Репозиторий замокан, маппер — реальный сгенерированный
 * {@link SiegeBossMapperImpl} (self-contained), время — детерминированный {@link Clock#fixed}.
 */
class SiegeScheduleServiceTest {

    private final SiegeBossMapper mapper = new SiegeBossMapperImpl();

    private SiegeScheduleService serviceAtWeek(int week) {
        LocalDate date = SiegeScheduleService.LAUNCH_DATE.plusWeeks(week);
        Clock clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        SiegeBossRepository repo = mock(SiegeBossRepository.class);
        // босс на любой запрошенный порядок — чтобы getActiveBoss мог смапиться; ключ кодирует порядок
        lenient().when(repo.findByRotationOrder(anyInt()))
                .thenAnswer(inv -> Optional.of(bossOfOrder(inv.getArgument(0))));
        return new SiegeScheduleService(repo, mapper, clock);
    }

    @Test
    void rotationCyclesWeeklyByOrder() {
        // неделя 0 -> порядок 1 (Фенрир), неделя 3 -> порядок 4 (Сурт), неделя 7 -> снова 1 (цикл)
        assertEquals("siege_fenrir", serviceAtWeek(0).getActiveBoss().bossKey());
        assertEquals("siege_surtr", serviceAtWeek(3).getActiveBoss().bossKey());
        assertEquals("siege_fenrir", serviceAtWeek(7).getActiveBoss().bossKey());
    }

    @Test
    void activeOrderIsFloorModPlusOne() {
        assertEquals(1, serviceAtWeek(0).activeOrder());
        assertEquals(4, serviceAtWeek(3).activeOrder());
        assertEquals(1, serviceAtWeek(7).activeOrder());
    }

    @Test
    void currentHpIsBaseTimesMultiplier() {
        SiegeScheduleService service = serviceAtWeek(0);

        // Фенрир: 1e9 × 1 = 1e9
        var fenrir = service.currentHp(boss("1000000000", "1.00"));
        assertEquals(9, fenrir.getExponent());
        assertEquals(1.0, fenrir.getMantissa(), 1e-9);

        // Сурт: 1e11 × 2 = 2e11
        assertEquals("200.00B", service.currentHp(boss("100000000000", "2.00")).toDisplayString());
        // Имир: 1e13 × 4 = 4e13
        assertEquals("40.00T", service.currentHp(boss("10000000000000", "4.00")).toDisplayString());
    }

    // --- helpers ---

    private static SiegeBoss boss(String baseHp, String multiplier) {
        SiegeBoss b = new SiegeBoss();
        b.setBaseHp(new BigInteger(baseHp));
        b.setHpMultiplier(new BigDecimal(multiplier));
        return b;
    }

    /** Полный босс для маппинга; bossKey кодирует порядок ротации (1 -> fenrir, 4 -> surtr). */
    private static SiegeBoss bossOfOrder(int order) {
        SiegeBoss b = boss("1000000000", "1.00");
        b.setRotationOrder(order);
        b.setBossKey(order == 4 ? "siege_surtr" : "siege_fenrir");
        b.setNameRu("x");
        b.setNameOriginal("x");
        b.setSpawnChance(new BigDecimal("25.00"));
        b.setMinRecommendedTier(1);
        b.setSkill1Name("x");
        b.setSkill1Description("x");
        b.setSkill2Name("x");
        b.setSkill2Description("x");
        b.setHireSkillName("x");
        b.setHireSkillDescription("x");
        b.setDefenders(List.of());
        return b;
    }
}
