package com.ragnarok.idle.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Боевая ветка Аватара (фарм). Дуэльные пассивы (HP/крит/броня/рег.) —
 * отдельная ветка из GDD §5.1, добавляется в Спринте 3.
 */
@Entity
@Table(name = "avatar")
@Getter
@Setter
public class Avatar {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    /** Оптимистическая блокировка — см. комментарий у {@link Player#getVersion()}. */
    @Version
    private Long version;

    @Column(name = "tap_damage_level", nullable = false)
    private Long tapDamageLevel;

    @Column(name = "autotap_level", nullable = false)
    private Long autotapLevel;
}
