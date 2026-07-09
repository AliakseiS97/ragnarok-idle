package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.Artifact;
import com.ragnarok.idle.domain.DeityHero;
import com.ragnarok.idle.domain.EventMonth;
import com.ragnarok.idle.domain.Futhark;
import com.ragnarok.idle.domain.HeroRole;
import com.ragnarok.idle.domain.Rarity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционный тест на сид мифологии (V10) через реальные репозитории.
 * БД — H2 в PostgreSQL-режиме, схему и данные накатывает Flyway (как и вся тестовая среда проекта).
 */
@SpringBootTest
class MythologyRepositoryTest {

    @Autowired
    private DeityHeroRepository deityHeroRepository;

    @Autowired
    private RuneRepository runeRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private SeasonalEventRepository seasonalEventRepository;

    @Test
    void seedRowCountsMatchMigration() {
        assertEquals(34, deityHeroRepository.findAll().size());
        assertEquals(40, runeRepository.findAll().size());
        assertEquals(6, artifactRepository.findAll().size());
        assertEquals(5, seasonalEventRepository.findAll().size());
    }

    @Test
    void findByHeroKeyReturnsMappedGod() {
        DeityHero odin = deityHeroRepository.findByHeroKey("god_odin").orElseThrow();

        assertEquals("Один", odin.getNameRu());
        assertEquals("Óðinn", odin.getNameOriginal());
        assertEquals(Rarity.LEGENDARY, odin.getRarity());
        assertEquals(HeroRole.UTILITY, odin.getRole());
    }

    @Test
    void findByRarityFiltersGods() {
        assertEquals(8, deityHeroRepository.findByRarity(Rarity.LEGENDARY).size());
    }

    @Test
    void findByFutharkAndTierFilterRunes() {
        assertEquals(24, runeRepository.findByFuthark(Futhark.ELDER).size());
        assertEquals(16, runeRepository.findByFuthark(Futhark.YOUNGER).size());
        assertEquals(8, runeRepository.findByTier(1).size());
        assertEquals(16, runeRepository.findByTier(4).size());
    }

    @Test
    void findByEventMonthFiltersEvents() {
        // В январе два ивента: Йоль и Торраблот.
        assertEquals(2, seasonalEventRepository.findByEventMonth(EventMonth.JANUARY).size());
    }

    @Test
    void mjolnirIsOwnedByThor() {
        // findAll грузит владельца через @EntityGraph — доступ к LAZY-связи вне транзакции безопасен.
        Artifact mjolnir = artifactRepository.findAll().stream()
                .filter(artifact -> "artifact_mjolnir".equals(artifact.getArtifactKey()))
                .findFirst()
                .orElseThrow();

        assertTrue(mjolnir.getOwner() != null, "у Мьёльнира должен быть владелец");
        assertEquals("god_thor", mjolnir.getOwner().getHeroKey());
        assertEquals("Тор", mjolnir.getOwner().getNameRu());
    }
}
