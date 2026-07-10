package com.ragnarok.idle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Защитник осадного босса (таблица {@code siege_defenders}, сид в миграции V12).
 * Read-only reference data. Soft delete по {@code is_deleted} через {@link SQLRestriction}.
 */
@Entity
@Table(name = "siege_defenders")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiegeDefender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_id", nullable = false)
    private SiegeBoss boss;

    @Column(name = "name_ru", nullable = false, length = 50)
    private String nameRu;

    /** Порядок защитника в бою. */
    @Column(nullable = false)
    private int position;

    @Column(name = "hp_percent_of_boss", nullable = false, precision = 5, scale = 2)
    private BigDecimal hpPercentOfBoss;

    @Column(name = "skill_description", nullable = false, length = 255)
    private String skillDescription;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
