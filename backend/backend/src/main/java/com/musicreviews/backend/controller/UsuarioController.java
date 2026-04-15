package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los usuarios.
// La ruta base de todos sus endpoints es /api/usuarios.
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    // Esto inyecta el servicio de usuarios para delegar en él toda la lógica de negocio.
    @Autowired
    private UsuarioService usuarioService;

    // GET /api/usuarios → esto devuelve la lista completa de usuarios.
    @GetMapping
    public List<Usuario> obtenerTodos() {
        return usuarioService.obtenerTodos();
    }

    // GET /api/usuarios/{id} → esto busca un usuario por su ID.
    // Si no existe devuelve 404 Not Found.
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/usuarios → esto crea un usuario nuevo con los datos del cuerpo de la petición.
    // Si el email o username ya existen, el servicio lanza una excepción.
    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.guardar(usuario));
    }

    // PUT /api/usuarios/{id} → esto actualiza los datos de perfil de un usuario existente.
    // Si no existe devuelve 404 Not Found.
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id, @RequestBody Usuario datos) {
        try {
            return ResponseEntity.ok(usuarioService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/usuarios/{id} → esto elimina un usuario por su ID.
    // Devuelve 204 No Content si se elimina correctamente, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            usuarioService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
