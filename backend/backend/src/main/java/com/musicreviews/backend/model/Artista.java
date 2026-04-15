package com.musicreviews.backend.model;

import jakarta.persistence.*;
import lombok.Data;

// Esta clase representa la tabla "artista" en la base de datos.
// Cada instancia es un artista musical cuyos álbumes pueden aparecer en la app.
@Entity
@Table(name = "artista")
@Data
public class Artista {

    // Identificador único del artista, generado automáticamente por la BD.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del artista o grupo musical. Campo obligatorio.
    @Column(nullable = false, length = 100)
    private String nombre;

    // URL de la imagen del artista, normalmente obtenida de la API de Spotify.
    private String foto;

    // Texto largo con la biografía del artista.
    @Column(columnDefinition = "TEXT")
    private String biografia;

    // Género musical principal del artista (ej: Rock, Pop, Jazz...).
    @Column(length = 50)
    private String genero;

    // País de origen del artista.
    @Column(length = 50)
    private String pais;
}
