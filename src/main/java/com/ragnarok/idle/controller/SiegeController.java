package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.SiegeBossDetailDto;
import com.ragnarok.idle.dto.SiegeBossSummaryDto;
import com.ragnarok.idle.service.SiegeScheduleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API осадных боссов (недельная ротация). */
@RestController
@RequestMapping("/api/siege")
public class SiegeController {

    private final SiegeScheduleService siegeScheduleService;

    public SiegeController(SiegeScheduleService siegeScheduleService) {
        this.siegeScheduleService = siegeScheduleService;
    }

    /** Активный (этой недели) босс с защитниками и текущим HP. */
    @GetMapping("/today")
    public SiegeBossDetailDto today() {
        return siegeScheduleService.getActiveBoss();
    }

    /** Всё недельное расписание по порядку ротации (без защитников). */
    @GetMapping("/schedule")
    public List<SiegeBossSummaryDto> schedule() {
        return siegeScheduleService.getWeekSchedule();
    }

    /** Детали конкретного босса; 404 если ключа нет. */
    @GetMapping("/bosses/{bossKey}")
    public SiegeBossDetailDto boss(@PathVariable("bossKey") String bossKey) {
        return siegeScheduleService.getBoss(bossKey);
    }
}
