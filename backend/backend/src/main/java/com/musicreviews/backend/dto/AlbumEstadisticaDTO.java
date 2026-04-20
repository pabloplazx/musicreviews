package com.musicreviews.backend.dto;

import com.musicreviews.backend.model.Album;
import lombok.AllArgsConstructor;
import lombok.Data;

// Esto representa un álbum junto a un valor numérico de estadística (puntuación media o total de reseñas).
@Data
@AllArgsConstructor
public class AlbumEstadisticaDTO {
    // Álbum con todos sus datos, incluyendo el artista.
    private Album album;
    // Valor estadístico asociado: puntuación media (1.0-5.0) o número de reseñas según el contexto.
    private double valor;
}
