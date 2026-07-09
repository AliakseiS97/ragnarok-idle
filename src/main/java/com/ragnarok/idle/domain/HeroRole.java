package com.ragnarok.idle.domain;

/**
 * Боевая роль героя-божества (колонка {@code role}, хранится строкой).
 * Значения — DISTINCT из сида мифологии (V10): все 8 ролей.
 */
public enum HeroRole {
    DPS,
    TANK,
    BUFFER,
    SUPPORT,
    UTILITY,
    ECONOMY,
    SPEED,
    TRICKSTER
}
