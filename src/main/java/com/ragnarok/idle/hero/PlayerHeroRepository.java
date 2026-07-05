package com.ragnarok.idle.hero;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerHeroRepository extends JpaRepository<PlayerHero, Long> {

    List<PlayerHero> findByPlayerId(Long playerId);

    Optional<PlayerHero> findByPlayerIdAndHeroId(Long playerId, Long heroId);
}
