package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.service.ArtistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los artistas.
// La ruta base de todos sus endpoints es /api/artistas.
@RestController
@RequestMapping("/api/artistas")
public class ArtistaController {

    // Esto inyecta el servicio de artistas para delegar en él toda la lógica de negocio.
    @Autowired
    private ArtistaService artistaService;

    // GET /api/artistas → esto devuelve todos los artistas.
    // Si se incluye el parámetro ?nombre=, filtra por nombre usando la búsqueda parcial.
    @GetMapping
    public List<Artista> obtenerTodos(@RequestParam(required = false) String nombre) {
        if (nombre != null && !nombre.isBlank()) {
            return artistaService.buscarPorNombre(nombre);
        }
        return artistaService.obtenerTodos();
    }

    // GET /api/artistas/{id} → esto busca un artista por su ID.
    // Si no existe devuelve 404 Not Found.
    @GetMapping("/{id}")
    public ResponseEntity<Artista> obtenerPorId(@PathVariable Long id) {
        return artistaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/artistas → esto crea un artista nuevo con los datos del cuerpo de la petición.
    @PostMapping
    public ResponseEntity<Artista> crear(@RequestBody Artista artista) {
        return ResponseEntity.ok(artistaService.guardar(artista));
    }

    // PUT /api/artistas/{id} → esto actualiza todos los campos de un artista existente.
    // Si no existe devuelve 404 Not Found.
    @PutMapping("/{id}")
    public ResponseEntity<Artista> actualizar(@PathVariable Long id, @RequestBody Artista datos) {
        try {
            return ResponseEntity.ok(artistaService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/artistas/{id} → esto elimina un artista por su ID.
    // Devuelve 204 No Content si se elimina correctamente, 404 si no existe.
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
