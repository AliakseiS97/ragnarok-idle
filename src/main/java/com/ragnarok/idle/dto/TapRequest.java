package com.ragnarok.idle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Верхняя граница 200 — грубая защита от одного запроса с абсурдным числом тапов.
 * Полноценная валидация темпа (тапы/сек, GDD §12.7) — отдельная задача позже.
 */
public record TapRequest(@Min(1) @Max(200) int taps) {
}
