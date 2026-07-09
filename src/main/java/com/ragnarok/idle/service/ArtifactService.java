package com.ragnarok.idle.service;

import com.ragnarok.idle.dto.ArtifactDto;
import com.ragnarok.idle.mapper.ArtifactMapper;
import com.ragnarok.idle.repository.ArtifactRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Read-only доступ к справочнику артефактов (сид V10). */
@Service
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactMapper artifactMapper;

    public ArtifactService(ArtifactRepository artifactRepository, ArtifactMapper artifactMapper) {
        this.artifactRepository = artifactRepository;
        this.artifactMapper = artifactMapper;
    }

    /** Все артефакты с подгруженным владельцем (через {@code @EntityGraph}, без N+1). */
    public List<ArtifactDto> findAll() {
        return artifactMapper.toDto(artifactRepository.findAll());
    }
}
