package com.ragnarok.idle.battle;

import com.ragnarok.idle.battle.dto.TapResponse;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.math.BigNumDto;
import com.ragnarok.idle.player.Avatar;
import com.ragnarok.idle.player.AvatarRepository;
import com.ragnarok.idle.player.Player;
import com.ragnarok.idle.player.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BattleService {

    /** Босс сидит на 11-й позиции: 10 обычных мобов (GDD §3.1) + таймер-босс. */
    private static final int BOSS_SUB_LEVEL = EconomyCurves.MOBS_PER_LEVEL + 1;

    private static final BigNum BASE_TAP = BigNum.of(3);
    private static final double TAP_UPGRADE_STEP = 0.5;

    /** Защита от бесконечного цикла при аномальных данных — по факту never hit. */
    private static final int MAX_KILLS_PER_REQUEST = 100_000;

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;

    public BattleService(PlayerRepository playerRepository, AvatarRepository avatarRepository) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
    }

    @Transactional
    public TapResponse tap(String username, int taps) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));

        BigNum tapDamageOne = BASE_TAP.multiply(1 + avatar.getTapDamageLevel() * TAP_UPGRADE_STEP);
        BigNum damageDealt = tapDamageOne.multiply(taps);
        BigNum remainingDamage = damageDealt;

        int mobsKilled = 0;
        boolean bossDefeated = false;
        boolean leveledUp = false;
        BigNum goldGained = BigNum.ZERO;

        int killsThisRequest = 0;
        while (remainingDamage.gte(player.getCurrentMobHp())) {
            if (++killsThisRequest > MAX_KILLS_PER_REQUEST) break;

            remainingDamage = remainingDamage.subtract(player.getCurrentMobHp());
            boolean isBoss = player.getCurrentSubLevel() == BOSS_SUB_LEVEL;

            goldGained = goldGained.add(isBoss
                    ? EconomyCurves.bossGold(player.getCurrentLevel())
                    : EconomyCurves.goldPerMob(player.getCurrentLevel()));

            if (isBoss) {
                bossDefeated = true;
                leveledUp = true;
                long nextLevel = player.getCurrentLevel() + 1;
                player.setCurrentLevel(nextLevel);
                if (nextLevel > player.getMaxLevel()) {
                    player.setMaxLevel(nextLevel);
                }
                player.setCurrentSubLevel(1);
                player.setCurrentMobHp(EconomyCurves.mobHp(nextLevel));
            } else {
                mobsKilled++;
                int nextSubLevel = player.getCurrentSubLevel() + 1;
                player.setCurrentSubLevel(nextSubLevel);
                player.setCurrentMobHp(nextSubLevel == BOSS_SUB_LEVEL
                        ? EconomyCurves.bossHp(player.getCurrentLevel())
                        : EconomyCurves.mobHp(player.getCurrentLevel()));
            }
        }

        if (!remainingDamage.isZero()) {
            player.setCurrentMobHp(player.getCurrentMobHp().subtract(remainingDamage));
        }

        player.setGold(player.getGold().add(goldGained));
        playerRepository.save(player);

        return new TapResponse(
                BigNumDto.from(damageDealt),
                mobsKilled,
                bossDefeated,
                leveledUp,
                player.getCurrentLevel(),
                player.getCurrentSubLevel(),
                BigNumDto.from(player.getCurrentMobHp()),
                BigNumDto.from(goldGained),
                BigNumDto.from(player.getGold())
        );
    }
}
