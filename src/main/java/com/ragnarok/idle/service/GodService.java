package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.DeityHero;
import com.ragnarok.idle.domain.Rarity;
import com.ragnarok.idle.dto.DeityHeroDto;
import com.ragnarok.idle.mapper.DeityHeroMapper;
import com.ragnarok.idle.repository.DeityHeroRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Read-only доступ к справочнику героев-божеств (сид V10). */
@Service
public class GodService {

    private final DeityHeroRepository deityHeroRepository;
    private final DeityHeroMapper deityHeroMapper;

    public GodService(DeityHeroRepository deityHeroRepository, DeityHeroMapper deityHeroMapper) {
        this.deityHeroRepository = deityHeroRepository;
        this.deityHeroMapper = deityHeroMapper;
    }

    /** Все боги, либо только заданной редкости (если {@code rarity != null}). */
    public List<DeityHeroDto> findAll(Rarity rarity) {
        List<DeityHero> gods = rarity == null
                ? deityHeroRepository.findAll()
                : deityHeroRepository.findByRarity(rarity);
        return deityHeroMapper.toDto(gods);
    }

    /** Бог по бизнес-ключу; 404 если не найден. */
    public DeityHeroDto findByHeroKey(String heroKey) {
        return deityHeroRepository.findByHeroKey(heroKey)
                .map(deityHeroMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "God not found: " + heroKey));
    }
}
