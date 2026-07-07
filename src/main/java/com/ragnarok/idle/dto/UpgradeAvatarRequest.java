package com.ragnarok.idle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpgradeAvatarRequest(@Min(1) @Max(10_000) long levels) {
}
