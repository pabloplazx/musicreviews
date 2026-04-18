package com.musicreviews.backend.controller;

import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Esta clase expone los endpoints REST relacionados con los usuarios.
// La ruta base de todos sus endpoints es /api/usuarios.
// NOTA: el registro de nuevos usuarios se hace a través de /api/auth/register, no aquí.
// Ese endpoint aplica BCrypt a la contraseña; crear usuarios desde aquí guardaría la contraseña en texto plano.
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // GET /api/usuarios → devuelve la lista completa de usuarios.
    @GetMapping
    public List<Usuario> obtenerTodos() {
        return usuarioService.obtenerTodos();
    }

    // GET /api/usuarios/{id} → busca un usuario por su ID. 404 si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/usuarios/{id} → actualiza username, foto de perfil y bio. No permite cambiar email ni contraseña.
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id, @RequestBody Usuario datos) {
        try {
            return ResponseEntity.ok(usuarioService.actualizar(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/usuarios/{id} → elimina un usuario. 204 si ok, 404 si no existe.
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
