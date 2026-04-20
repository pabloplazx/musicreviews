package com.musicreviews.backend.service;

import com.musicreviews.backend.dto.GeneroEstadisticaDTO;
import com.musicreviews.backend.dto.ResumenDTO;
import com.musicreviews.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Esto prueba la lógica de EstadisticasService de forma aislada, sin BD ni Spring.
@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    // Mocks de todos los repositorios que usa EstadisticasService.
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private ArtistaRepository artistaRepository;
    @Mock
    private ResenaRepository resenaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    // Servicio real con los mocks inyectados.
    @InjectMocks
    private EstadisticasService estadisticasService;

    // Esto verifica que obtenerResumen devuelve los totales correctos de cada repositorio.
    @Test
    void obtenerResumen_devuelveTotalesCorrectos() {
        when(albumRepository.count()).thenReturn(469L);
        when(artistaRepository.count()).thenReturn(99L);
        when(resenaRepository.count()).thenReturn(10L);
        when(usuarioRepository.count()).thenReturn(3L);

        ResumenDTO resultado = estadisticasService.obtenerResumen();

        assertEquals(469L, resultado.getTotalAlbumes());
        assertEquals(99L, resultado.getTotalArtistas());
        assertEquals(10L, resultado.getTotalResenas());
        assertEquals(3L, resultado.getTotalUsuarios());
    }

    // Esto verifica que obtenerDistribucionGeneros transforma correctamente los resultados de la query.
    @Test
    void obtenerDistribucionGeneros_devuelveListaDeGeneros() {
        Object[] fila1 = {"hip-hop", 120L};
        Object[] fila2 = {"rock", 85L};
        when(albumRepository.findDistribucionPorGenero()).thenReturn(List.of(fila1, fila2));

        List<GeneroEstadisticaDTO> resultado = estadisticasService.obtenerDistribucionGeneros();

        assertEquals(2, resultado.size());
        assertEquals("hip-hop", resultado.get(0).getGenero());
        assertEquals(120L, resultado.get(0).getTotal());
        assertEquals("rock", resultado.get(1).getGenero());
    }

    // Esto verifica que obtenerDistribucionGeneros devuelve lista vacía cuando no hay álbumes.
    @Test
    void obtenerDistribucionGeneros_sinDatos_devuelveListaVacia() {
        when(albumRepository.findDistribucionPorGenero()).thenReturn(List.of());

        List<GeneroEstadisticaDTO> resultado = estadisticasService.obtenerDistribucionGeneros();

        assertTrue(resultado.isEmpty());
    }

    // Esto verifica que obtenerTopAlbumes devuelve lista vacía cuando no hay reseñas.
    @Test
    void obtenerTopAlbumes_sinResenas_devuelveListaVacia() {
        when(resenaRepository.findTopAlbumesPorPuntuacion(any())).thenReturn(List.of());

        assertTrue(estadisticasService.obtenerTopAlbumes().isEmpty());
    }

    // Esto verifica que obtenerTopArtistas devuelve lista vacía cuando no hay reseñas.
    @Test
    void obtenerTopArtistas_sinResenas_devuelveListaVacia() {
        when(resenaRepository.findTopArtistasPorPuntuacion(any())).thenReturn(List.of());

        assertTrue(estadisticasService.obtenerTopArtistas().isEmpty());
    }
}
