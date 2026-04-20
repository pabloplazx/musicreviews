package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los favoritos.
// La ruta base de todos sus endpoints es /api/favoritos.
@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    // GET /api/favoritos?usuarioId= → devuelve todos los favoritos de un usuario.
    @GetMapping
    public ResponseEntity<List<Favorito>> obtenerPorUsuario(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(favoritoService.obtenerPorUsuario(usuarioId));
    }

    // GET /api/favoritos/existe?usuarioId=&albumId= → comprueba si un álbum es favorito. Devuelve true/false.
    @GetMapping("/existe")
    public ResponseEntity<Boolean> esFavorito(
            @RequestParam Long usuarioId, @RequestParam Long albumId) {
        return ResponseEntity.ok(favoritoService.esFavorito(usuarioId, albumId));
    }

    // POST /api/favoritos → añade un álbum a favoritos. 400 si ya estaba añadido.
    @PostMapping
    public ResponseEntity<Favorito> agregar(@RequestBody Favorito favorito) {
        return ResponseEntity.ok(favoritoService.agregar(favorito));
    }

    // DELETE /api/favoritos?usuarioId=&albumId= → elimina un favorito. 204 si ok, 404 si no existía.
    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Long usuarioId, @RequestParam Long albumId) {
        favoritoService.eliminar(usuarioId, albumId);
        return ResponseEntity.noContent().build();
    }
}
