package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Esta interfaz gestiona el acceso a la tabla "favorito" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    // Esto carga un favorito por ID junto con su usuario y álbum completos,
    // evitando que Hibernate devuelva la instancia cacheada con campos nulos.
    @Query("SELECT f FROM Favorito f JOIN FETCH f.usuario JOIN FETCH f.album WHERE f.id = :id")
    Optional<Favorito> findByIdConRelaciones(@Param("id") Long id);

    // Esto devuelve todos los álbumes marcados como favoritos por un usuario. Se usa en su perfil.
    List<Favorito> findByUsuarioId(Long usuarioId);

    // Esto comprueba si un álbum ya está en favoritos de un usuario antes de añadirlo de nuevo.
    boolean existsByUsuarioIdAndAlbumId(Long usuarioId, Long albumId);

    // Esto elimina el favorito de un usuario sobre un álbum concreto (quitar de favoritos).
    // Necesita @Transactional en el servicio porque es una operación de escritura personalizada.
    void deleteByUsuarioIdAndAlbumId(Long usuarioId, Long albumId);
}
