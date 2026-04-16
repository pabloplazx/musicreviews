package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Artista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Esta interfaz gestiona el acceso a la tabla "artista" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface ArtistaRepository extends JpaRepository<Artista, Long> {

    // Esto busca artistas cuyo nombre contenga el texto indicado, sin distinguir mayúsculas/minúsculas.
    // Se usa para el buscador de artistas en la app.
    List<Artista> findByNombreContainingIgnoreCase(String nombre);

    // Esto comprueba si ya existe un artista con ese nombre exacto en la BD.
    // Se usa al importar desde Spotify para evitar duplicados.
    boolean existsByNombreIgnoreCase(String nombre);

    // Esto busca un artista por nombre exacto (sin distinguir mayúsculas).
    // Se usa al reimportar artistas que existen pero no tienen álbumes.
    java.util.Optional<Artista> findByNombreIgnoreCase(String nombre);
}
