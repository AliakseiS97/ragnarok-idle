package com.ragnarok.idle.controller;

import com.ragnarok.idle.domain.Futhark;
import com.ragnarok.idle.dto.RuneDto;
import com.ragnarok.idle.service.RuneService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API справочника рун. */
@RestController
@RequestMapping("/api/runes")
public class RuneController {

    private final RuneService runeService;

    public RuneController(RuneService runeService) {
        this.runeService = runeService;
    }

    @GetMapping
    public List<RuneDto> list(@RequestParam(name = "futhark", required = false) Futhark futhark,
                              @RequestParam(name = "tier", required = false) Integer tier) {
        return runeService.find(futhark, tier);
    }
}
