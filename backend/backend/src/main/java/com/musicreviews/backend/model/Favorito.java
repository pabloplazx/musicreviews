package com.musicreviews.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Esta clase representa la tabla "favorito" en la base de datos.
// Guarda la relación entre un usuario y un álbum que ha marcado como favorito.
// Un usuario no puede añadir el mismo álbum dos veces, controlado con la restricción UNIQUE.
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "favorito", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "album_id"})
})
@Data
public class Favorito {

    // Identificador único del favorito, generado automáticamente por la BD.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: el usuario solo se carga cuando se accede al campo, no al cargar el favorito.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // LAZY: el álbum solo se carga cuando se accede al campo, no al cargar el favorito.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    // Fecha y hora en que el usuario añadió el álbum a favoritos. Se asigna automáticamente.
    @Column(name = "fecha_agregado")
    private LocalDateTime fechaAgregado;

    // Antes de guardar por primera vez, esto asigna la fecha en que se agrega a favoritos.
    @PrePersist
    public void prePersist() {
        fechaAgregado = LocalDateTime.now();
    }
}
