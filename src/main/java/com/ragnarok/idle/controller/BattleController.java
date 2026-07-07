package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.TapRequest;
import com.ragnarok.idle.dto.TapResponse;
import com.ragnarok.idle.service.BattleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/battle")
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @PostMapping("/tap")
    public TapResponse tap(@Valid @RequestBody TapRequest request, Authentication authentication) {
        return battleService.tap(authentication.getName(), request.taps());
    }
}
