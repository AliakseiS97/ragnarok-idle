package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.PlayerHeroResponse;
import com.ragnarok.idle.dto.UpgradeHeroRequest;
import com.ragnarok.idle.service.HeroService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/heroes")
public class HeroController {

    private final HeroService heroService;

    public HeroController(HeroService heroService) {
        this.heroService = heroService;
    }

    @PostMapping("/{id}/buy")
    public PlayerHeroResponse buy(@PathVariable("id") Long id, Authentication authentication) {
        return heroService.buy(authentication.getName(), id);
    }

    @PostMapping("/{id}/upgrade")
    public PlayerHeroResponse upgrade(@PathVariable("id") Long id, @Valid @RequestBody UpgradeHeroRequest request,
                                       Authentication authentication) {
        return heroService.upgrade(authentication.getName(), id, request.levels());
    }
}
