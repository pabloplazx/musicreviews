package com.musicreviews.backend.controller;

import com.musicreviews.backend.dto.AuthResponse;
import com.musicreviews.backend.dto.LoginRequest;
import com.musicreviews.backend.dto.RegisterRequest;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import com.musicreviews.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Ya existe un usuario con ese email");
        }
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Ya existe un usuario con ese username");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));

        usuarioRepository.save(usuario);

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return ResponseEntity.ok(new AuthResponse(token, usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getRol().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            return ResponseEntity.status(401).body("Email o contraseña incorrectos");
        }

        if (!usuario.isActivo()) {
            return ResponseEntity.status(403).body("Cuenta desactivada");
        }

        usuario.setFechaUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return ResponseEntity.ok(new AuthResponse(token, usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getRol().name()));
    }
}
