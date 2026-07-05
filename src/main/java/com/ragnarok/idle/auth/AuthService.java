package com.ragnarok.idle.auth;

import com.ragnarok.idle.player.Player;
import com.ragnarok.idle.player.PlayerRepository;
import com.ragnarok.idle.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.playerRepository = playerRepository;
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
        playerRepository.save(player);

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
