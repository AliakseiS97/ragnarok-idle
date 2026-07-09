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
 * Справочник рун Старшего и Младшего Футарка (таблица {@code runes}, сид в миграции V10).
 * Read-only reference data. Soft delete по {@code is_deleted} через {@link SQLRestriction}.
 */
@Entity
@Table(name = "runes")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Rune {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "rune_key", nullable = false, unique = true, length = 50)
    private String runeKey;

    @Column(nullable = false, length = 5)
    private String symbol;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Futhark futhark;

    @Column(nullable = false)
    private int tier;

    @Column(length = 100)
    private String meaning;

    @Column(name = "effect_type", nullable = false, length = 30)
    private String effectType;

    @Column(name = "effect_value", nullable = false)
    private int effectValue;

    @Column(name = "effect_description", nullable = false, length = 255)
    private String effectDescription;

    @Column(name = "flavor_text")
    private String flavorText;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
