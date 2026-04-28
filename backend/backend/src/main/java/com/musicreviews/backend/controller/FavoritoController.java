package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los favoritos.
// La ruta base de todos sus endpoints es /api/favoritos.
@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    // GET /api/favoritos?usuarioId= → devuelve todos los favoritos de un usuario.
    @GetMapping
    public ResponseEntity<List<Favorito>> obtenerPorUsuario(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(favoritoService.obtenerPorUsuario(usuarioId));
    }

    // GET /api/favoritos/existe?usuarioId=&albumId= → comprueba si un álbum es favorito.
    @GetMapping("/existe")
    public ResponseEntity<Boolean> esFavorito(
            @RequestParam Long usuarioId, @RequestParam Long albumId) {
        return ResponseEntity.ok(favoritoService.esFavorito(usuarioId, albumId));
    }

    // POST /api/favoritos → añade un álbum a favoritos. 400 si duplicado, 403 si se intenta
    // añadir favorito a otro usuario.
    @PostMapping
    public ResponseEntity<Favorito> agregar(@RequestBody Favorito favorito, Authentication auth) {
        return ResponseEntity.ok(favoritoService.agregar(favorito, auth.getName(), esAdmin(auth)));
    }

    // DELETE /api/favoritos?usuarioId=&albumId= → elimina un favorito. Solo el dueño o ADMIN.
    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Long usuarioId, @RequestParam Long albumId, Authentication auth) {
        favoritoService.eliminar(usuarioId, albumId, auth.getName(), esAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
