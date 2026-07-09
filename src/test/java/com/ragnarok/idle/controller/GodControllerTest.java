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
 * Тест read-only эндпоинта GET /api/gods/{heroKey}. Эндпоинт под JWT (SecurityConfig не менялся),
 * поэтому берём реальный токен через регистрацию — как это делает клиент.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void getExistingGod_returnsOkAndHidesInternalFields() throws Exception {
        String token = authService.register("gods_reader_ok", "password123");

        mockMvc.perform(get("/api/gods/god_odin").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroKey").value("god_odin"))
                .andExpect(jsonPath("$.nameRu").value("Один"))
                .andExpect(jsonPath("$.rarity").value("LEGENDARY"))
                // внутренние поля не должны утекать в API
                .andExpect(jsonPath("$.artPrompt").doesNotExist())
                .andExpect(jsonPath("$.loreSource").doesNotExist());
    }

    @Test
    void getUnknownGod_returnsNotFound() throws Exception {
        String token = authService.register("gods_reader_404", "password123");

        mockMvc.perform(get("/api/gods/does_not_exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
