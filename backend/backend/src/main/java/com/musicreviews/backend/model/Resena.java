package com.musicreviews.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Esta clase representa la tabla "resena" en la base de datos.
// Un usuario solo puede tener una reseña por álbum, controlado con la restricción UNIQUE.
@Entity
@Table(name = "resena", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "album_id"})
})
@Data
public class Resena {

    // Identificador único de la reseña, generado automáticamente por la BD.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: el usuario solo se carga cuando se accede al campo, no al cargar la reseña.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // LAZY: el álbum solo se carga cuando se accede al campo, no al cargar la reseña.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    // Puntuación del 1 al 5. La validación del rango se hace en la capa de servicio.
    @Column(nullable = false)
    private int puntuacion;

    // Texto de la reseña escrito por el usuario. Puede ser largo, por eso se usa TEXT.
    @Column(name = "contenido", columnDefinition = "TEXT")
    private String comentario;

    // Fecha y hora en que se creó la reseña. Se asigna automáticamente en prePersist.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Fecha y hora de la última edición. Se actualiza automáticamente en preUpdate.
    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;

    // Antes de guardar por primera vez, esto asigna la fecha de creación.
    @PrePersist
    public void prePersist() {
        fechaCreacion = LocalDateTime.now();
    }

    // Cada vez que se actualiza la reseña, esto guarda la fecha de la última edición.
    @PreUpdate
    public void preUpdate() {
        fechaEdicion = LocalDateTime.now();
    }
}
