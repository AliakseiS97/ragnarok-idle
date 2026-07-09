package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.AvatarUpgradeResponse;
import com.ragnarok.idle.dto.UpgradeAvatarRequest;
import com.ragnarok.idle.service.AvatarService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping("/tap-damage/upgrade")
    public AvatarUpgradeResponse upgradeTapDamage(@Valid @RequestBody UpgradeAvatarRequest request,
                                                   Authentication authentication) {
        return avatarService.upgradeTapDamage(authentication.getName(), request.mode());
    }

    @PostMapping("/autotap/upgrade")
    public AvatarUpgradeResponse upgradeAutotap(@Valid @RequestBody UpgradeAvatarRequest request,
                                                 Authentication authentication) {
        return avatarService.upgradeAutotap(authentication.getName(), request.mode());
    }
}
