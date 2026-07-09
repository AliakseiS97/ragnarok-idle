package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.EventMonth;
import com.ragnarok.idle.dto.SeasonalEventDto;
import com.ragnarok.idle.mapper.SeasonalEventMapper;
import com.ragnarok.idle.repository.SeasonalEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Read-only доступ к справочнику сезонных ивентов (сид V10). */
@Service
public class SeasonalEventService {

    private final SeasonalEventRepository seasonalEventRepository;
    private final SeasonalEventMapper seasonalEventMapper;

    public SeasonalEventService(SeasonalEventRepository seasonalEventRepository,
                                 SeasonalEventMapper seasonalEventMapper) {
        this.seasonalEventRepository = seasonalEventRepository;
        this.seasonalEventMapper = seasonalEventMapper;
    }

    /** Все ивенты, либо только заданного месяца (если {@code month != null}). */
    public List<SeasonalEventDto> find(EventMonth month) {
        return seasonalEventMapper.toDto(month == null
                ? seasonalEventRepository.findAll()
                : seasonalEventRepository.findByEventMonth(month));
    }
}
