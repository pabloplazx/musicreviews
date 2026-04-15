package com.musicreviews.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

// Esta clase representa la tabla "album" en la base de datos.
// Solo el administrador puede añadir álbumes. Las portadas son URLs de la API de Spotify.
@Entity
@Table(name = "album")
@Data
public class Album {

    // Identificador único del álbum, generado automáticamente por la BD.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Título del álbum. Campo obligatorio.
    @Column(nullable = false, length = 150)
    private String titulo;

    // URL de la portada del álbum obtenida de la API de Spotify.
    private String portada;

    // Fecha de lanzamiento oficial del álbum.
    @Column(name = "fecha_lanzamiento")
    private LocalDate fechaLanzamiento;

    // Género musical del álbum (ej: Rock, Pop, Hip-Hop...).
    @Column(length = 50)
    private String genero;

    // Descripción o sinopsis del álbum.
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Esto representa la relación con el artista al que pertenece el álbum.
    // Se usa ManyToOne porque un artista puede tener muchos álbumes.
    @ManyToOne
    @JoinColumn(name = "artista_id")
    private Artista artista;
}
