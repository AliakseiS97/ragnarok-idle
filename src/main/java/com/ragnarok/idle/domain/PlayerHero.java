package com.ragnarok.idle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Связь игрок-герой. Строка создаётся при первой покупке героя. */
@Entity
@Table(name = "player_hero", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "hero_id"}))
public class PlayerHero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "hero_id", nullable = false)
    private Long heroId;

    @Column(nullable = false)
    private Long level;

    @Column(nullable = false)
    private boolean activated;

    /**
     * Ранг Саги 1..N (GDD §3.8, см. {@link SagaRank}). Задаёт потолок уровня («стену») и множитель DPS.
     * Свежекупленный герой — ранг 1 «Обычный».
     */
    @Column(name = "saga_rank", nullable = false)
    private int sagaRank = Saga.FIRST_RANK;

    /** Под-ступень Саги I..X внутри текущего ранга (1..{@link Saga#SUB_STEPS_PER_RANK}). */
    @Column(name = "saga_sub_step", nullable = false)
    private int sagaSubStep = Saga.FIRST_SUB_STEP;

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getHeroId() {
        return heroId;
    }

    public void setHeroId(Long heroId) {
        this.heroId = heroId;
    }

    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public int getSagaRank() {
        return sagaRank;
    }

    public void setSagaRank(int sagaRank) {
        this.sagaRank = sagaRank;
    }

    public int getSagaSubStep() {
        return sagaSubStep;
    }

    public void setSagaSubStep(int sagaSubStep) {
        this.sagaSubStep = sagaSubStep;
    }
}
