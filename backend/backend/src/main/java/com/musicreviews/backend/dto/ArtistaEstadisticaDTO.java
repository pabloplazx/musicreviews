package com.musicreviews.backend.dto;

import com.musicreviews.backend.model.Artista;
import lombok.AllArgsConstructor;
import lombok.Data;

// Esto representa un artista junto a su puntuación media calculada a partir de las reseñas de sus álbumes.
@Data
@AllArgsConstructor
public class ArtistaEstadisticaDTO {
    // Artista con todos sus datos (nombre, foto, biografía, género, país).
    private Artista artista;
    // Puntuación media de todos los álbumes del artista que tienen al menos una reseña.
    private double puntuacionMedia;
}
