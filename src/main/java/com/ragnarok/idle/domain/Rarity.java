package com.ragnarok.idle.domain;

/**
 * Редкость героев-божеств и артефактов (колонка {@code rarity}, хранится строкой).
 * Значения соответствуют DISTINCT из сида мифологии (V10): RARE, EPIC, LEGENDARY —
 * перечислены по возрастанию редкости.
 */
public enum Rarity {
    RARE,
    EPIC,
    LEGENDARY
}
