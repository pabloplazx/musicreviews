package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.FavoritoRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Esto prueba la lógica de negocio de FavoritoService de forma aislada, sin BD ni Spring.
@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    private static final String EMAIL = "test@test.com";
    private static final String OTRO_EMAIL = "otro@test.com";

    @Mock
    private FavoritoRepository favoritoRepository;

    // El servicio consulta UsuarioRepository en agregar/eliminar para resolver el id del
    // usuario autenticado a partir del email del JWT.
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private FavoritoService favoritoService;

    private Favorito favorito;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(EMAIL);

        Album album = new Album();
        album.setId(1L);

        favorito = new Favorito();
        favorito.setId(1L);
        favorito.setUsuario(usuario);
        favorito.setAlbum(album);
    }

    // --- TESTS DE agregar() ---

    @Test
    void agregar_conFavoritoNuevo_guardaCorrectamente() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(false);
        when(favoritoRepository.save(favorito)).thenReturn(favorito);
        doNothing().when(entityManager).refresh(favorito);

        Favorito resultado = favoritoService.agregar(favorito, EMAIL, false);

        assertNotNull(resultado);
        verify(favoritoRepository).save(favorito);
        verify(entityManager).refresh(favorito);
    }

    @Test
    void agregar_conFavoritoYaExistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> favoritoService.agregar(favorito, EMAIL, false));
        assertEquals("El álbum ya está en favoritos", ex.getMessage());
        verify(favoritoRepository, never()).save(any());
    }

    // Verificación de propiedad: maría no puede añadir favoritos a la lista de carlos.
    @Test
    void agregar_paraOtroUsuario_lanzaAccesoDenegada() {
        Usuario otro = new Usuario();
        otro.setId(2L);
        otro.setEmail(OTRO_EMAIL);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(otro));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> favoritoService.agregar(favorito, EMAIL, false));
        assertEquals("Solo puedes gestionar tus propios favoritos", ex.getMessage());
        verify(favoritoRepository, never()).save(any());
    }

    // --- TESTS DE eliminar() ---

    @Test
    void eliminar_conFavoritoExistente_eliminaCorrectamente() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        favoritoService.eliminar(1L, 1L, EMAIL, false);

        verify(favoritoRepository).deleteByUsuarioIdAndAlbumId(1L, 1L);
    }

    @Test
    void eliminar_conFavoritoInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> favoritoService.eliminar(1L, 99L, EMAIL, false));
        assertEquals("El álbum no está en favoritos", ex.getMessage());
        verify(favoritoRepository, never()).deleteByUsuarioIdAndAlbumId(any(), any());
    }

    @Test
    void eliminar_deOtroUsuario_lanzaAccesoDenegada() {
        Usuario otro = new Usuario();
        otro.setId(2L);
        otro.setEmail(OTRO_EMAIL);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(otro));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> favoritoService.eliminar(1L, 1L, EMAIL, false));
        assertEquals("Solo puedes gestionar tus propios favoritos", ex.getMessage());
        verify(favoritoRepository, never()).deleteByUsuarioIdAndAlbumId(any(), any());
    }

    // --- TESTS DE esFavorito() y obtenerPorUsuario() ---

    @Test
    void esFavorito_cuandoExiste_devuelveTrue() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);
        assertTrue(favoritoService.esFavorito(1L, 1L));
    }

    @Test
    void esFavorito_cuandoNoExiste_devuelveFalse() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 99L)).thenReturn(false);
        assertFalse(favoritoService.esFavorito(1L, 99L));
    }

    @Test
    void obtenerPorUsuario_devuelveLista() {
        when(favoritoRepository.findByUsuarioId(1L)).thenReturn(List.of(favorito));

        List<Favorito> resultado = favoritoService.obtenerPorUsuario(1L);

        assertEquals(1, resultado.size());
        verify(favoritoRepository).findByUsuarioId(1L);
    }
}
