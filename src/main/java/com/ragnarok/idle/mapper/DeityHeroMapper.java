package com.ragnarok.idle.mapper;

import com.ragnarok.idle.domain.DeityHero;
import com.ragnarok.idle.dto.DeityHeroDto;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeityHeroMapper {

    DeityHeroDto toDto(DeityHero entity);

    List<DeityHeroDto> toDto(List<DeityHero> entities);
}
