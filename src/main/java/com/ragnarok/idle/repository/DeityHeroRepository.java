package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.DeityHero;
import com.ragnarok.idle.domain.Rarity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeityHeroRepository extends JpaRepository<DeityHero, Long> {

    List<DeityHero> findByRarity(Rarity rarity);

    Optional<DeityHero> findByHeroKey(String heroKey);
}
