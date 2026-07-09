package com.ragnarok.idle.dto;

import com.ragnarok.idle.economy.PurchaseMode;
import jakarta.validation.constraints.NotNull;

/** Улучшение ветки Аватара (тап/автотап) пачкой: клиент шлёт режим, цену пачки считает сервер. */
public record UpgradeAvatarRequest(@NotNull PurchaseMode mode) {
}
