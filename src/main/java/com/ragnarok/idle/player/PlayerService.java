package com.ragnarok.idle.player;

import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.hero.Hero;
import com.ragnarok.idle.hero.HeroRepository;
import com.ragnarok.idle.hero.HeroService;
import com.ragnarok.idle.hero.PlayerHero;
import com.ragnarok.idle.hero.PlayerHeroRepository;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.math.BigNumDto;
import com.ragnarok.idle.player.dto.PlayerHeroView;
import com.ragnarok.idle.player.dto.PlayerStateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    /** Потолок офлайн-дохода (GDD §12.5). */
    private static final long OFFLINE_CAP_SECONDS = 12 * 3600;

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;
    private final HeroRepository heroRepository;
    private final PlayerHeroRepository playerHeroRepository;
    private final HeroService heroService;

    public PlayerService(PlayerRepository playerRepository, AvatarRepository avatarRepository,
                          HeroRepository heroRepository, PlayerHeroRepository playerHeroRepository,
                          HeroService heroService) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
        this.heroRepository = heroRepository;
        this.playerHeroRepository = playerHeroRepository;
        this.heroService = heroService;
    }

    @Transactional
    public PlayerStateResponse getState(String username) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));

        BigNum offlineGold = collectOfflineIncome(player);
        playerRepository.save(player);

        Avatar avatar = avatarRepository.findById(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Avatar missing"));

        Map<Long, PlayerHero> owned = playerHeroRepository.findByPlayerId(player.getId()).stream()
                .collect(Collectors.toMap(PlayerHero::getHeroId, Function.identity()));

        List<PlayerHeroView> heroViews = heroRepository.findAll().stream()
                .sorted(Comparator.comparing(Hero::getId))
                .map(hero -> {
                    PlayerHero playerHero = owned.get(hero.getId());
                    return new PlayerHeroView(
                            hero.getId(),
                            hero.getName(),
                            hero.getType().name(),
                            playerHero != null,
                            playerHero != null ? playerHero.getLevel() : 0L,
                            BigNumDto.from(hero.getPrice())
                    );
                })
                .toList();

        return new PlayerStateResponse(
                player.getCurrentLevel(),
                player.getMaxLevel(),
                player.getCurrentSubLevel(),
                BigNumDto.from(player.getCurrentMobHp()),
                BigNumDto.from(player.getGold()),
                BigNumDto.from(player.getAsh()),
                BigNumDto.from(offlineGold),
                avatar.getTapDamageLevel(),
                avatar.getAutotapLevel(),
                heroViews
        );
    }

    /** Начисляет накопленное офлайн-золото прямо в player.gold и возвращает начисленную сумму. */
    private BigNum collectOfflineIncome(Player player) {
        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = Duration.between(player.getLastCollectedAt(), now).getSeconds();
        player.setLastCollectedAt(now);

        if (elapsedSeconds <= 0) {
            return BigNum.ZERO;
        }

        BigNum totalPassiveDps = heroService.totalPassiveDps(player.getId());
        if (totalPassiveDps.isZero()) {
            return BigNum.ZERO;
        }

        long cappedSeconds = Math.min(elapsedSeconds, OFFLINE_CAP_SECONDS);
        long level = player.getCurrentLevel();

        // Пассивный DPS "добивает" мобов текущего уровня так же, как тапы:
        // золото/сек = DPS × (золото за моба / HP моба) на замороженном (офлайн не двигает уровни) уровне.
        BigNum goldPerSecond = totalPassiveDps
                .multiply(EconomyCurves.goldPerMob(level))
                .divide(EconomyCurves.mobHp(level));
        BigNum offlineGold = goldPerSecond.multiply(cappedSeconds);

        player.setGold(player.getGold().add(offlineGold));
        return offlineGold;
    }
}
