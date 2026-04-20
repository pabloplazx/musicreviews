package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.ResenaRepository;
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

// Esto prueba la lógica de negocio de ResenaService de forma aislada, sin BD ni Spring.
// Se usan mocks (objetos falsos) para simular el comportamiento del repositorio.
@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    // Mock del repositorio — simula la BD sin conectarse a ella.
    @Mock
    private ResenaRepository resenaRepository;

    // Servicio real con el mock inyectado en lugar del repositorio real.
    @InjectMocks
    private ResenaService resenaService;

    private Resena resena;
    private Usuario usuario;
    private Album album;

    // Esto prepara los objetos de prueba antes de cada test.
    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);

        album = new Album();
        album.setId(1L);

        resena = new Resena();
        resena.setId(1L);
        resena.setUsuario(usuario);
        resena.setAlbum(album);
        resena.setPuntuacion(4);
        resena.setComentario("Muy buen álbum");
    }

    // --- TESTS DE crear() ---

    // Esto verifica que una reseña válida se guarda correctamente.
    @Test
    void crear_conDatosValidos_guardaYDevuelveResena() {
        when(resenaRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(false);
        when(resenaRepository.save(resena)).thenReturn(resena);

        Resena resultado = resenaService.crear(resena);

        assertNotNull(resultado);
        assertEquals(4, resultado.getPuntuacion());
        verify(resenaRepository).save(resena);
    }

    // Esto verifica que una puntuación de 0 lanza excepción.
    @Test
    void crear_conPuntuacionCero_lanzaExcepcion() {
        resena.setPuntuacion(0);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resenaService.crear(resena));
        assertEquals("La puntuación debe estar entre 1 y 5", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // Esto verifica que una puntuación de 6 lanza excepción.
    @Test
    void crear_conPuntuacionSeis_lanzaExcepcion() {
        resena.setPuntuacion(6);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resenaService.crear(resena));
        assertEquals("La puntuación debe estar entre 1 y 5", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // Esto verifica que no se puede crear una segunda reseña del mismo usuario para el mismo álbum.
    @Test
    void crear_conResenaYaExistente_lanzaExcepcion() {
        when(resenaRepository.existsByUsuarioIdAndAlbumId(1L, 1L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resenaService.crear(resena));
        assertEquals("El usuario ya ha reseñado este álbum", ex.getMessage());
        verify(resenaRepository, never()).save(any());
    }

    // --- TESTS DE actualizar() ---

    // Esto verifica que actualizar una reseña existente con datos válidos funciona correctamente.
    @Test
    void actualizar_conDatosValidos_actualizaYDevuelveResena() {
        Resena datosNuevos = new Resena();
        datosNuevos.setPuntuacion(5);
        datosNuevos.setComentario("Obra maestra");

        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(resena)).thenReturn(resena);

        Resena resultado = resenaService.actualizar(1L, datosNuevos);

        assertEquals(5, resultado.getPuntuacion());
        assertEquals("Obra maestra", resultado.getComentario());
    }

    // Esto verifica que actualizar con puntuación inválida lanza excepción.
    @Test
    void actualizar_conPuntuacionInvalida_lanzaExcepcion() {
        Resena datosNuevos = new Resena();
        datosNuevos.setPuntuacion(7);

        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));

        assertThrows(RuntimeException.class, () -> resenaService.actualizar(1L, datosNuevos));
        verify(resenaRepository, never()).save(any());
    }

    // Esto verifica que actualizar una reseña que no existe lanza excepción.
    @Test
    void actualizar_conIdInexistente_lanzaExcepcion() {
        when(resenaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> resenaService.actualizar(99L, resena));
    }

    // --- TESTS DE eliminar() ---

    // Esto verifica que eliminar una reseña existente llama a deleteById.
    @Test
    void eliminar_conIdExistente_eliminaCorrectamente() {
        when(resenaRepository.existsById(1L)).thenReturn(true);

        resenaService.eliminar(1L);

        verify(resenaRepository).deleteById(1L);
    }

    // Esto verifica que eliminar una reseña inexistente lanza excepción.
    @Test
    void eliminar_conIdInexistente_lanzaExcepcion() {
        when(resenaRepository.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> resenaService.eliminar(99L));
        assertEquals("Reseña no encontrada", ex.getMessage());
        verify(resenaRepository, never()).deleteById(any());
    }

    // --- TESTS DE consultas ---

    // Esto verifica que obtenerPorAlbum delega correctamente en el repositorio.
    @Test
    void obtenerPorAlbum_devuelveLista() {
        when(resenaRepository.findByAlbumId(1L)).thenReturn(List.of(resena));

        List<Resena> resultado = resenaService.obtenerPorAlbum(1L);

        assertEquals(1, resultado.size());
        verify(resenaRepository).findByAlbumId(1L);
    }

    // Esto verifica que obtenerPorUsuario delega correctamente en el repositorio.
    @Test
    void obtenerPorUsuario_devuelveLista() {
        when(resenaRepository.findByUsuarioId(1L)).thenReturn(List.of(resena));

        List<Resena> resultado = resenaService.obtenerPorUsuario(1L);

        assertEquals(1, resultado.size());
        verify(resenaRepository).findByUsuarioId(1L);
    }
}
