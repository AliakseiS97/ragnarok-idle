package com.ragnarok.idle.battle;

import com.ragnarok.idle.battle.dto.TapRequest;
import com.ragnarok.idle.battle.dto.TapResponse;
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
