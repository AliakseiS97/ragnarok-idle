package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.Artifact;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    /** Грузим владельца одним запросом (fetch join), чтобы не ловить N+1 при маппинге в DTO. */
    @Override
    @EntityGraph(attributePaths = "owner")
    List<Artifact> findAll();
}
