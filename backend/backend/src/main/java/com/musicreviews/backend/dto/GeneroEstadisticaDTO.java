package com.musicreviews.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Esto representa un género musical junto al número de álbumes que tiene en el catálogo.
@Data
@AllArgsConstructor
public class GeneroEstadisticaDTO {
    // Nombre del género musical (ej: hip-hop, rock, pop).
    private String genero;
    // Número total de álbumes de ese género en el catálogo.
    private long total;
}
