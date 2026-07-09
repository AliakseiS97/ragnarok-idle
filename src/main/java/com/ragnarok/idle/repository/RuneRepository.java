package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.Futhark;
import com.ragnarok.idle.domain.Rune;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuneRepository extends JpaRepository<Rune, Long> {

    List<Rune> findByFuthark(Futhark futhark);

    List<Rune> findByTier(int tier);
}
