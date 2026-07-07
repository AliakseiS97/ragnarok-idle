package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.RebirthResponse;
import com.ragnarok.idle.service.RebirthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rebirth")
public class RebirthController {

    private final RebirthService rebirthService;

    public RebirthController(RebirthService rebirthService) {
        this.rebirthService = rebirthService;
    }

    @PostMapping
    public RebirthResponse rebirth(Authentication authentication) {
        return rebirthService.rebirth(authentication.getName());
    }
}
