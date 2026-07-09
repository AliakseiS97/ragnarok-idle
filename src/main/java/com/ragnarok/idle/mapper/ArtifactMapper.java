package com.ragnarok.idle.mapper;

import com.ragnarok.idle.domain.Artifact;
import com.ragnarok.idle.dto.ArtifactDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArtifactMapper {

    // Владельца разворачиваем плоско; при null-владельце MapStruct вернёт null для обоих полей.
    @Mapping(target = "ownerHeroKey", source = "owner.heroKey")
    @Mapping(target = "ownerNameRu", source = "owner.nameRu")
    ArtifactDto toDto(Artifact entity);

    List<ArtifactDto> toDto(List<Artifact> entities);
}
