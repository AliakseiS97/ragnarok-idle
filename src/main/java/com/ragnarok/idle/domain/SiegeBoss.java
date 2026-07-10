package com.ragnarok.idle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Осадный босс недельной ротации (таблица {@code siege_bosses}, сид в миграции V12).
 * Активный босс недели выбирается по {@code rotation_order} (см. {@code SiegeScheduleService}).
 * Read-only reference data. Soft delete по {@code is_deleted} через {@link SQLRestriction}.
 */
@Entity
@Table(name = "siege_bosses")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiegeBoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "boss_key", nullable = false, unique = true, length = 50)
    private String bossKey;

    @Column(name = "name_ru", nullable = false, length = 50)
    private String nameRu;

    @Column(name = "name_original", nullable = false, length = 50)
    private String nameOriginal;

    /** Порядок в понедельной ротации 1..7 (уникален). */
    @Column(name = "rotation_order", nullable = false, unique = true)
    private int rotationOrder;

    @Column(name = "base_hp", nullable = false, precision = 19, scale = 0)
    private BigInteger baseHp;

    /** Шанс появления босса (25/15/5%) — под будущий спавн-ролл. */
    @Column(name = "spawn_chance", nullable = false, precision = 5, scale = 2)
    private BigDecimal spawnChance;

    /** Множитель HP от редкости (25%→×1, 15%→×2, 5%→×4). currentHp = base_hp × hp_multiplier. */
    @Column(name = "hp_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal hpMultiplier;

    /** Таймер до вайпа; {@code null} у боссов без него. */
    @Column(name = "enrage_seconds")
    private Integer enrageSeconds;

    @Column(name = "skill1_name", nullable = false, length = 100)
    private String skill1Name;

    @Column(name = "skill1_description", nullable = false, length = 255)
    private String skill1Description;

    @Column(name = "skill2_name", nullable = false, length = 100)
    private String skill2Name;

    @Column(name = "skill2_description", nullable = false, length = 255)
    private String skill2Description;

    @Column(name = "hire_skill_name", nullable = false, length = 100)
    private String hireSkillName;

    @Column(name = "hire_skill_description", nullable = false, length = 255)
    private String hireSkillDescription;

    @Column(name = "min_recommended_tier", nullable = false)
    private int minRecommendedTier;

    @OneToMany(mappedBy = "boss", fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<SiegeDefender> defenders = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
