package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.RefreshToken;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.RefreshTokenRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${refresh-token.expiration-days:7}")
    private int expirationDays;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;

    // Elimina el refresh token anterior del usuario (si lo hay) y crea uno nuevo.
    @Transactional
    public RefreshToken crear(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        refreshTokenRepository.deleteByUsuarioId(usuarioId);

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUsuario(usuario);
        rt.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        return refreshTokenRepository.save(rt);
    }

    // Valida el token: existe y no ha expirado. Si ha expirado lo elimina y lanza error.
    public RefreshToken verificar(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ReglaNegocioException("Token de refresco inválido"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(rt);
            throw new ReglaNegocioException("Sesión expirada. Por favor, inicia sesión de nuevo");
        }
        return rt;
    }

    @Transactional
    public void eliminar(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}
