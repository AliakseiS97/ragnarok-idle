package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.AutoAdvanceRequest;
import com.ragnarok.idle.dto.PlayerStateResponse;
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

    /** «К боссу»: на 10-ю подлокацию босс-уровня, таймер 30с пошёл. */
    @PostMapping("/boss")
    public PlayerStateResponse goToBoss(Authentication authentication) {
        return battleService.goToBoss(authentication.getName());
    }

    /** Уровень назад — фармить мобов «перед боссом». */
    @PostMapping("/level/back")
    public PlayerStateResponse levelBack(Authentication authentication) {
        return battleService.changeLevel(authentication.getName(), -1);
    }

    /** Уровень вперёд (не выше достигнутого maxLevel). */
    @PostMapping("/level/forward")
    public PlayerStateResponse levelForward(Authentication authentication) {
        return battleService.changeLevel(authentication.getName(), 1);
    }

    /** Флаг автоперехода: false — фарм-цикл мобов текущего уровня. */
    @PostMapping("/auto-advance")
    public PlayerStateResponse setAutoAdvance(@Valid @RequestBody AutoAdvanceRequest request,
                                               Authentication authentication) {
        return battleService.setAutoAdvance(authentication.getName(), request.enabled());
    }
}
