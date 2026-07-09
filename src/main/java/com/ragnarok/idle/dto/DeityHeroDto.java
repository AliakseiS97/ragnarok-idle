package com.ragnarok.idle.dto;

import com.ragnarok.idle.domain.HeroRole;
import com.ragnarok.idle.domain.Rarity;

/**
 * Публичное представление героя-божества. Внутренние поля {@code artPrompt} и
 * {@code loreSource} намеренно не включены.
 */
public record DeityHeroDto(
        Long id,
        String heroKey,
        String nameRu,
        String nameOriginal,
        String pantheon,
        Rarity rarity,
        HeroRole role,
        String runeAnchor,
        String skillName,
        String skillDescription
) {
}
