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
 * Справочник сезонных ивентов (таблица {@code seasonal_events}, сид в миграции V10).
 * Read-only reference data. Soft delete по {@code is_deleted} через {@link SQLRestriction}.
 */
@Entity
@Table(name = "seasonal_events")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SeasonalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 50)
    private String eventKey;

    @Column(name = "name_ru", nullable = false, length = 50)
    private String nameRu;

    @Column(name = "name_original", nullable = false, length = 80)
    private String nameOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_month", nullable = false, length = 15)
    private EventMonth eventMonth;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "game_mechanics", nullable = false, length = 255)
    private String gameMechanics;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
