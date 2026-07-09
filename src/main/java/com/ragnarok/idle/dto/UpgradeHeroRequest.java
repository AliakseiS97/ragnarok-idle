package com.ragnarok.idle.dto;

import com.ragnarok.idle.economy.PurchaseMode;
import jakarta.validation.constraints.NotNull;

/** Апгрейд героя пачкой: клиент шлёт режим (x1/x5/.../MAX), цену пачки считает сервер. */
public record UpgradeHeroRequest(@NotNull PurchaseMode mode) {
}
