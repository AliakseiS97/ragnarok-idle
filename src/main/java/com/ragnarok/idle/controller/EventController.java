package com.ragnarok.idle.controller;

import com.ragnarok.idle.domain.EventMonth;
import com.ragnarok.idle.dto.SeasonalEventDto;
import com.ragnarok.idle.service.SeasonalEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API справочника сезонных ивентов. */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final SeasonalEventService seasonalEventService;

    public EventController(SeasonalEventService seasonalEventService) {
        this.seasonalEventService = seasonalEventService;
    }

    @GetMapping
    public List<SeasonalEventDto> list(@RequestParam(name = "month", required = false) EventMonth month) {
        return seasonalEventService.find(month);
    }
}
