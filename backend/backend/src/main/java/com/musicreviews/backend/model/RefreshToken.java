package com.musicreviews.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Tabla de refresh tokens. Cada fila representa un token de larga duración
// vinculado a un usuario. Al hacer login se elimina el anterior y se crea uno nuevo.
@Entity
@Table(name = "refresh_token")
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
