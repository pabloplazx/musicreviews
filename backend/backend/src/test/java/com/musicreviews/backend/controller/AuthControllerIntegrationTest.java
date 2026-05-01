package com.musicreviews.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicreviews.backend.dto.LoginRequest;
import com.musicreviews.backend.dto.RegisterRequest;
import com.musicreviews.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests de integración del flujo de autenticación.
// Levantan el contexto Spring entero (controller + service + JPA + security)
// pero contra H2 en memoria, no contra Aiven.
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        usuarioRepository.deleteAll();
    }

    // --- REGISTER ---

    @Test
    void register_conDatosValidos_devuelve200ConToken() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("pablo");
        req.setEmail("pablo@test.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("pablo"))
                .andExpect(jsonPath("$.email").value("pablo@test.com"))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    void register_conEmailDuplicado_devuelve400() throws Exception {
        crearUsuarioEnBd("existente", "duplicado@test.com", "secret123");

        RegisterRequest req = new RegisterRequest();
        req.setUsername("otro");
        req.setEmail("duplicado@test.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_conEmailMalFormado_devuelve400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("pablo");
        req.setEmail("no-es-un-email");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- LOGIN ---

    @Test
    void login_conCredencialesValidas_devuelve200ConToken() throws Exception {
        crearUsuarioEnBd("pablo", "pablo@test.com", "secret123");

        LoginRequest req = new LoginRequest();
        req.setEmail("pablo@test.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("pablo@test.com"));
    }

    @Test
    void login_conPasswordIncorrecta_devuelve400() throws Exception {
        crearUsuarioEnBd("pablo", "pablo@test.com", "secret123");

        LoginRequest req = new LoginRequest();
        req.setEmail("pablo@test.com");
        req.setPassword("wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_conEmailInexistente_devuelve400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("nadie@test.com");
        req.setPassword("secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    private void crearUsuarioEnBd(String username, String email, String passwordPlano) {
        com.musicreviews.backend.model.Usuario u = new com.musicreviews.backend.model.Usuario();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(passwordPlano));
        usuarioRepository.save(u);
    }
}
