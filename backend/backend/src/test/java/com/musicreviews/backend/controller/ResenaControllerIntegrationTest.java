package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.model.Resena;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests de integración para los GET públicos de reseñas (lo que consume la página
// Detalle Álbum y Perfil Usuario). También verifica que crear una reseña sin token
// está bloqueado por seguridad (página Crear Reseña requiere login).
@SpringBootTest
@ActiveProfiles("test")
class ResenaControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ArtistaRepository artistaRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ResenaRepository resenaRepository;
    @Autowired private FavoritoRepository favoritoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private Album album;
    private Usuario usuario;

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
        artista.setNombre("Frank Ocean");
        artista = artistaRepository.save(artista);

        album = new Album();
        album.setTitulo("Blonde");
        album.setArtista(artista);
        album = albumRepository.save(album);

        usuario = new Usuario();
        usuario.setUsername("pablo");
        usuario.setEmail("pablo@test.com");
        usuario.setPassword(passwordEncoder.encode("secret123"));
        usuario = usuarioRepository.save(usuario);

        Resena resena = new Resena();
        resena.setAlbum(album);
        resena.setUsuario(usuario);
        resena.setPuntuacion(4.5);
        resena.setComentario("Obra maestra");
        resenaRepository.save(resena);
    }

    @Test
    void obtenerPorAlbumId_devuelveListaConLaResenaCreada() throws Exception {
        mockMvc.perform(get("/api/resenas").param("albumId", album.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].puntuacion").value(4.5))
                .andExpect(jsonPath("$[0].comentario").value("Obra maestra"));
    }

    @Test
    void obtenerPorUsuarioId_devuelveListaConLaResenaCreada() throws Exception {
        mockMvc.perform(get("/api/resenas").param("usuarioId", usuario.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void obtener_sinParametros_devuelve400() throws Exception {
        mockMvc.perform(get("/api/resenas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearResena_sinAutenticar_devuelve401o403() throws Exception {
        String body = "{\"puntuacion\":5,\"album\":{\"id\":" + album.getId() + "},\"usuario\":{\"id\":" + usuario.getId() + "}}";

        mockMvc.perform(post("/api/resenas")
                        .contentType("application/json")
                        .content(body))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Esperado 401 o 403 sin token, recibido " + status);
                    }
                });
    }
}
