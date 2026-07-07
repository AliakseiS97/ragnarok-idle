package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Avatar;
import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.AvatarRepository;
import com.ragnarok.idle.repository.PlayerRepository;
import com.ragnarok.idle.security.JwtService;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final AvatarRepository avatarRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PlayerRepository playerRepository, AvatarRepository avatarRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.avatarRepository = avatarRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(String username, String rawPassword) {
        if (playerRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        Player player = new Player();
        player.setUsername(username);
        player.setPasswordHash(passwordEncoder.encode(rawPassword));
        player.setCurrentLevel(1L);
        player.setMaxLevel(1L);
        player.setGold(BigNum.ZERO);
        player.setAsh(BigNum.ZERO);
        player.setLastCollectedAt(LocalDateTime.now());
        player.setCurrentSubLevel(1);
        // HP первого моба = baseHP из HP-кривой (GDD §3.2); полная формула — в BattleService (Часть 3).
        player.setCurrentMobHp(BigNum.of(10));
        playerRepository.save(player);

        Avatar avatar = new Avatar();
        avatar.setPlayerId(player.getId());
        // урон/тап = уровень тапа (GDD §3.4): старт с ур.1 -> урон 1
        avatar.setTapDamageLevel(1L);
        avatar.setAutotapLevel(0L);
        avatarRepository.save(avatar);

        return jwtService.generateToken(username);
    }

    public String login(String username, String rawPassword) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, player.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return jwtService.generateToken(username);
    }
}
