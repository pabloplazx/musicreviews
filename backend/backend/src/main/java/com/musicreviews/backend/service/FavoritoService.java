package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Favorito;
import com.musicreviews.backend.repository.FavoritoRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Esta clase contiene la lógica de negocio relacionada con los favoritos.
// Se encarga de que un usuario no pueda añadir el mismo álbum dos veces.
@Service
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class FavoritoService {

    // final + @RequiredArgsConstructor reemplaza @Autowired. Los campos inmutables son más seguros y fáciles de testear.
    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;

    // Necesario para forzar la recarga de relaciones LAZY tras un save() dentro de la misma transacción.
    private final EntityManager entityManager;

    // readOnly=true indica a Hibernate que no rastree cambios en esta consulta, mejorando el rendimiento.
    @Transactional(readOnly = true)
    public List<Favorito> obtenerPorUsuario(Long usuarioId) {
        return favoritoRepository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public boolean esFavorito(Long usuarioId, Long albumId) {
        return favoritoRepository.existsByUsuarioIdAndAlbumId(usuarioId, albumId);
    }

    // @Transactional permite que entityManager.refresh() recargue las relaciones LAZY desde la BD
    // tras el save(), evitando que Spring devuelva la instancia cacheada con campos nulos.
    // Verifica que el usuario solo añade favoritos a su propia lista (o es ADMIN).
    @Transactional
    public Favorito agregar(Favorito favorito, String emailLlamante, boolean esAdmin) {
        if (!esAdmin) {
            Long idLlamante = idDelEmail(emailLlamante);
            if (!favorito.getUsuario().getId().equals(idLlamante)) {
                throw new AccesoDenegadoException("Solo puedes gestionar tus propios favoritos");
            }
        }

        if (favoritoRepository.existsByUsuarioIdAndAlbumId(
                favorito.getUsuario().getId(), favorito.getAlbum().getId())) {
            throw new ReglaNegocioException("El álbum ya está en favoritos");
        }
        Favorito guardado = favoritoRepository.save(favorito);
        entityManager.refresh(guardado);
        return guardado;
    }

    // @Transactional necesario porque deleteBy... es una operación de escritura personalizada de JPA.
    // Verifica que el usuario solo borra de su propia lista (o es ADMIN).
    @Transactional
    public void eliminar(Long usuarioId, Long albumId, String emailLlamante, boolean esAdmin) {
        if (!esAdmin) {
            Long idLlamante = idDelEmail(emailLlamante);
            if (!usuarioId.equals(idLlamante)) {
                throw new AccesoDenegadoException("Solo puedes gestionar tus propios favoritos");
            }
        }

        if (!favoritoRepository.existsByUsuarioIdAndAlbumId(usuarioId, albumId)) {
            throw new RecursoNoEncontradoException("El álbum no está en favoritos");
        }
        favoritoRepository.deleteByUsuarioIdAndAlbumId(usuarioId, albumId);
    }

    private Long idDelEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AccesoDenegadoException("Sesión inválida"))
                .getId();
    }
}
