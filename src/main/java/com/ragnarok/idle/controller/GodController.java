package com.ragnarok.idle.controller;

import com.ragnarok.idle.domain.Rarity;
import com.ragnarok.idle.dto.DeityHeroDto;
import com.ragnarok.idle.service.GodService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API справочника героев-божеств. */
@RestController
@RequestMapping("/api/gods")
public class GodController {

    private final GodService godService;

    public GodController(GodService godService) {
        this.godService = godService;
    }

    @GetMapping
    public List<DeityHeroDto> list(@RequestParam(name = "rarity", required = false) Rarity rarity) {
        return godService.findAll(rarity);
    }

    @GetMapping("/{heroKey}")
    public DeityHeroDto get(@PathVariable("heroKey") String heroKey) {
        return godService.findByHeroKey(heroKey);
    }
}
