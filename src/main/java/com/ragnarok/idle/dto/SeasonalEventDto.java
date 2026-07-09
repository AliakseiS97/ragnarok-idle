package com.ragnarok.idle.dto;

import com.ragnarok.idle.domain.EventMonth;

/** Публичное представление сезонного ивента. */
public record SeasonalEventDto(
        Long id,
        String eventKey,
        String nameRu,
        String nameOriginal,
        EventMonth eventMonth,
        int durationDays,
        String gameMechanics
) {
}
