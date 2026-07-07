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

    /** С этого уровня тапа открыто умение «+10% к урону за клик» (10-е улучшение, GDD §3.4). */
    public static final long CLICK_SKILL_LEVEL = 10;
    private static final double CLICK_SKILL_MULT = 1.1;

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;
    private final CombatEngine combatEngine;

    public BattleService(PlayerRepository playerRepository, AvatarRepository avatarRepository,
                          CombatEngine combatEngine) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
        this.combatEngine = combatEngine;
    }

    /**
     * Урон одного тапа Аватара = уровень тапа (ур.1 → 1, ур.9 → 9, GDD §3.4);
     * с ур.10 действует умение ×1.1 (целые числа: floor, ур.10 → 11).
     */
    public static BigNum tapDamage(Avatar avatar) {
        long level = Math.max(1, avatar.getTapDamageLevel());
        BigNum damage = BigNum.of(level);
        if (level >= CLICK_SKILL_LEVEL) {
            damage = damage.multiply(CLICK_SKILL_MULT).floor();
        }
        return damage;
    }

    @Transactional
    public TapResponse tap(String username, int taps) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));

        BigNum damagePerTap = tapDamage(avatar);
        CombatEngine.DamageResult result = combatEngine.applyHits(player, damagePerTap, taps);
        playerRepository.save(player);

        return new TapResponse(
                BigNumDto.from(damagePerTap.multiply(taps)),
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
