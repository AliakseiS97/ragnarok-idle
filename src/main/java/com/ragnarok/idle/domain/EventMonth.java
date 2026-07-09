package com.ragnarok.idle.domain;

/**
 * Месяц проведения сезонного ивента (колонка {@code event_month}, хранится строкой).
 * Сид (V10) использует APRIL, OCTOBER, JANUARY, NOVEMBER; перечисление покрывает
 * все 12 месяцев в календарном порядке — на случай будущих ивентов.
 */
public enum EventMonth {
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER
}
