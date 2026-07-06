package com.ragnarok.idle.player;

import com.ragnarok.idle.player.dto.AvatarUpgradeResponse;
import com.ragnarok.idle.player.dto.UpgradeAvatarRequest;
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
        return avatarService.upgradeTapDamage(authentication.getName(), request.levels());
    }

    @PostMapping("/autotap/upgrade")
    public AvatarUpgradeResponse upgradeAutotap(@Valid @RequestBody UpgradeAvatarRequest request,
                                                 Authentication authentication) {
        return avatarService.upgradeAutotap(authentication.getName(), request.levels());
    }
}
