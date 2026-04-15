package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.service.FavoritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los favoritos.
// La ruta base de todos sus endpoints es /api/favoritos.
@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    // Esto inyecta el servicio de favoritos para delegar en él toda la lógica de negocio.
    @Autowired
    private FavoritoService favoritoService;

    // GET /api/favoritos?usuarioId= → esto devuelve todos los álbumes favoritos de un usuario.
    @GetMapping
    public ResponseEntity<List<Favorito>> obtenerPorUsuario(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(favoritoService.obtenerPorUsuario(usuarioId));
    }

    // GET /api/favoritos/existe?usuarioId=&albumId=
    // → esto comprueba si un álbum concreto ya está en favoritos de un usuario.
    // Devuelve true o false.
    @GetMapping("/existe")
    public ResponseEntity<Boolean> esFavorito(
            @RequestParam Long usuarioId, @RequestParam Long albumId) {
        return ResponseEntity.ok(favoritoService.esFavorito(usuarioId, albumId));
    }

    // POST /api/favoritos → esto añade un álbum a favoritos.
    // El servicio evita que se añada el mismo álbum dos veces.
    @PostMapping
    public ResponseEntity<Object> agregar(@RequestBody Favorito favorito) {
        try {
            return ResponseEntity.ok(favoritoService.agregar(favorito));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/favoritos?usuarioId=&albumId= → esto elimina un álbum de favoritos.
    // Devuelve 204 No Content si se elimina correctamente, 404 si no estaba en favoritos.
    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Long usuarioId, @RequestParam Long albumId) {
        try {
            favoritoService.eliminar(usuarioId, albumId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
