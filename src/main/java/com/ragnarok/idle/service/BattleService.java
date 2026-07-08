package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.dto.TapResponse;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class BattleService {

    /** С этого уровня тапа открыто умение «+10% к урону за клик» (10-е улучшение, GDD §3.4). */
    public static final long CLICK_SKILL_LEVEL = 10;
    private static final double CLICK_SKILL_MULT = 1.1;

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;
    private final CombatEngine combatEngine;
    private final PlayerService playerService;

    public BattleService(PlayerRepository playerRepository, AvatarRepository avatarRepository,
                          CombatEngine combatEngine, PlayerService playerService) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
        this.combatEngine = combatEngine;
        this.playerService = playerService;
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
        Player player = findPlayer(username);
        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));

        combatEngine.syncBossTimer(player, LocalDateTime.now());
        BigNum damagePerTap = tapDamage(avatar);
        CombatEngine.DamageResult result = combatEngine.applyHits(player, damagePerTap, taps, false);
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
                BigNumDto.from(player.getGold()),
                BigNumDto.from(result.ashGained())
        );
    }

    /**
     * «К боссу»: мгновенный прыжок на застрявший незакрытый боссовый уровень (maxLevel). Пройти
     * боссовый уровень можно только убив босса — значит, если maxLevel кратен 5, игрок именно там
     * застрял (не хватило DPS/тапов), возможно уйдя фармить золото назад по обычным уровням.
     */
    @Transactional
    public PlayerStateResponse goToBoss(String username) {
        Player player = findPlayer(username);
        long bossLevel = player.getMaxLevel();
        if (!CombatEngine.isBossLevel(bossLevel)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No pending boss to return to");
        }
        if (player.getCurrentLevel() == bossLevel) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already fighting the boss");
        }
        combatEngine.enterLevel(player, bossLevel);
        playerRepository.save(player);
        return playerService.getState(username);
    }

    /** Переключение уровня для фарма: delta = -1 (назад) или +1 (вперёд, не выше maxLevel). */
    @Transactional
    public PlayerStateResponse changeLevel(String username, int delta) {
        Player player = findPlayer(username);
        long targetLevel = player.getCurrentLevel() + delta;
        if (targetLevel < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already at the first level");
        }
        if (targetLevel > player.getMaxLevel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Level not unlocked yet");
        }
        combatEngine.enterLevel(player, targetLevel);
        playerRepository.save(player);
        return playerService.getState(username);
    }

    /** Флаг автоперехода: false — фарм-цикл мобов текущего уровня (босс не атакуется). */
    @Transactional
    public PlayerStateResponse setAutoAdvance(String username, boolean enabled) {
        Player player = findPlayer(username);
        player.setAutoAdvance(enabled);
        playerRepository.save(player);
        return playerService.getState(username);
    }

    private Player findPlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
    }
}
