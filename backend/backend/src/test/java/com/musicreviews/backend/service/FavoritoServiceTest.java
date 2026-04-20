package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.FavoritoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Esto prueba la lógica de negocio de FavoritoService de forma aislada, sin BD ni Spring.
@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    // Mock del repositorio — simula la BD sin conectarse a ella.
    @Mock
    private FavoritoRepository favoritoRepository;

    // Mock del EntityManager — necesario porque FavoritoService lo usa para refresh() tras save().
    @Mock
    private EntityManager entityManager;

    // Servicio real con los mocks inyectados.
    @InjectMocks
    private FavoritoService favoritoService;

    private Favorito favorito;

    // Esto prepara los objetos de prueba antes de cada test.
    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Album album = new Album();
        album.setId(1L);

        favorito = new Favorito();
        favorito.setId(1L);
        favorito.setUsuario(usuario);
        favorito.setAlbum(album);
    }

    // --- TESTS DE agregar() ---

    // Esto verifica que añadir un favorito nuevo llama a save y refresh.
    @Test
    void agregar_conFavoritoNuevo_guardaCorrectamente() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(false);
        when(favoritoRepository.save(favorito)).thenReturn(favorito);
        doNothing().when(entityManager).refresh(favorito);

        Favorito resultado = favoritoService.agregar(favorito);

        assertNotNull(resultado);
        verify(favoritoRepository).save(favorito);
        verify(entityManager).refresh(favorito);
    }

    // Esto verifica que añadir un álbum que ya está en favoritos lanza excepción.
    @Test
    void agregar_conFavoritoYaExistente_lanzaExcepcion() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> favoritoService.agregar(favorito));
        assertEquals("El álbum ya está en favoritos", ex.getMessage());
        verify(favoritoRepository, never()).save(any());
    }

    // --- TESTS DE eliminar() ---

    // Esto verifica que eliminar un favorito existente llama a deleteByUsuarioIdAndAlbumId.
    @Test
    void eliminar_conFavoritoExistente_eliminaCorrectamente() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        favoritoService.eliminar(1L, 1L);

        verify(favoritoRepository).deleteByUsuarioIdAndAlbumId(1L, 1L);
    }

    // Esto verifica que eliminar un favorito que no existe lanza excepción.
    @Test
    void eliminar_conFavoritoInexistente_lanzaExcepcion() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> favoritoService.eliminar(1L, 99L));
        assertEquals("El álbum no está en favoritos", ex.getMessage());
        verify(favoritoRepository, never()).deleteByUsuarioIdAndAlbumId(any(), any());
    }

    // --- TESTS DE esFavorito() y obtenerPorUsuario() ---

    // Esto verifica que esFavorito devuelve true cuando el repositorio lo confirma.
    @Test
    void esFavorito_cuandoExiste_devuelveTrue() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        assertTrue(favoritoService.esFavorito(1L, 1L));
    }

    // Esto verifica que esFavorito devuelve false cuando el álbum no está en favoritos.
    @Test
    void esFavorito_cuandoNoExiste_devuelveFalse() {
        when(favoritoRepository.existsByUsuarioIdAndAlbumId(1L, 99L)).thenReturn(false);

        assertFalse(favoritoService.esFavorito(1L, 99L));
    }

    // Esto verifica que obtenerPorUsuario delega correctamente en el repositorio.
    @Test
    void obtenerPorUsuario_devuelveLista() {
        when(favoritoRepository.findByUsuarioId(1L)).thenReturn(List.of(favorito));

        List<Favorito> resultado = favoritoService.obtenerPorUsuario(1L);

        assertEquals(1, resultado.size());
        verify(favoritoRepository).findByUsuarioId(1L);
    }
}
