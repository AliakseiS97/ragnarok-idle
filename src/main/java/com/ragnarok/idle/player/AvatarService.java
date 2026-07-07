package com.ragnarok.idle.player;

import com.ragnarok.idle.battle.BattleService;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.math.BigNumDto;
import com.ragnarok.idle.player.dto.AvatarUpgradeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Прокачка боевой ветки Аватара (урон/тап, автотап) за золото — цена как у героев (GDD §3.4/§13, Спринт 1). */
@Service
public class AvatarService {

    /** Цена 1-го апгрейда тапа (ур.1 → 2) = 5 (economy_constants.md). */
    private static final BigNum BASE_UPGRADE_COST = BigNum.of(5);
    /** Рост цены апгрейда Аватара ×1.15 — стандарт жанра; свой множитель в economy_constants.md не задан. */
    private static final double UPGRADE_COST_GROWTH = 1.15;
    /** 10-е улучшение тапа (ур.9 → 10) открывает умение +10% к клику и стоит фикс 109 (economy_constants.md). */
    private static final BigNum SKILL_UPGRADE_COST = BigNum.of(109);

    /**
     * Цена апгрейда ветки Аватара с уровня currentLevel на следующий (целая, ceil):
     * до умения — 5 × 1.15^(ур-1); ур.9 → 10 — фикс 109; дальше — 109 × 1.15^(ур-9), монотонно.
     */
    public static BigNum upgradeCostFrom(long currentLevel) {
        long level = Math.max(currentLevel, 1);
        if (level + 1 == BattleService.CLICK_SKILL_LEVEL) {
            return SKILL_UPGRADE_COST;
        }
        if (level >= BattleService.CLICK_SKILL_LEVEL) {
            return SKILL_UPGRADE_COST
                    .multiply(BigNum.of(UPGRADE_COST_GROWTH).pow(level - BattleService.CLICK_SKILL_LEVEL + 1))
                    .ceil();
        }
        return BASE_UPGRADE_COST.multiply(BigNum.of(UPGRADE_COST_GROWTH).pow(level - 1)).ceil();
    }

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;

    public AvatarService(PlayerRepository playerRepository, AvatarRepository avatarRepository) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
    }

    @Transactional
    public AvatarUpgradeResponse upgradeTapDamage(String username, long levels) {
        Player player = findPlayer(username);
        Avatar avatar = findAvatar(player.getId());

        BigNum cost = spendGoldForLevels(player, avatar.getTapDamageLevel(), levels);
        avatar.setTapDamageLevel(avatar.getTapDamageLevel() + levels);

        playerRepository.save(player);
        avatarRepository.save(avatar);

        return toResponse(avatar, cost, player.getGold());
    }

    @Transactional
    public AvatarUpgradeResponse upgradeAutotap(String username, long levels) {
        Player player = findPlayer(username);
        Avatar avatar = findAvatar(player.getId());

        BigNum cost = spendGoldForLevels(player, avatar.getAutotapLevel(), levels);
        avatar.setAutotapLevel(avatar.getAutotapLevel() + levels);

        playerRepository.save(player);
        avatarRepository.save(avatar);

        return toResponse(avatar, cost, player.getGold());
    }

    private BigNum spendGoldForLevels(Player player, long currentLevel, long levels) {
        BigNum totalCost = BigNum.ZERO;
        for (long i = 0; i < levels; i++) {
            totalCost = totalCost.add(upgradeCostFrom(currentLevel + i));
        }

        if (player.getGold().lt(totalCost)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough gold");
        }

        player.setGold(player.getGold().subtract(totalCost));
        return totalCost;
    }

    private AvatarUpgradeResponse toResponse(Avatar avatar, BigNum goldSpent, BigNum goldRemaining) {
        return new AvatarUpgradeResponse(
                avatar.getTapDamageLevel(),
                avatar.getAutotapLevel(),
                BigNumDto.from(goldSpent),
                BigNumDto.from(goldRemaining)
        );
    }

    private Player findPlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
    }

    private Avatar findAvatar(Long playerId) {
        return avatarRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));
    }
}
