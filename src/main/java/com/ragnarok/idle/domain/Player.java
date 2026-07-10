package com.ragnarok.idle.domain;

import com.ragnarok.idle.math.BigNum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "player")
@Getter
@Setter
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Оптимистическая блокировка: без неё конкурентные запросы (тап/тик пассивного DPS/ребёрт)
     * делают read-modify-write по одной и той же строке без проверки версии — "последний записавший
     * побеждает" и молча теряет чужие изменения (напр. тап, зависший в полёте, может откатить часть
     * сброса при ребёрте). С @Version конкурентная запись кидает ObjectOptimisticLockingFailureException
     * вместо тихой потери данных.
     */
    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "current_level", nullable = false)
    private Long currentLevel;

    @Column(name = "max_level", nullable = false)
    private Long maxLevel;

    @Column(nullable = false)
    private BigNum gold;

    @Column(nullable = false)
    private BigNum ash;

    @Column(name = "last_collected_at", nullable = false)
    private LocalDateTime lastCollectedAt;

    /**
     * Позиция в текущем уровне: 1..10 — подлокации ОБЫЧНОГО уровня. На боссовом уровне (каждый 5-й,
     * {@link com.ragnarok.idle.service.CombatEngine#isBossLevel}) всегда 1 — сам уровень целиком
     * один бой с боссом, подлокаций-мобов там нет.
     */
    @Column(name = "current_sub_level", nullable = false)
    private Integer currentSubLevel;

    /** Остаток HP моба/босса, с которым сейчас бьётся игрок. */
    @Column(name = "current_mob_hp", nullable = false)
    private BigNum currentMobHp;

    /** Автопереход: true — обычная прогрессия (мобы → босс → след. уровень); false — фарм-цикл мобов уровня. */
    @Column(name = "auto_advance", nullable = false)
    private boolean autoAdvance = true;

    /** Начало текущего боя с боссом (таймер 30 сек); null — игрок не на боссе. */
    @Column(name = "boss_started_at")
    private LocalDateTime bossStartedAt;

    /** Сколько раз игрок перерождался. Дроп Славы с мобов открывается только при rebirthCount >= 1. */
    @Column(name = "rebirth_count", nullable = false)
    private Long rebirthCount = 0L;
}
