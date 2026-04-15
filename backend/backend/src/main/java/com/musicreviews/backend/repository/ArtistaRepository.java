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
}
