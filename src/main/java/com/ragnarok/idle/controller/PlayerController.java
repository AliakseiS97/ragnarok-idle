package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.economy.PurchaseMode;
import com.ragnarok.idle.service.PlayerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    /**
     * @param mode выбранный на клиенте режим мультипокупки — сервер под него считает цену пачки
     *             для кнопок апгрейда (герои + тап Аватара). По умолчанию x1.
     */
    @GetMapping("/me")
    public PlayerStateResponse me(@RequestParam(name = "mode", defaultValue = "X1") PurchaseMode mode,
                                  Authentication authentication) {
        return playerService.getState(authentication.getName(), mode);
    }
}
