package com.ragnarok.idle.mapper;

import com.ragnarok.idle.domain.SeasonalEvent;
import com.ragnarok.idle.dto.SeasonalEventDto;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeasonalEventMapper {

    SeasonalEventDto toDto(SeasonalEvent entity);

    List<SeasonalEventDto> toDto(List<SeasonalEvent> entities);
}
