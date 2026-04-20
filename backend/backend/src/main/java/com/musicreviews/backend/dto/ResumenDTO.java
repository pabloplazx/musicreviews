package com.musicreviews.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Esto contiene los totales globales de la aplicación: álbumes, artistas, reseñas y usuarios.
@Data
@AllArgsConstructor
public class ResumenDTO {
    // Total de álbumes en el catálogo.
    private long totalAlbumes;
    // Total de artistas registrados.
    private long totalArtistas;
    // Total de reseñas escritas por los usuarios.
    private long totalResenas;
    // Total de usuarios registrados en la app.
    private long totalUsuarios;
}
