package com.musicreviews.backend.controller;

import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.service.AlbumService;
import com.musicreviews.backend.service.SpotifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Esta clase expone los endpoints REST relacionados con los álbumes.
// La ruta base de todos sus endpoints es /api/albumes.
@RestController
@RequestMapping("/api/albumes")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class AlbumController {

    private final AlbumService albumService;
    private final SpotifyService spotifyService;

    // GET /api/albumes → devuelve álbumes paginados.
    // Acepta ?q= (búsqueda unificada en título o artista), ?titulo= (solo título),
    // ?genero=, ?artistaId= para filtrar, ?page=0&size=12 para paginar,
    // y ?sort= con valores: az (default), za, recientes, antiguos.
    @GetMapping
    public Page<Album> obtenerTodos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Long artistaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "az") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        if (q != null && !q.isBlank()) return albumService.buscar(q, pageable);
        if (titulo != null && !titulo.isBlank()) return albumService.buscarPorTitulo(titulo, pageable);
        if (genero != null && !genero.isBlank()) return albumService.obtenerPorGenero(genero, pageable);
        if (artistaId != null) return albumService.obtenerPorArtista(artistaId, pageable);
        return albumService.obtenerTodos(pageable);
    }

    // Traduce el valor del parámetro sort a un Sort de Spring Data.
    // Valores aceptados: az, za, recientes, antiguos. Cualquier otro → az por defecto.
    private Sort parseSort(String sort) {
        return switch (sort) {
            case "za" -> Sort.by("titulo").descending();
            case "recientes" -> Sort.by("fechaLanzamiento").descending();
            case "antiguos" -> Sort.by("fechaLanzamiento").ascending();
            default -> Sort.by("titulo").ascending();
        };
    }

    // GET /api/albumes/{id} → busca un álbum por su ID. 404 JSON si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Album> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.obtenerPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Álbum no encontrado")));
    }

    // POST /api/albumes → crea un álbum nuevo. Solo accesible por ADMIN (SecurityConfig).
    @PostMapping
    public ResponseEntity<Album> crear(@Valid @RequestBody Album album) {
        return ResponseEntity.ok(albumService.guardar(album));
    }

    // PUT /api/albumes/{id} → actualiza todos los campos de un álbum. 404 si no existe.
    @PutMapping("/{id}")
    public ResponseEntity<Album> actualizar(@PathVariable Long id, @Valid @RequestBody Album datos) {
        return ResponseEntity.ok(albumService.actualizar(id, datos));
    }

    // DELETE /api/albumes/{id} → elimina un álbum. 204 si ok, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        albumService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/albumes/{id}/canciones → devuelve canciones + URL del álbum en Spotify.
    // Endpoint público (permitAll en SecurityConfig para GET /api/albumes/**).
    @GetMapping("/{id}/canciones")
    public ResponseEntity<Map<String, Object>> getCanciones(@PathVariable Long id) {
        return ResponseEntity.ok(spotifyService.getCanciones(id));
    }
}
