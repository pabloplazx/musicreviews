package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.repository.AlbumRepository;
import com.musicreviews.backend.repository.ArtistaRepository;
import com.musicreviews.backend.repository.FavoritoRepository;
import com.musicreviews.backend.repository.ResenaRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests de integración para los endpoints públicos de artistas (página Detalle Artista
// y filtro de Búsqueda). Verifica listado, filtro por nombre y consulta por id.
@SpringBootTest
@ActiveProfiles("test")
class ArtistaControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ArtistaRepository artistaRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;
    private Artista artista;

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

        artista = new Artista();
        artista.setNombre("Kendrick Lamar");
        artista.setGenero("Hip-Hop");
        artista.setPais("USA");
        artista = artistaRepository.save(artista);
    }

    @Test
    void listarArtistas_devuelveListaConElCreado() throws Exception {
        mockMvc.perform(get("/api/artistas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Kendrick Lamar"));
    }

    @Test
    void listarArtistas_filtroPorNombreParcial_devuelveCoincidencias() throws Exception {
        mockMvc.perform(get("/api/artistas").param("nombre", "Kendrick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Kendrick Lamar"));
    }

    @Test
    void obtenerPorId_existente_devuelve200ConDatos() throws Exception {
        mockMvc.perform(get("/api/artistas/{id}", artista.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Kendrick Lamar"))
                .andExpect(jsonPath("$.pais").value("USA"));
    }

    @Test
    void obtenerPorId_inexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/artistas/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
