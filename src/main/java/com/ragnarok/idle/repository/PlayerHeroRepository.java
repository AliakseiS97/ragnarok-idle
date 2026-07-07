package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.PlayerHero;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerHeroRepository extends JpaRepository<PlayerHero, Long> {

    List<PlayerHero> findByPlayerId(Long playerId);

    Optional<PlayerHero> findByPlayerIdAndHeroId(Long playerId, Long heroId);
}
