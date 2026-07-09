package com.ragnarok.idle.dto;

import com.ragnarok.idle.domain.Rarity;

/**
 * Публичное представление артефакта. Владелец отдаётся плоско — {@code ownerHeroKey}
 * и {@code ownerNameRu} (может быть null у безхозного артефакта). {@code artPrompt} не включён.
 */
public record ArtifactDto(
        Long id,
        String artifactKey,
        String nameRu,
        String nameOriginal,
        Rarity rarity,
        String ownerHeroKey,
        String ownerNameRu,
        String effect,
        String setBonus
) {
}
