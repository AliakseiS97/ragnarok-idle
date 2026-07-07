package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.service.PlayerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/me")
    public PlayerStateResponse me(Authentication authentication) {
        return playerService.getState(authentication.getName());
    }
}
