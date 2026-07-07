package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.RebirthResponse;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.PlayerHeroRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RebirthService {

    /** Ребёрт доступен с этого maxLevel (GDD §3.9). Артефакт +старт-уровня — Спринт 2+. */
    private static final long MIN_REBIRTH_LEVEL = 100;

    /** ashGained = floor(ashBoost × mobHP(maxLevel)^ashExp) (GDD §3.9). */
    private static final double ASH_EXPONENT = 0.0009;

    /** Буст выхода Пепла ×2.5 (балансировка плейтеста: базовая формула давала слишком мало). */
    private static final double ASH_BOOST = 2.5;

    private final PlayerRepository playerRepository;
    private final PlayerHeroRepository playerHeroRepository;
    private final AvatarRepository avatarRepository;

    public RebirthService(PlayerRepository playerRepository, PlayerHeroRepository playerHeroRepository,
                           AvatarRepository avatarRepository) {
        this.playerRepository = playerRepository;
        this.playerHeroRepository = playerHeroRepository;
        this.avatarRepository = avatarRepository;
    }

    /**
     * Полный сброс забега в обмен на Пепел: золото → 0, уровень → 1, герои теряются
     * совсем (в UI — «ур. 0», нанимать заново), Аватар — на стартовый ур.1 тапа.
     * Сохраняются только Пепел и maxLevel (исторический рекорд).
     */
    @Transactional
    public RebirthResponse rebirth(String username) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));

        if (player.getMaxLevel() < MIN_REBIRTH_LEVEL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rebirth requires reaching level " + MIN_REBIRTH_LEVEL);
        }

        BigNum ashGained = EconomyCurves.mobHp(player.getMaxLevel())
                .pow(ASH_EXPONENT)
                .multiply(ASH_BOOST)
                .floor();

        player.setAsh(player.getAsh().add(ashGained));
        player.setGold(BigNum.ZERO);
        player.setCurrentLevel(1L);
        player.setCurrentSubLevel(1);
        player.setCurrentMobHp(EconomyCurves.mobHp(1));
        player.setBossStartedAt(null);
        player.setAutoAdvance(true);
        // maxLevel НЕ сбрасываем — исторический рекорд (нужен артефакту +старт-уровня, Спринт 2+).
        playerRepository.save(player);

        // герои теряются полностью — после ребёрта все снова «ур. 0» (не куплены)
        playerHeroRepository.deleteAll(playerHeroRepository.findByPlayerId(player.getId()));

        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));
        avatar.setTapDamageLevel(1L);
        avatar.setAutotapLevel(0L);
        avatarRepository.save(avatar);

        return new RebirthResponse(BigNumDto.from(ashGained), BigNumDto.from(player.getAsh()));
    }
}
