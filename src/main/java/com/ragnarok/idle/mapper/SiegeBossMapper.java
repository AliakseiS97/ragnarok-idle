package com.ragnarok.idle.mapper;

import com.ragnarok.idle.domain.SiegeBoss;
import com.ragnarok.idle.domain.SiegeDefender;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.SiegeBossDetailDto;
import com.ragnarok.idle.dto.SiegeBossSummaryDto;
import com.ragnarok.idle.dto.SiegeDefenderDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SiegeBossMapper {

    // currentHp — вычисляемое (base_hp × hp_multiplier), передаём отдельным параметром;
    // остальные поля мапятся по имени с boss, defenders — через список-метод ниже.
    @Mapping(target = "currentHp", source = "currentHp")
    SiegeBossSummaryDto toSummary(SiegeBoss boss, BigNumDto currentHp);

    @Mapping(target = "currentHp", source = "currentHp")
    SiegeBossDetailDto toDetail(SiegeBoss boss, BigNumDto currentHp);

    SiegeDefenderDto toDefenderDto(SiegeDefender defender);

    List<SiegeDefenderDto> toDefenderDtos(List<SiegeDefender> defenders);
}
