package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.service.ArtistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los artistas.
// La ruta base de todos sus endpoints es /api/artistas.
@RestController
@RequestMapping("/api/artistas")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class ArtistaController {

    private final ArtistaService artistaService;

    // GET /api/artistas → devuelve todos los artistas.
    // Con ?nombre= filtra por nombre usando búsqueda parcial.
    @GetMapping
    public List<Artista> obtenerTodos(@RequestParam(required = false) String nombre) {
        if (nombre != null && !nombre.isBlank()) {
            return artistaService.buscarPorNombre(nombre);
        }
        return artistaService.obtenerTodos();
    }

    // GET /api/artistas/{id} → busca un artista por su ID. 404 si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Artista> obtenerPorId(@PathVariable Long id) {
        return artistaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/artistas → crea un artista nuevo. Solo accesible por ADMIN (SecurityConfig).
    @PostMapping
    public ResponseEntity<Artista> crear(@RequestBody Artista artista) {
        return ResponseEntity.ok(artistaService.guardar(artista));
    }

    // PUT /api/artistas/{id} → actualiza todos los campos de un artista. 404 si no existe.
    @PutMapping("/{id}")
    public ResponseEntity<Artista> actualizar(@PathVariable Long id, @RequestBody Artista datos) {
        try {
            return ResponseEntity.ok(artistaService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/artistas/{id} → elimina un artista. 204 si ok, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            artistaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
