package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.repository.ArtistaRepository;
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

// Esto prueba la lógica de negocio de ArtistaService de forma aislada, sin BD ni Spring.
@ExtendWith(MockitoExtension.class)
class ArtistaServiceTest {

    // Mock del repositorio — simula la BD sin conectarse a ella.
    @Mock
    private ArtistaRepository artistaRepository;

    // Servicio real con el mock inyectado.
    @InjectMocks
    private ArtistaService artistaService;

    private Artista artista;

    // Esto prepara el objeto de prueba antes de cada test.
    @BeforeEach
    void setUp() {
        artista = new Artista();
        artista.setId(1L);
        artista.setNombre("Radiohead");
        artista.setGenero("rock alternativo");
        artista.setPais("Reino Unido");
    }

    // --- TESTS DE guardar() ---

    // Esto verifica que guardar un artista nuevo llama a save y devuelve el artista.
    @Test
    void guardar_llamaASaveYDevuelveArtista() {
        when(artistaRepository.save(artista)).thenReturn(artista);

        Artista resultado = artistaService.guardar(artista);

        assertNotNull(resultado);
        assertEquals("Radiohead", resultado.getNombre());
        verify(artistaRepository).save(artista);
    }

    // --- TESTS DE actualizar() ---

    // Esto verifica que actualizar un artista existente modifica todos sus campos.
    @Test
    void actualizar_conIdExistente_actualizaTodosLosCampos() {
        Artista datosNuevos = new Artista();
        datosNuevos.setNombre("Radiohead Updated");
        datosNuevos.setFoto("nueva_foto.jpg");
        datosNuevos.setBiografia("Nueva bio");
        datosNuevos.setGenero("art rock");
        datosNuevos.setPais("Reino Unido");

        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista));
        when(artistaRepository.save(artista)).thenReturn(artista);

        Artista resultado = artistaService.actualizar(1L, datosNuevos);

        assertEquals("Radiohead Updated", resultado.getNombre());
        assertEquals("art rock", resultado.getGenero());
    }

    // Esto verifica que actualizar un artista que no existe lanza excepción.
    @Test
    void actualizar_conIdInexistente_lanzaExcepcion() {
        when(artistaRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> artistaService.actualizar(99L, artista));
        assertEquals("Artista no encontrado", ex.getMessage());
    }

    // --- TESTS DE eliminar() ---

    // Esto verifica que eliminar un artista existente llama a deleteById.
    @Test
    void eliminar_conIdExistente_eliminaCorrectamente() {
        when(artistaRepository.existsById(1L)).thenReturn(true);

        artistaService.eliminar(1L);

        verify(artistaRepository).deleteById(1L);
    }

    // Esto verifica que eliminar un artista inexistente lanza excepción.
    @Test
    void eliminar_conIdInexistente_lanzaExcepcion() {
        when(artistaRepository.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> artistaService.eliminar(99L));
        assertEquals("Artista no encontrado", ex.getMessage());
        verify(artistaRepository, never()).deleteById(any());
    }

    // --- TESTS DE consultas ---

    // Esto verifica que buscarPorNombre delega correctamente en el repositorio.
    @Test
    void buscarPorNombre_devuelveLista() {
        when(artistaRepository.findByNombreContainingIgnoreCase("radio")).thenReturn(List.of(artista));

        List<Artista> resultado = artistaService.buscarPorNombre("radio");

        assertEquals(1, resultado.size());
        assertEquals("Radiohead", resultado.get(0).getNombre());
    }

    // Esto verifica que obtenerPorId devuelve el artista cuando existe.
    @Test
    void obtenerPorId_cuandoExiste_devuelveArtista() {
        when(artistaRepository.findById(1L)).thenReturn(Optional.of(artista));

        Optional<Artista> resultado = artistaService.obtenerPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Radiohead", resultado.get().getNombre());
    }
}
