package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Esta interfaz gestiona el acceso a la tabla "favorito" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    // Esto devuelve todos los álbumes marcados como favoritos por un usuario. Se usa en su perfil.
    List<Favorito> findByUsuarioId(Long usuarioId);

    // Esto comprueba si un álbum ya está en favoritos de un usuario antes de añadirlo de nuevo.
    boolean existsByUsuarioIdAndAlbumId(Long usuarioId, Long albumId);

    // Esto elimina el favorito de un usuario sobre un álbum concreto (quitar de favoritos).
    // Necesita @Transactional en el servicio porque es una operación de escritura personalizada.
    void deleteByUsuarioIdAndAlbumId(Long usuarioId, Long albumId);
}
