package com.musicreviews.backend.controller;

import com.musicreviews.backend.dto.AuthResponse;
import com.musicreviews.backend.dto.LoginRequest;
import com.musicreviews.backend.dto.RegisterRequest;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.RefreshToken;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import com.musicreviews.backend.security.JwtUtil;
import com.musicreviews.backend.service.EmailService;
import com.musicreviews.backend.service.RefreshTokenService;
import com.musicreviews.backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    // POST /api/auth/register → crea la cuenta, envía email de verificación y devuelve mensaje.
    // No inicia sesión automáticamente — el usuario debe confirmar el email primero.
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        String token = usuarioService.registrar(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        emailService.enviarConfirmacion(request.getEmail(), request.getUsername(), token);

        return ResponseEntity.ok(Map.of("mensaje", "Hemos enviado un correo a " + request.getEmail() + ". Confirma tu cuenta para iniciar sesión."));
    }

    // POST /api/auth/login → autentica, verifica el email y devuelve access token + refresh token.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.obtenerPorEmail(request.getEmail()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ReglaNegocioException("Email o contraseña incorrectos");
        }

        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("Cuenta desactivada");
        }

        if (!usuario.isEmailVerificado()) {
            throw new ReglaNegocioException("Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada.");
        }

        usuarioService.actualizarUltimoLogin(usuario.getId());

        String accessToken = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        RefreshToken refreshToken = refreshTokenService.crear(usuario.getId());

        return ResponseEntity.ok(new AuthResponse(
                accessToken, refreshToken.getToken(),
                usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getRol().name()));
    }

    // GET /api/auth/verificar?token=uuid → valida el token de email y activa la cuenta.
    // Devuelve un mensaje de éxito — el usuario inicia sesión normalmente después.
    @GetMapping("/verificar")
    public ResponseEntity<Map<String, String>> verificar(@RequestParam String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElseThrow(() -> new ReglaNegocioException("Enlace de verificación inválido o ya utilizado"));

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Cuenta verificada correctamente. Ya puedes iniciar sesión."));
    }

    // POST /api/auth/refresh → intercambia un refresh token válido por un nuevo access token.
    // Body: { "refreshToken": "uuid" }
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String tokenStr = body.get("refreshToken");
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new ReglaNegocioException("Refresh token requerido");
        }

        RefreshToken refreshToken = refreshTokenService.verificar(tokenStr);
        Usuario usuario = refreshToken.getUsuario();
        String nuevoAccessToken = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());

        return ResponseEntity.ok(Map.of("token", nuevoAccessToken));
    }

    // POST /api/auth/logout → invalida el refresh token del servidor.
    // Body: { "refreshToken": "uuid" }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        String tokenStr = body.get("refreshToken");
        if (tokenStr != null && !tokenStr.isBlank()) {
            refreshTokenService.eliminar(tokenStr);
        }
        return ResponseEntity.noContent().build();
    }
}
