package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Hero;
import com.ragnarok.idle.domain.HeroType;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.dto.BigNumDto;
import com.ragnarok.idle.dto.PlayerHeroResponse;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.HeroRepository;
import com.ragnarok.idle.repository.PlayerHeroRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HeroService {

    /** +10%/уровень (economy_constants.md: "Множ. DPS за ур. героя"; было 1.08 — поднято плейтестом). */
    private static final double DPS_GROWTH = 1.10;
    /** Веха бафера срабатывает ОДНОКРАТНО на этом уровне героя (GDD §3.7/правка ТЗ), а не за каждый уровень. */
    private static final long BAFFER_MILESTONE_LEVEL = 25;
    /** +25% к общему DPS команды разово на вехе; дальше растёт только личный DPS бафера. */
    private static final double BAFFER_MILESTONE_BONUS = 0.25;
    private static final double SPECIAL_BAFFER_MULT = 3.0;
    /** Цена апгрейда героя растёт на 7%/уровень; базой берём цену найма героя (в источнике отдельно не указана). */
    private static final double UPGRADE_COST_GROWTH = 1.07;

    private final PlayerRepository playerRepository;
    private final HeroRepository heroRepository;
    private final PlayerHeroRepository playerHeroRepository;

    public HeroService(PlayerRepository playerRepository, HeroRepository heroRepository,
                        PlayerHeroRepository playerHeroRepository) {
        this.playerRepository = playerRepository;
        this.heroRepository = heroRepository;
        this.playerHeroRepository = playerHeroRepository;
    }

    /** Суммарный пассивный DPS игрока с агрегацией баферов (GDD §3.5). Милевехи/редкость/артефакты — след. спринты. */
    public BigNum totalPassiveDps(Long playerId) {
        return passiveDpsByHero(playerId).values().stream().reduce(BigNum.ZERO, BigNum::add);
    }

    /** Вклад каждого героя в totalPassiveDps (после бафер-множителя) — для показа в UI (GDD §3.5). */
    public Map<Long, BigNum> passiveDpsByHero(Long playerId) {
        List<PlayerHero> owned = playerHeroRepository.findByPlayerId(playerId);
        if (owned.isEmpty()) {
            return Map.of();
        }

        Map<Long, Hero> heroesById = heroRepository.findAllById(
                owned.stream().map(PlayerHero::getHeroId).toList()
        ).stream().collect(Collectors.toMap(Hero::getId, Function.identity()));

        Map<Long, BigNum> rawDpsByHero = new HashMap<>();
        double bafferBonus = 0;
        boolean specialBafferActive = false;

        for (PlayerHero ph : owned) {
            Hero hero = heroesById.get(ph.getHeroId());
            BigNum heroDps = hero.getBaseDps().multiply(BigNum.of(DPS_GROWTH).pow(ph.getLevel() - 1));
            rawDpsByHero.put(ph.getHeroId(), heroDps);

            if (hero.getType() == HeroType.BAFFER) {
                if (ph.getLevel() >= BAFFER_MILESTONE_LEVEL) {
                    bafferBonus += BAFFER_MILESTONE_BONUS;
                }
                if (hero.isSpecialBaffer()) {
                    specialBafferActive = true;
                }
            }
        }

        if (specialBafferActive) {
            bafferBonus *= SPECIAL_BAFFER_MULT;
        }
        double multiplier = 1 + bafferBonus;
        // DPS каждого героя — целое; округление до БЛИЖАЙШЕГО, чтобы +10%/ур. был виден
        // уже на первом апгрейде дешёвых героев (floor съедал 5×1.1=5.5 обратно в 5)
        rawDpsByHero.replaceAll((heroId, dps) -> dps.multiply(multiplier).add(BigNum.of(0.5)).floor());
        return rawDpsByHero;
    }

    @Transactional
    public PlayerHeroResponse buy(String username, Long heroId) {
        Player player = findPlayer(username);
        Hero hero = findHero(heroId);

        if (playerHeroRepository.findByPlayerIdAndHeroId(player.getId(), heroId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hero already owned");
        }
        if (player.getGold().lt(hero.getPrice())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough gold");
        }

        player.setGold(player.getGold().subtract(hero.getPrice()));
        playerRepository.save(player);

        PlayerHero playerHero = new PlayerHero();
        playerHero.setPlayerId(player.getId());
        playerHero.setHeroId(heroId);
        playerHero.setLevel(1L);
        playerHero.setActivated(true);
        playerHeroRepository.save(playerHero);

        return new PlayerHeroResponse(heroId, hero.getName(), 1L,
                BigNumDto.from(hero.getPrice()), BigNumDto.from(player.getGold()));
    }

    /** Цена апгрейда героя с уровня currentLevel на следующий: ceil(цена найма × 1.07^(currentLevel-1)) — целая. */
    public static BigNum upgradeCostFrom(Hero hero, long currentLevel) {
        return hero.getPrice().multiply(BigNum.of(UPGRADE_COST_GROWTH).pow(currentLevel - 1)).ceil();
    }

    @Transactional
    public PlayerHeroResponse upgrade(String username, Long heroId, long levels) {
        Player player = findPlayer(username);
        Hero hero = findHero(heroId);
        PlayerHero playerHero = playerHeroRepository.findByPlayerIdAndHeroId(player.getId(), heroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Hero not bought yet"));

        BigNum totalCost = BigNum.ZERO;
        for (long i = 0; i < levels; i++) {
            totalCost = totalCost.add(upgradeCostFrom(hero, playerHero.getLevel() + i));
        }

        if (player.getGold().lt(totalCost)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough gold");
        }

        player.setGold(player.getGold().subtract(totalCost));
        playerHero.setLevel(playerHero.getLevel() + levels);
        playerRepository.save(player);
        playerHeroRepository.save(playerHero);

        return new PlayerHeroResponse(heroId, hero.getName(), playerHero.getLevel(),
                BigNumDto.from(totalCost), BigNumDto.from(player.getGold()));
    }

    private Player findPlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
    }

    private Hero findHero(Long heroId) {
        return heroRepository.findById(heroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hero not found"));
    }
}
