package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los álbumes.
// La ruta base de todos sus endpoints es /api/albumes.
@RestController
@RequestMapping("/api/albumes")
public class AlbumController {

    // Esto inyecta el servicio de álbumes para delegar en él toda la lógica de negocio.
    @Autowired
    private AlbumService albumService;

    // GET /api/albumes → esto devuelve todos los álbumes.
    // Acepta parámetros opcionales ?titulo=, ?genero= y ?artistaId= para filtrar resultados.
    @GetMapping
    public List<Album> obtenerTodos(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Long artistaId) {

        if (titulo != null && !titulo.isBlank()) {
            return albumService.buscarPorTitulo(titulo);
        }
        if (genero != null && !genero.isBlank()) {
            return albumService.obtenerPorGenero(genero);
        }
        if (artistaId != null) {
            return albumService.obtenerPorArtista(artistaId);
        }
        return albumService.obtenerTodos();
    }

    // GET /api/albumes/{id} → esto busca un álbum por su ID.
    // Si no existe devuelve 404 Not Found.
    @GetMapping("/{id}")
    public ResponseEntity<Album> obtenerPorId(@PathVariable Long id) {
        return albumService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/albumes → esto crea un álbum nuevo con los datos del cuerpo de la petición.
    @PostMapping
    public ResponseEntity<Album> crear(@RequestBody Album album) {
        return ResponseEntity.ok(albumService.guardar(album));
    }

    // PUT /api/albumes/{id} → esto actualiza todos los campos de un álbum existente.
    // Si no existe devuelve 404 Not Found.
    @PutMapping("/{id}")
    public ResponseEntity<Album> actualizar(@PathVariable Long id, @RequestBody Album datos) {
        try {
            return ResponseEntity.ok(albumService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/albumes/{id} → esto elimina un álbum por su ID.
    // Devuelve 204 No Content si se elimina correctamente, 404 si no existe.
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
