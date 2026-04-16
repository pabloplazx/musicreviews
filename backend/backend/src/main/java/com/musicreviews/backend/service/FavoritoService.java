package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.repository.FavoritoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Esta clase contiene la lógica de negocio relacionada con los favoritos.
// Se encarga de que un usuario no pueda añadir el mismo álbum dos veces.
@Service
public class FavoritoService {

    // Esto inyecta el repositorio de favoritos para poder acceder a la base de datos.
    @Autowired
    private FavoritoRepository favoritoRepository;

    // Esto permite refrescar entidades desde la BD para obtener los datos completos tras un save.
    @Autowired
    private EntityManager entityManager;

    // Esto devuelve todos los álbumes marcados como favoritos por un usuario.
    public List<Favorito> obtenerPorUsuario(Long usuarioId) {
        return favoritoRepository.findByUsuarioId(usuarioId);
    }

    // Esto comprueba si un álbum ya está en la lista de favoritos de un usuario.
    public boolean esFavorito(Long usuarioId, Long albumId) {
        return favoritoRepository.existsByUsuarioIdAndAlbumId(usuarioId, albumId);
    }

    // Esto añade un álbum a favoritos. Antes comprueba que no esté ya en la lista.
    // Usa @Transactional para que refresh() pueda recargar las relaciones desde la BD.
    @Transactional
    public Favorito agregar(Favorito favorito) {
        if (favoritoRepository.existsByUsuarioIdAndAlbumId(
                favorito.getUsuario().getId(), favorito.getAlbum().getId())) {
            throw new RuntimeException("El álbum ya está en favoritos");
        }
        Favorito guardado = favoritoRepository.save(favorito);
        entityManager.refresh(guardado);
        return guardado;
    }

    // Esto elimina un favorito concreto. Usa @Transactional porque deleteBy... es una operación
    // de escritura personalizada que JPA necesita ejecutar dentro de una transacción.
    @Transactional
    public void eliminar(Long usuarioId, Long albumId) {
        if (!favoritoRepository.existsByUsuarioIdAndAlbumId(usuarioId, albumId)) {
            throw new RuntimeException("El álbum no está en favoritos");
        }
        favoritoRepository.deleteByUsuarioIdAndAlbumId(usuarioId, albumId);
    }
}
