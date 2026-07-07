package com.ragnarok.idle.domain;

import com.ragnarok.idle.math.BigNum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    /** Позиция в текущем уровне: 1..10 — обычные мобы, 11 — таймер-босс. */
    @Column(name = "current_sub_level", nullable = false)
    private Integer currentSubLevel;

    /** Остаток HP моба/босса, с которым сейчас бьётся игрок. */
    @Column(name = "current_mob_hp", nullable = false)
    private BigNum currentMobHp;

    /** Автопереход: true — обычная прогрессия (мобы → босс → след. уровень); false — фарм-цикл мобов уровня. */
    @Column(name = "auto_advance", nullable = false)
    private Boolean autoAdvance = true;

    /** Начало текущего боя с боссом (таймер 30 сек); null — игрок не на боссе. */
    @Column(name = "boss_started_at")
    private LocalDateTime bossStartedAt;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Long currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Long getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(Long maxLevel) {
        this.maxLevel = maxLevel;
    }

    public BigNum getGold() {
        return gold;
    }

    public void setGold(BigNum gold) {
        this.gold = gold;
    }

    public BigNum getAsh() {
        return ash;
    }

    public void setAsh(BigNum ash) {
        this.ash = ash;
    }

    public LocalDateTime getLastCollectedAt() {
        return lastCollectedAt;
    }

    public void setLastCollectedAt(LocalDateTime lastCollectedAt) {
        this.lastCollectedAt = lastCollectedAt;
    }

    public Integer getCurrentSubLevel() {
        return currentSubLevel;
    }

    public void setCurrentSubLevel(Integer currentSubLevel) {
        this.currentSubLevel = currentSubLevel;
    }

    public BigNum getCurrentMobHp() {
        return currentMobHp;
    }

    public void setCurrentMobHp(BigNum currentMobHp) {
        this.currentMobHp = currentMobHp;
    }

    public boolean isAutoAdvance() {
        return Boolean.TRUE.equals(autoAdvance);
    }

    public void setAutoAdvance(boolean autoAdvance) {
        this.autoAdvance = autoAdvance;
    }

    public LocalDateTime getBossStartedAt() {
        return bossStartedAt;
    }

    public void setBossStartedAt(LocalDateTime bossStartedAt) {
        this.bossStartedAt = bossStartedAt;
    }
}
