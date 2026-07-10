package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.SiegeBoss;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.SiegeBossDetailDto;
import com.ragnarok.idle.dto.SiegeBossSummaryDto;
import com.ragnarok.idle.mapper.SiegeBossMapper;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.SiegeBossRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only доступ к осадным боссам (сид V12). Ротация понедельная: активен ровно один босс,
 * каждую неделю — следующий по {@code rotation_order}, по кругу.
 * <p>Текущий HP статичен (роста по времени нет): {@code currentHp = base_hp × hp_multiplier},
 * где множитель задаётся редкостью (шанс 25%→×1, 15%→×2, 5%→×4).
 */
@Service
public class SiegeScheduleService {

    /** Якорь отсчёта недель ротации — понедельник запуска. */
    static final LocalDate LAUNCH_DATE = LocalDate.of(2026, 7, 13);

    /** Число боссов в цикле ротации. */
    private static final int ROTATION_SIZE = 7;

    private final SiegeBossRepository siegeBossRepository;
    private final SiegeBossMapper siegeBossMapper;
    private final Clock clock;

    public SiegeScheduleService(SiegeBossRepository siegeBossRepository, SiegeBossMapper siegeBossMapper,
                                 Clock clock) {
        this.siegeBossRepository = siegeBossRepository;
        this.siegeBossMapper = siegeBossMapper;
        this.clock = clock;
    }

    /** Активный (на текущей неделе) босс с защитниками и рассчитанным HP; 404 если ротация пуста. */
    public SiegeBossDetailDto getActiveBoss() {
        int order = activeOrder();
        SiegeBoss boss = siegeBossRepository.findByRotationOrder(order)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No siege boss for rotation order " + order));
        return siegeBossMapper.toDetail(boss, BigNumDto.from(currentHp(boss)));
    }

    /** Всё недельное расписание по порядку ротации (без защитников). */
    public List<SiegeBossSummaryDto> getWeekSchedule() {
        return siegeBossRepository.findAllByOrderByRotationOrderAsc().stream()
                .map(boss -> siegeBossMapper.toSummary(boss, BigNumDto.from(currentHp(boss))))
                .toList();
    }

    /** Детали босса по бизнес-ключу; 404 если не найден. */
    public SiegeBossDetailDto getBoss(String bossKey) {
        return siegeBossRepository.findByBossKey(bossKey)
                .map(boss -> siegeBossMapper.toDetail(boss, BigNumDto.from(currentHp(boss))))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Siege boss not found: " + bossKey));
    }

    /** Порядок активного босса 1..7 = floorMod(недель с запуска, 7) + 1; корректно и до запуска. */
    int activeOrder() {
        long weeks = ChronoUnit.WEEKS.between(LAUNCH_DATE, LocalDate.now(clock));
        return Math.floorMod(weeks, ROTATION_SIZE) + 1;
    }

    /** Текущий HP босса: base_hp × hp_multiplier (статично, через BigNum — HP может быть огромным). */
    BigNum currentHp(SiegeBoss boss) {
        return BigNum.of(boss.getBaseHp().doubleValue())
                .multiply(boss.getHpMultiplier().doubleValue())
                .floor();
    }
}
