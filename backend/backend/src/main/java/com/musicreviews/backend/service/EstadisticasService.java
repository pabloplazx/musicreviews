package com.musicreviews.backend.service;

import com.musicreviews.backend.dto.*;
import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Esta clase contiene la lógica para calcular rankings y estadísticas de la aplicación.
@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private final AlbumRepository albumRepository;
    private final ArtistaRepository artistaRepository;
    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;

    // Esto cuenta el total de registros en cada tabla y los devuelve agrupados en un solo objeto.
    @Transactional(readOnly = true)
    public ResumenDTO obtenerResumen() {
        return new ResumenDTO(
                albumRepository.count(),
                artistaRepository.count(),
                resenaRepository.count(),
                usuarioRepository.count()
        );
    }

    // Esto obtiene los 10 álbumes con mayor puntuación media. La query agrupa por álbum y ordena por AVG DESC.
    @Transactional(readOnly = true)
    public List<AlbumEstadisticaDTO> obtenerTopAlbumes() {
        return resenaRepository.findTopAlbumesPorPuntuacion(PageRequest.of(0, 10))
                .stream()
                .map(row -> new AlbumEstadisticaDTO((Album) row[0], ((Double) row[1])))
                .toList();
    }

    // Esto obtiene los 10 álbumes con más reseñas escritas. El valor es el conteo total de reseñas.
    @Transactional(readOnly = true)
    public List<AlbumEstadisticaDTO> obtenerMasResenados() {
        return resenaRepository.findAlbumesMasResenados(PageRequest.of(0, 10))
                .stream()
                .map(row -> new AlbumEstadisticaDTO((Album) row[0], ((Long) row[1]).doubleValue()))
                .toList();
    }

    // Esto obtiene los 10 artistas cuyas puntuaciones medias son más altas entre todos sus álbumes reseñados.
    @Transactional(readOnly = true)
    public List<ArtistaEstadisticaDTO> obtenerTopArtistas() {
        return resenaRepository.findTopArtistasPorPuntuacion(PageRequest.of(0, 10))
                .stream()
                .map(row -> new ArtistaEstadisticaDTO((Artista) row[0], (Double) row[1]))
                .toList();
    }

    // Esto cuenta cuántos álbumes hay de cada género y devuelve la lista ordenada de mayor a menor.
    @Transactional(readOnly = true)
    public List<GeneroEstadisticaDTO> obtenerDistribucionGeneros() {
        return albumRepository.findDistribucionPorGenero()
                .stream()
                .map(row -> new GeneroEstadisticaDTO((String) row[0], (Long) row[1]))
                .toList();
    }

    // Esto obtiene los 10 álbumes mejor puntuados dentro del género indicado.
    @Transactional(readOnly = true)
    public List<AlbumEstadisticaDTO> obtenerTopPorGenero(String genero) {
        return resenaRepository.findTopAlbumesPorGenero(genero, PageRequest.of(0, 10))
                .stream()
                .map(row -> new AlbumEstadisticaDTO((Album) row[0], (Double) row[1]))
                .toList();
    }

    // Esto devuelve las 10 reseñas más recientes de la app, ordenadas por fecha de creación descendente.
    @Transactional(readOnly = true)
    public List<Resena> obtenerActividadReciente() {
        return resenaRepository.findTop10ByOrderByFechaCreacionDesc();
    }

    // Esto devuelve los 10 álbumes añadidos más recientemente al catálogo, identificados por ID más alto.
    @Transactional(readOnly = true)
    public List<Album> obtenerAlbumesRecientes() {
        return albumRepository.findTop10ByOrderByIdDesc();
    }
}
