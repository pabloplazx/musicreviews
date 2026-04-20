package com.musicreviews.backend.controller;

import com.musicreviews.backend.dto.AuthResponse;
import com.musicreviews.backend.dto.LoginRequest;
import com.musicreviews.backend.dto.RegisterRequest;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.security.JwtUtil;
import com.musicreviews.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

// Esta clase expone los endpoints de autenticación: registro y login.
// No requiere token JWT — son las rutas de entrada al sistema.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class AuthController {

    // Acceso al servicio de usuarios en lugar de al repositorio directamente,
    // respetando la separación de capas controller → service → repository.
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // POST /api/auth/register → registra un nuevo usuario.
    // La contraseña se hashea con BCrypt antes de guardarla.
    // Las validaciones de duplicado las gestiona UsuarioService (lanza ReglaNegocioException → 400).
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario guardado = usuarioService.guardar(usuario);

        String token = jwtUtil.generarToken(guardado.getEmail(), guardado.getRol().name());
        return ResponseEntity.ok(new AuthResponse(token, guardado.getId(), guardado.getUsername(), guardado.getEmail(), guardado.getRol().name()));
    }

    // POST /api/auth/login → autentica al usuario y devuelve un token JWT.
    // Actualiza fechaUltimoLogin en cada inicio de sesión.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(request.getEmail()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(401).body("Email o contraseña incorrectos");
        }

        if (!usuario.isActivo()) {
            return ResponseEntity.status(403).body("Cuenta desactivada");
        }

        usuarioService.actualizarUltimoLogin(usuario.getId());

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return ResponseEntity.ok(new AuthResponse(token, usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getRol().name()));
    }
}
