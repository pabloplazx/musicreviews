package com.musicreviews.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Tests de integración para favoritos. Toda la API requiere autenticación
// (página Mis Favoritos), así que aquí verificamos que sin token está bloqueado.
// Los flujos con auth los cubren los tests unitarios de FavoritoService.
@SpringBootTest
@ActiveProfiles("test")
class FavoritoControllerIntegrationTest {

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void listarFavoritos_sinAutenticar_estaBloqueado() throws Exception {
        mockMvc.perform(get("/api/favoritos").param("usuarioId", "1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Esperado 401/403 sin token, recibido " + status);
                    }
                });
    }

    @Test
    void agregarFavorito_sinAutenticar_estaBloqueado() throws Exception {
        mockMvc.perform(post("/api/favoritos")
                        .contentType("application/json")
                        .content("{\"usuario\":{\"id\":1},\"album\":{\"id\":1}}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Esperado 401/403 sin token, recibido " + status);
                    }
                });
    }
}
