package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.service.ResenaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con las reseñas.
// La ruta base de todos sus endpoints es /api/resenas.
@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    // Esto inyecta el servicio de reseñas para delegar en él toda la lógica de negocio.
    @Autowired
    private ResenaService resenaService;

    // GET /api/resenas?albumId= → esto devuelve todas las reseñas de un álbum concreto.
    // GET /api/resenas?usuarioId= → esto devuelve todas las reseñas de un usuario concreto.
    // Al menos uno de los dos parámetros debe estar presente.
    @GetMapping
    public ResponseEntity<List<Resena>> obtener(
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Long usuarioId) {

        if (albumId != null) {
            return ResponseEntity.ok(resenaService.obtenerPorAlbum(albumId));
        }
        if (usuarioId != null) {
            return ResponseEntity.ok(resenaService.obtenerPorUsuario(usuarioId));
        }
        return ResponseEntity.badRequest().build();
    }

    // GET /api/resenas/usuario/{usuarioId}/album/{albumId}
    // → esto busca la reseña concreta de un usuario sobre un álbum.
    // Si no existe devuelve 404 Not Found.
    @GetMapping("/usuario/{usuarioId}/album/{albumId}")
    public ResponseEntity<Resena> obtenerPorUsuarioYAlbum(
            @PathVariable Long usuarioId, @PathVariable Long albumId) {
        return resenaService.obtenerPorUsuarioYAlbum(usuarioId, albumId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/resenas → esto crea una reseña nueva.
    // El servicio valida que la puntuación sea 1-5 y que el usuario no haya reseñado ya ese álbum.
    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody Resena resena) {
        try {
            return ResponseEntity.ok(resenaService.crear(resena));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/resenas/{id} → esto actualiza la puntuación y el comentario de una reseña existente.
    // Si no existe devuelve 404 Not Found.
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id, @RequestBody Resena datos) {
        try {
            return ResponseEntity.ok(resenaService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/resenas/{id} → esto elimina una reseña por su ID.
    // Devuelve 204 No Content si se elimina correctamente, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            resenaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
