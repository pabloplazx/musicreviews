package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Esta interfaz gestiona el acceso a la tabla "album" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    // Esto busca álbumes cuyo título contenga el texto indicado, sin distinguir mayúsculas/minúsculas.
    // Se usa para el buscador de álbumes en la app.
    List<Album> findByTituloContainingIgnoreCase(String titulo);

    // Esto devuelve todos los álbumes de un artista concreto, identificado por su ID.
    List<Album> findByArtistaId(Long artistaId);

    // Esto filtra álbumes por género musical, sin distinguir mayúsculas/minúsculas.
    List<Album> findByGeneroIgnoreCase(String genero);
}
