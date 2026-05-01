package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Album;
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

// Tests de integración para los endpoints públicos de estadísticas (página Rankings).
// Solo verifica que devuelven 200 con la estructura esperada — la lógica de cálculo
// está probada en EstadisticasServiceTest (unitarios).
@SpringBootTest
@ActiveProfiles("test")
class EstadisticasControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ArtistaRepository artistaRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

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

        Artista artista = new Artista();
        artista.setNombre("Bad Bunny");
        artista.setGenero("Reggaeton");
        artista = artistaRepository.save(artista);

        Album album = new Album();
        album.setTitulo("Un Verano Sin Ti");
        album.setGenero("Reggaeton");
        album.setArtista(artista);
        albumRepository.save(album);
    }

    @Test
    void resumen_devuelve200ConTotales() throws Exception {
        mockMvc.perform(get("/api/estadisticas/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlbumes").value(1))
                .andExpect(jsonPath("$.totalArtistas").value(1));
    }

    @Test
    void topAlbumes_devuelveLista() throws Exception {
        mockMvc.perform(get("/api/estadisticas/top-albumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void topArtistas_devuelveLista() throws Exception {
        mockMvc.perform(get("/api/estadisticas/top-artistas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void distribucionGeneros_devuelveLista() throws Exception {
        mockMvc.perform(get("/api/estadisticas/generos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void albumesRecientes_devuelveLista() throws Exception {
        mockMvc.perform(get("/api/estadisticas/albumes-recientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
