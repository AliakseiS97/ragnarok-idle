package com.ragnarok.idle.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerNewUser_returnsToken() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload("odin", "wisdom123"));

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerDuplicateUsername_returnsConflict() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload("thor", "mjolnir123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithCorrectPassword_returnsToken() throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload("loki", "trickster123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isOk());

        String loginBody = objectMapper.writeValueAsString(new RegisterPayload("loki", "trickster123"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPassword_returnsUnauthorized() throws Exception {
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload("freya", "correct-password"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isOk());

        String loginBody = objectMapper.writeValueAsString(new RegisterPayload("freya", "wrong-password"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    private record RegisterPayload(String username, String password) {
    }
}
