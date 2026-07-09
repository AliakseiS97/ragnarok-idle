package com.ragnarok.idle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Справочник героев-божеств скандинавского пантеона (таблица {@code deity_heroes},
 * сид в миграции V10). Read-only reference data. Soft delete по {@code is_deleted}
 * через {@link SQLRestriction} — удалённые строки не попадают ни в один запрос.
 */
@Entity
@Table(name = "deity_heroes")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeityHero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "hero_key", nullable = false, unique = true, length = 50)
    private String heroKey;

    @Column(name = "name_ru", nullable = false, length = 50)
    private String nameRu;

    @Column(name = "name_original", nullable = false, length = 50)
    private String nameOriginal;

    @Column(nullable = false, length = 20)
    private String pantheon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rarity rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private HeroRole role;

    @Column(name = "rune_anchor", length = 30)
    private String runeAnchor;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "skill_description", nullable = false, length = 255)
    private String skillDescription;

    /** Внутреннее поле (источник лора) — в DTO/API не отдаётся. */
    @Column(name = "lore_source", length = 100)
    private String loreSource;

    /** Внутреннее поле (промпт для генерации арта) — в DTO/API не отдаётся. */
    @Column(name = "art_prompt")
    private String artPrompt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
