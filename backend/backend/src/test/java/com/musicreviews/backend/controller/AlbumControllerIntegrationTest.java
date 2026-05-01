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

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests de integración para los endpoints públicos del catálogo (página Catálogo, Búsqueda
// y Detalle Álbum). Verifica listado paginado, filtros y consulta por id contra H2.
@SpringBootTest
@ActiveProfiles("test")
class AlbumControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ArtistaRepository artistaRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;
    private Album album;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Orden de borrado: hijas (con FK) antes que padres.
        resenaRepository.deleteAll();
        favoritoRepository.deleteAll();
        albumRepository.deleteAll();
        artistaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Artista artista = new Artista();
        artista.setNombre("Radiohead");
        artista.setGenero("Rock");
        artista = artistaRepository.save(artista);

        album = new Album();
        album.setTitulo("OK Computer");
        album.setGenero("Rock");
        album.setFechaLanzamiento(LocalDate.of(1997, 6, 16));
        album.setArtista(artista);
        album = albumRepository.save(album);
    }

    @Test
    void listarAlbumes_devuelvePaginaConElAlbumCreado() throws Exception {
        // Spring Boot 4 anida los metadatos de Page bajo "$.page" (no en raíz como en versiones previas).
        mockMvc.perform(get("/api/albumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].titulo").value("OK Computer"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void listarAlbumes_filtroPorTituloParcial_devuelveCoincidencias() throws Exception {
        mockMvc.perform(get("/api/albumes").param("titulo", "Computer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("OK Computer"));
    }

    @Test
    void listarAlbumes_filtroPorTituloSinCoincidencias_devuelvePaginaVacia() throws Exception {
        mockMvc.perform(get("/api/albumes").param("titulo", "ZZZ-no-existe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void obtenerPorId_existente_devuelve200ConDatos() throws Exception {
        mockMvc.perform(get("/api/albumes/{id}", album.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("OK Computer"))
                .andExpect(jsonPath("$.artista.nombre").value("Radiohead"));
    }

    @Test
    void obtenerPorId_inexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/albumes/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
