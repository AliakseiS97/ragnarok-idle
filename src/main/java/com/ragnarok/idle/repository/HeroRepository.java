package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.Hero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroRepository extends JpaRepository<Hero, Long> {
}
