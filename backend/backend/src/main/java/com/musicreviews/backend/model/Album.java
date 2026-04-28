package com.musicreviews.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

// Esta clase representa la tabla "album" en la base de datos.
// Solo el administrador puede añadir álbumes. Las portadas son URLs de la API de Spotify.
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    // EAGER: el artista se carga siempre junto al álbum porque es necesario para mostrarlo.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artista_id", nullable = false)
    private Artista artista;
}
