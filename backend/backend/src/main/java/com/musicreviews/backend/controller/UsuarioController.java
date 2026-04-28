package com.musicreviews.backend.controller;

import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Esta clase expone los endpoints REST relacionados con los usuarios.
// La ruta base de todos sus endpoints es /api/usuarios.
// El registro de nuevos usuarios se hace a través de /api/auth/register (aplica BCrypt).
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

    // GET /api/usuarios/{id} → busca un usuario por su ID. 404 JSON si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado")));
    }

    // GET /api/usuarios/username/{username} → busca un usuario por su username. 404 JSON si no existe.
    @GetMapping("/username/{username}")
    public ResponseEntity<Usuario> obtenerPorUsername(@PathVariable String username) {
        return ResponseEntity.ok(usuarioService.obtenerPorUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado")));
    }

    // PUT /api/usuarios/{id} → actualiza username, foto de perfil y bio. Solo el dueño o ADMIN.
    // No permite cambiar email ni contraseña.
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id, @RequestBody Usuario datos, Authentication auth) {
        return ResponseEntity.ok(usuarioService.actualizar(id, datos, auth.getName(), esAdmin(auth)));
    }

    // PATCH /api/usuarios/{id}/activo → activa o desactiva una cuenta. Solo ADMIN.
    // Body: {"activo": true|false}.
    @PatchMapping("/{id}/activo")
    public ResponseEntity<Usuario> cambiarActivo(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean activo = body.getOrDefault("activo", true);
        return ResponseEntity.ok(usuarioService.cambiarActivo(id, activo));
    }

    // DELETE /api/usuarios/{id} → elimina un usuario. Solo el dueño o ADMIN.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        usuarioService.eliminar(id, auth.getName(), esAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    private boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
