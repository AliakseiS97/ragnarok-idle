package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Hero;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.domain.PlayerHero;
import com.ragnarok.idle.domain.Saga;
import com.ragnarok.idle.domain.SagaRank;
import com.ragnarok.idle.dto.SagaPromoteResponse;
import com.ragnarok.idle.repository.HeroRepository;
import com.ragnarok.idle.repository.PlayerHeroRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Повышение Саги героя (GDD §3.8).
 *
 * <p><b>ВНИМАНИЕ — Этап 1, ЗАГЛУШКА.</b> Сейчас повышение бесплатно и мгновенно: один вызов =
 * +1 под-ступень (на X → новый ранг, ступень→I). Единственное условие — герой куплен.
 * Сделано для проверки механики стены/множителя.
 *
 * <p><b>TODO Этап 2 — «Клятва на крови»:</b> реальный механизм повышения Саги — это ритуал с
 * жертвой других героев (расход), кулдауном 24ч и растущей ценой. Он заменит эту заглушку;
 * сигнатуру/эндпоинт менять не придётся — добавятся проверки стоимости/кулдауна/жертв.
 */
@Service
public class SagaService {

    private final PlayerRepository playerRepository;
    private final HeroRepository heroRepository;
    private final PlayerHeroRepository playerHeroRepository;

    public SagaService(PlayerRepository playerRepository, HeroRepository heroRepository,
                        PlayerHeroRepository playerHeroRepository) {
        this.playerRepository = playerRepository;
        this.heroRepository = heroRepository;
        this.playerHeroRepository = playerHeroRepository;
    }

    @Transactional
    public SagaPromoteResponse promote(String username, Long heroId) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Player not found"));
        Hero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hero not found"));

        // Условие (заглушка): герой должен быть КУПЛЕН. Стоимость/кулдаун/жертвы — Этап 2.
        PlayerHero playerHero = playerHeroRepository.findByPlayerIdAndHeroId(player.getId(), heroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Hero not bought yet"));

        if (Saga.isMaxed(playerHero.getSagaRank(), playerHero.getSagaSubStep())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Максимальная Сага уже достигнута");
        }

        if (playerHero.getSagaSubStep() < Saga.SUB_STEPS_PER_RANK) {
            // ещё есть под-ступени внутри ранга — просто +1 (I→II→...→X)
            playerHero.setSagaSubStep(playerHero.getSagaSubStep() + 1);
        } else {
            // на X-й ступени — переход на новый ранг, ступень сбрасывается в I (+500% разово, см. Saga)
            playerHero.setSagaRank(SagaRank.byRank(playerHero.getSagaRank()).next().rank());
            playerHero.setSagaSubStep(Saga.FIRST_SUB_STEP);
        }
        playerHeroRepository.save(playerHero);

        SagaRank rank = SagaRank.byRank(playerHero.getSagaRank());
        return new SagaPromoteResponse(
                heroId,
                hero.getName(),
                playerHero.getSagaRank(),
                rank.getDisplayName(),
                playerHero.getSagaSubStep(),
                Saga.toRoman(playerHero.getSagaSubStep()),
                rank.getLevelCap()
        );
    }
}
