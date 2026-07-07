package com.ragnarok.idle.domain;

import com.ragnarok.idle.math.BigNum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Справочник героев (seed из economy_constants.md). id — фиксированный номер
 * из дизайн-таблицы, не автогенерируется.
 */
@Entity
@Table(name = "hero")
public class Hero {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HeroType type;

    @Column(name = "base_dps", nullable = false)
    private BigNum baseDps;

    @Column(name = "skill_code", nullable = false, length = 50)
    private String skillCode;

    @Column(nullable = false)
    private BigNum price;

    /** Эйнхерий (id=11): усиливает бонус баферов ×3 (GDD §3.5). */
    @Column(name = "special_baffer", nullable = false)
    private boolean specialBaffer;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public HeroType getType() {
        return type;
    }

    public BigNum getBaseDps() {
        return baseDps;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public BigNum getPrice() {
        return price;
    }

    public boolean isSpecialBaffer() {
        return specialBaffer;
    }
}
