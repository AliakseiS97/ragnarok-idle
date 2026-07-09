package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Futhark;
import com.ragnarok.idle.domain.Rune;
import com.ragnarok.idle.dto.RuneDto;
import com.ragnarok.idle.mapper.RuneMapper;
import com.ragnarok.idle.repository.RuneRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Read-only доступ к справочнику рун (сид V10). */
@Service
public class RuneService {

    private final RuneRepository runeRepository;
    private final RuneMapper runeMapper;

    public RuneService(RuneRepository runeRepository, RuneMapper runeMapper) {
        this.runeRepository = runeRepository;
        this.runeMapper = runeMapper;
    }

    /**
     * Руны с опциональной фильтрацией по футарку и/или тиру. Используем только заявленные
     * методы репозитория ({@code findByFuthark}, {@code findByTier}); если заданы оба
     * параметра — пересечение считаем в памяти (набор рун маленький, 40 строк).
     */
    public List<RuneDto> find(Futhark futhark, Integer tier) {
        List<Rune> runes;
        if (futhark != null && tier != null) {
            runes = runeRepository.findByFuthark(futhark).stream()
                    .filter(rune -> rune.getTier() == tier)
                    .toList();
        } else if (futhark != null) {
            runes = runeRepository.findByFuthark(futhark);
        } else if (tier != null) {
            runes = runeRepository.findByTier(tier);
        } else {
            runes = runeRepository.findAll();
        }
        return runeMapper.toDto(runes);
    }
}
