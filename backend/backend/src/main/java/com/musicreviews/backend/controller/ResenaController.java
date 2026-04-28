package com.musicreviews.backend.controller;

import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.service.ResenaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        throw new ReglaNegocioException("Se requiere albumId o usuarioId como parámetro");
    }

    // GET /api/resenas/usuario/{usuarioId}/album/{albumId} → reseña concreta de un usuario sobre un álbum.
    @GetMapping("/usuario/{usuarioId}/album/{albumId}")
    public ResponseEntity<Resena> obtenerPorUsuarioYAlbum(
            @PathVariable Long usuarioId, @PathVariable Long albumId) {
        return ResponseEntity.ok(resenaService.obtenerPorUsuarioYAlbum(usuarioId, albumId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada")));
    }

    // POST /api/resenas → crea una reseña. Valida puntuación (0.5-5), duplicado y que el
    // usuarioId del body coincide con el del token (un usuario no puede crear reseñas en
    // nombre de otro). 403 si se intenta suplantar.
    @PostMapping
    public ResponseEntity<Resena> crear(@RequestBody Resena resena, Authentication auth) {
        return ResponseEntity.ok(resenaService.crear(resena, auth.getName(), esAdmin(auth)));
    }

    // PUT /api/resenas/{id} → actualiza puntuación y comentario. Solo el dueño o ADMIN.
    // 400 si puntuación inválida, 403 si no es dueño, 404 si no existe.
    @PutMapping("/{id}")
    public ResponseEntity<Resena> actualizar(@PathVariable Long id, @RequestBody Resena datos, Authentication auth) {
        return ResponseEntity.ok(resenaService.actualizar(id, datos, auth.getName(), esAdmin(auth)));
    }

    // DELETE /api/resenas/{id} → elimina una reseña. Solo el dueño o ADMIN.
    // 204 si ok, 403 si no es dueño, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        resenaService.eliminar(id, auth.getName(), esAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
