package com.musicreviews.backend.controller;

import com.musicreviews.backend.dto.*;
import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST de rankings y estadísticas de la aplicación.
// La ruta base de todos sus endpoints es /api/estadisticas.
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    // GET /api/estadisticas/resumen → totales globales de álbumes, artistas, reseñas y usuarios.
    @GetMapping("/resumen")
    public ResumenDTO resumen() {
        return estadisticasService.obtenerResumen();
    }

    // GET /api/estadisticas/top-albumes → top 10 álbumes con mejor puntuación media.
    @GetMapping("/top-albumes")
    public List<AlbumEstadisticaDTO> topAlbumes() {
        return estadisticasService.obtenerTopAlbumes();
    }

    // GET /api/estadisticas/mas-resenados → top 10 álbumes con más reseñas.
    @GetMapping("/mas-resenados")
    public List<AlbumEstadisticaDTO> masResenados() {
        return estadisticasService.obtenerMasResenados();
    }

    // GET /api/estadisticas/top-artistas → top 10 artistas con mejor puntuación media en sus álbumes.
    @GetMapping("/top-artistas")
    public List<ArtistaEstadisticaDTO> topArtistas() {
        return estadisticasService.obtenerTopArtistas();
    }

    // GET /api/estadisticas/generos → distribución de álbumes por género.
    @GetMapping("/generos")
    public List<GeneroEstadisticaDTO> generos() {
        return estadisticasService.obtenerDistribucionGeneros();
    }

    // GET /api/estadisticas/top-por-genero?genero=hip-hop → top 10 álbumes de un género concreto.
    @GetMapping("/top-por-genero")
    public List<AlbumEstadisticaDTO> topPorGenero(@RequestParam String genero) {
        return estadisticasService.obtenerTopPorGenero(genero);
    }

    // GET /api/estadisticas/actividad-reciente → últimas 10 reseñas escritas.
    @GetMapping("/actividad-reciente")
    public List<Resena> actividadReciente() {
        return estadisticasService.obtenerActividadReciente();
    }

    // GET /api/estadisticas/albumes-recientes → últimos 10 álbumes añadidos al catálogo.
    @GetMapping("/albumes-recientes")
    public List<Album> albumesRecientes() {
        return estadisticasService.obtenerAlbumesRecientes();
    }
}
