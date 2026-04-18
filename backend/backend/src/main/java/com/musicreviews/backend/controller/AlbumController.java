package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los álbumes.
// La ruta base de todos sus endpoints es /api/albumes.
@RestController
@RequestMapping("/api/albumes")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class AlbumController {

    private final AlbumService albumService;

    // GET /api/albumes → devuelve todos los álbumes.
    // Acepta ?titulo=, ?genero= y ?artistaId= para filtrar resultados.
    @GetMapping
    public List<Album> obtenerTodos(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Long artistaId) {

        if (titulo != null && !titulo.isBlank()) return albumService.buscarPorTitulo(titulo);
        if (genero != null && !genero.isBlank()) return albumService.obtenerPorGenero(genero);
        if (artistaId != null) return albumService.obtenerPorArtista(artistaId);
        return albumService.obtenerTodos();
    }

    // GET /api/albumes/{id} → busca un álbum por su ID. 404 si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Album> obtenerPorId(@PathVariable Long id) {
        return albumService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/albumes → crea un álbum nuevo. Solo accesible por ADMIN (SecurityConfig).
    @PostMapping
    public ResponseEntity<Album> crear(@RequestBody Album album) {
        return ResponseEntity.ok(albumService.guardar(album));
    }

    // PUT /api/albumes/{id} → actualiza todos los campos de un álbum. 404 si no existe.
    @PutMapping("/{id}")
    public ResponseEntity<Album> actualizar(@PathVariable Long id, @RequestBody Album datos) {
        try {
            return ResponseEntity.ok(albumService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/albumes/{id} → elimina un álbum. 204 si ok, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            albumService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
