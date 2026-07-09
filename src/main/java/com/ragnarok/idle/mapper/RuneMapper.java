package com.ragnarok.idle.mapper;

import com.ragnarok.idle.domain.Rune;
import com.ragnarok.idle.dto.RuneDto;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RuneMapper {

    RuneDto toDto(Rune entity);

    List<RuneDto> toDto(List<Rune> entities);
}
