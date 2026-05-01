package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.AlbumRepository;
import com.musicreviews.backend.repository.ArtistaRepository;
import com.musicreviews.backend.repository.FavoritoRepository;
import com.musicreviews.backend.repository.ResenaRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests de integración para los endpoints públicos de usuarios (página Perfil Usuario).
// Solo /api/usuarios/username/{username} es público; el resto requiere autenticación.
@SpringBootTest
@ActiveProfiles("test")
class UsuarioControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ArtistaRepository artistaRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        resenaRepository.deleteAll();
        favoritoRepository.deleteAll();
        albumRepository.deleteAll();
        artistaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario u = new Usuario();
        u.setUsername("pablo");
        u.setEmail("pablo@test.com");
        u.setPassword(passwordEncoder.encode("secret123"));
        u.setBio("Bio de prueba");
        usuarioRepository.save(u);
    }

    @Test
    void obtenerPorUsername_existente_devuelve200ConDatosPublicos() throws Exception {
        mockMvc.perform(get("/api/usuarios/username/{username}", "pablo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("pablo"))
                .andExpect(jsonPath("$.bio").value("Bio de prueba"))
                // El password no debe serializarse nunca (WRITE_ONLY).
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void obtenerPorUsername_inexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/usuarios/username/{username}", "nadie"))
                .andExpect(status().isNotFound());
    }
}
