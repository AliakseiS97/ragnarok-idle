package com.ragnarok.idle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Тестовая прокрутка времени (часы); фактическое начисление ограничено потолком офлайна 12ч (GDD §12.5). */
public record SkipTimeRequest(@Min(1) @Max(100) int hours) {
}
