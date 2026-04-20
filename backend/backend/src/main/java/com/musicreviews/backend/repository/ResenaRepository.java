package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Resena;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // Esto devuelve los álbumes con mejor puntuación media, agrupados por álbum.
    @Query("SELECT r.album, AVG(r.puntuacion) as avg FROM Resena r GROUP BY r.album ORDER BY avg DESC")
    List<Object[]> findTopAlbumesPorPuntuacion(Pageable pageable);

    // Esto devuelve los álbumes con más reseñas, agrupados por álbum.
    @Query("SELECT r.album, COUNT(r) as cnt FROM Resena r GROUP BY r.album ORDER BY cnt DESC")
    List<Object[]> findAlbumesMasResenados(Pageable pageable);

    // Esto devuelve los artistas con mejor puntuación media en sus álbumes.
    @Query("SELECT r.album.artista, AVG(r.puntuacion) as avg FROM Resena r GROUP BY r.album.artista ORDER BY avg DESC")
    List<Object[]> findTopArtistasPorPuntuacion(Pageable pageable);

    // Esto devuelve los álbumes mejor puntuados dentro de un género concreto.
    @Query("SELECT r.album, AVG(r.puntuacion) as avg FROM Resena r WHERE LOWER(r.album.genero) = LOWER(:genero) GROUP BY r.album ORDER BY avg DESC")
    List<Object[]> findTopAlbumesPorGenero(String genero, Pageable pageable);

    // Esto devuelve las últimas reseñas escritas, ordenadas por fecha descendente.
    List<Resena> findTop10ByOrderByFechaCreacionDesc();
}
