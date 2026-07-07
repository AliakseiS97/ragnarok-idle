package com.ragnarok.idle.battle;

import com.ragnarok.idle.battle.dto.TapResponse;
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

    /** baseTap=1 (economy_constants.md: "Базовый урон/тап"). */
    private static final BigNum BASE_TAP = BigNum.ONE;
    /** Целый шаг (был 0.5): урон/тап = 1, 2, 3, ... — дробных чисел в игре нет. */
    private static final double TAP_UPGRADE_STEP = 1;

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;
    private final CombatEngine combatEngine;

    public BattleService(PlayerRepository playerRepository, AvatarRepository avatarRepository,
                          CombatEngine combatEngine) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
        this.combatEngine = combatEngine;
    }

    /** Урон одного тапа Аватара (GDD §3.4; криты — позже). */
    public static BigNum tapDamage(Avatar avatar) {
        return BASE_TAP.multiply(1 + avatar.getTapDamageLevel() * TAP_UPGRADE_STEP);
    }

    @Transactional
    public TapResponse tap(String username, int taps) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));

        BigNum damageDealt = tapDamage(avatar).multiply(taps);
        CombatEngine.DamageResult result = combatEngine.applyDamage(player, damageDealt);
        playerRepository.save(player);

        return new TapResponse(
                BigNumDto.from(damageDealt),
                result.mobsKilled(),
                result.bossDefeated(),
                result.leveledUp(),
                player.getCurrentLevel(),
                player.getCurrentSubLevel(),
                BigNumDto.from(player.getCurrentMobHp()),
                BigNumDto.from(result.goldGained()),
                BigNumDto.from(player.getGold())
        );
    }
}
