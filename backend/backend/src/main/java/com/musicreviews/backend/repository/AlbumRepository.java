package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

// Esta interfaz gestiona el acceso a la tabla "album" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    // Esto busca álbumes cuyo título contenga el texto indicado, sin distinguir mayúsculas/minúsculas.
    // Se usa para el buscador de álbumes en la app.
    Page<Album> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    // Esto devuelve todos los álbumes de un artista concreto, identificado por su ID.
    Page<Album> findByArtistaId(Long artistaId, Pageable pageable);

    // Esto devuelve la lista completa de álbumes de un artista — usado internamente por SpotifyService.
    List<Album> findByArtistaId(Long artistaId);

    // Esto filtra álbumes por género musical, sin distinguir mayúsculas/minúsculas.
    Page<Album> findByGeneroIgnoreCase(String genero, Pageable pageable);

    // Esto devuelve los últimos 10 álbumes añadidos al catálogo.
    List<Album> findTop10ByOrderByIdDesc();

    // Esto devuelve la distribución de álbumes agrupados por género.
    @Query("SELECT a.genero, COUNT(a) as cnt FROM Album a GROUP BY a.genero ORDER BY cnt DESC")
    List<Object[]> findDistribucionPorGenero();
}
