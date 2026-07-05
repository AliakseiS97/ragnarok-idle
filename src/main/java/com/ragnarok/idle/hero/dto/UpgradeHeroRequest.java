package com.ragnarok.idle.hero.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpgradeHeroRequest(@Min(1) @Max(10_000) long levels) {
}
