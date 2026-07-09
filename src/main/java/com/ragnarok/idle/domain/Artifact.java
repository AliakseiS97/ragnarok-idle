package com.ragnarok.idle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Справочник легендарных артефактов (таблица {@code artifacts}, сид в миграции V10).
 * Read-only reference data. Soft delete по {@code is_deleted} через {@link SQLRestriction}.
 */
@Entity
@Table(name = "artifacts")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "artifact_key", nullable = false, unique = true, length = 50)
    private String artifactKey;

    @Column(name = "name_ru", nullable = false, length = 50)
    private String nameRu;

    @Column(name = "name_original", nullable = false, length = 50)
    private String nameOriginal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rarity rarity;

    /**
     * Владелец артефакта. Связь по бизнес-ключу {@code owner_hero_key -> deity_heroes.hero_key},
     * а не по id. LAZY — грузим только когда нужно (в списке — через {@code @EntityGraph}, без N+1).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_hero_key", referencedColumnName = "hero_key",
            insertable = false, updatable = false)
    private DeityHero owner;

    @Column(nullable = false, length = 255)
    private String effect;

    @Column(name = "set_bonus", length = 255)
    private String setBonus;

    /** Внутреннее поле (промпт для генерации арта) — в DTO/API не отдаётся. */
    @Column(name = "art_prompt")
    private String artPrompt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
