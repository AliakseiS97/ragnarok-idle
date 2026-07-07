package com.ragnarok.idle.player;

import com.ragnarok.idle.player.dto.PlayerStateResponse;
import com.ragnarok.idle.player.dto.SkipTimeRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ТЕСТОВЫЕ ручки для плейтеста баланса — не игровой API.
 * Прокрутка времени позже переедет в игровую механику (Яблоки Идунн, GDD Яблоки-траты).
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final PlayerService playerService;

    public DebugController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/skip-time")
    public PlayerStateResponse skipTime(@Valid @RequestBody SkipTimeRequest request,
                                         Authentication authentication) {
        return playerService.skipTime(authentication.getName(), request.hours());
    }
}
