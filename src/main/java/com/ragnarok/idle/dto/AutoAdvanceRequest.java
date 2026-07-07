package com.ragnarok.idle.dto;

import jakarta.validation.constraints.NotNull;

/** Флаг автоперехода: true — обычная прогрессия, false — фарм-цикл мобов текущего уровня. */
public record AutoAdvanceRequest(@NotNull Boolean enabled) {
}
