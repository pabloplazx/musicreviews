package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Esta interfaz gestiona el acceso a la tabla "resena" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    // Esto devuelve todas las reseñas de un álbum concreto. Se usa para mostrarlas en la ficha del álbum.
    List<Resena> findByAlbumId(Long albumId);

    // Esto devuelve todas las reseñas escritas por un usuario. Se usa en el perfil del usuario.
    List<Resena> findByUsuarioId(Long usuarioId);

    // Esto busca la reseña específica de un usuario sobre un álbum concreto.
    Optional<Resena> findByUsuarioIdAndAlbumId(Long usuarioId, Long albumId);

    // Esto comprueba si un usuario ya ha reseñado un álbum antes de permitirle crear una nueva reseña.
    boolean existsByUsuarioIdAndAlbumId(Long usuarioId, Long albumId);
}
