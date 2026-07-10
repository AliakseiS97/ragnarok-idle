package com.ragnarok.idle.controller;

import com.ragnarok.idle.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тест read-only эндпоинтов осады. Под JWT (SecurityConfig не менялся), поэтому берём реальный
 * токен через регистрацию — как это делает клиент.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SiegeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void getToday_returnsActiveBossWithDefendersAndHp() throws Exception {
        String token = authService.register("siege_reader_ok", "password123");

        // на любой неделе активен ровно один босс — эндпоинт всегда 200
        mockMvc.perform(get("/api/siege/today").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bossKey").isNotEmpty())
                .andExpect(jsonPath("$.defenders").isArray())
                .andExpect(jsonPath("$.currentHp.display").isNotEmpty());
    }

    @Test
    void getUnknownBoss_returnsNotFound() throws Exception {
        String token = authService.register("siege_reader_404", "password123");

        mockMvc.perform(get("/api/siege/bosses/does_not_exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
