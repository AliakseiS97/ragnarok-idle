package com.ragnarok.idle.rebirth;

import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.hero.PlayerHero;
import com.ragnarok.idle.hero.PlayerHeroRepository;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.math.BigNumDto;
import com.ragnarok.idle.player.Player;
import com.ragnarok.idle.player.PlayerRepository;
import com.ragnarok.idle.rebirth.dto.RebirthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RebirthService {

    /** Ребёрт доступен с этого maxLevel (GDD §3.9). Артефакт +старт-уровня — Спринт 2+. */
    private static final long MIN_REBIRTH_LEVEL = 100;

    /** ashGained = floor(baseAsh × mobHP(maxLevel)^ashExp), baseAsh=1 (GDD §3.9 — точных чисел в economy_constants.md нет). */
    private static final double ASH_EXPONENT = 0.0009;

    private final PlayerRepository playerRepository;
    private final PlayerHeroRepository playerHeroRepository;

    public RebirthService(PlayerRepository playerRepository, PlayerHeroRepository playerHeroRepository) {
        this.playerRepository = playerRepository;
        this.playerHeroRepository = playerHeroRepository;
    }

    @Transactional
    public RebirthResponse rebirth(String username) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));

        if (player.getMaxLevel() < MIN_REBIRTH_LEVEL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rebirth requires reaching level " + MIN_REBIRTH_LEVEL);
        }

        BigNum ashGained = EconomyCurves.mobHp(player.getMaxLevel()).pow(ASH_EXPONENT).floor();

        player.setAsh(player.getAsh().add(ashGained));
        player.setGold(BigNum.ZERO);
        player.setCurrentLevel(1L);
        player.setCurrentSubLevel(1);
        player.setCurrentMobHp(EconomyCurves.mobHp(1));
        // maxLevel НЕ сбрасываем — исторический рекорд (нужен артефакту +старт-уровня, Спринт 2+).
        playerRepository.save(player);

        List<PlayerHero> ownedHeroes = playerHeroRepository.findByPlayerId(player.getId());
        ownedHeroes.forEach(playerHero -> playerHero.setLevel(1L));
        playerHeroRepository.saveAll(ownedHeroes);

        return new RebirthResponse(BigNumDto.from(ashGained), BigNumDto.from(player.getAsh()));
    }
}
