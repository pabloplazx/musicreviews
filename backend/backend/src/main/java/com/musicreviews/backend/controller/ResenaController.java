package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.service.ResenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con las reseñas.
// La ruta base de todos sus endpoints es /api/resenas.
@RestController
@RequestMapping("/api/resenas")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class ResenaController {

    private final ResenaService resenaService;

    // GET /api/resenas?albumId= → reseñas de un álbum.
    // GET /api/resenas?usuarioId= → reseñas de un usuario.
    @GetMapping
    public ResponseEntity<List<Resena>> obtener(
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) Long usuarioId) {

        if (albumId != null) return ResponseEntity.ok(resenaService.obtenerPorAlbum(albumId));
        if (usuarioId != null) return ResponseEntity.ok(resenaService.obtenerPorUsuario(usuarioId));
        return ResponseEntity.badRequest().build();
    }

    // GET /api/resenas/usuario/{usuarioId}/album/{albumId} → reseña concreta de un usuario sobre un álbum.
    @GetMapping("/usuario/{usuarioId}/album/{albumId}")
    public ResponseEntity<Resena> obtenerPorUsuarioYAlbum(
            @PathVariable Long usuarioId, @PathVariable Long albumId) {
        return resenaService.obtenerPorUsuarioYAlbum(usuarioId, albumId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/resenas → crea una reseña. Valida puntuación (1-5) y que no exista duplicado.
    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody Resena resena) {
        try {
            return ResponseEntity.ok(resenaService.crear(resena));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/resenas/{id} → actualiza puntuación y comentario. 400 si puntuación inválida.
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id, @RequestBody Resena datos) {
        try {
            return ResponseEntity.ok(resenaService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/resenas/{id} → elimina una reseña. 204 si ok, 404 si no existe.
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
