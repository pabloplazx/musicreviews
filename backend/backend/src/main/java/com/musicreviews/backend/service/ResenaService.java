package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.repository.ResenaRepository;
import com.musicreviews.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con las reseñas.
// Verifica puntuación, evita duplicados y comprueba la propiedad del recurso
// en las operaciones de modificación (sólo el dueño o un ADMIN pueden modificar).
@Service
@RequiredArgsConstructor
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Resena> obtenerPorAlbum(Long albumId) {
        return resenaRepository.findByAlbumId(albumId);
    }

    @Transactional(readOnly = true)
    public List<Resena> obtenerPorUsuario(Long usuarioId) {
        return resenaRepository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public Optional<Resena> obtenerPorUsuarioYAlbum(Long usuarioId, Long albumId) {
        return resenaRepository.findByUsuarioIdAndAlbumId(usuarioId, albumId);
    }

    // Crear: además de validar puntuación y duplicado, comprobamos que el usuarioId
    // del body coincide con el del usuario autenticado. Sin esto, maría con su token
    // podría enviar `usuario.id = 3` y crear una reseña en nombre de carlos.
    // Un ADMIN sí puede crear reseñas en nombre de otro (tarea de moderación).
    @Transactional
    public Resena crear(Resena resena, String emailLlamante, boolean esAdmin) {
        validarPuntuacion(resena.getPuntuacion());

        if (!esAdmin) {
            Long idLlamante = idDelEmail(emailLlamante);
            if (!resena.getUsuario().getId().equals(idLlamante)) {
                throw new AccesoDenegadoException("Solo puedes crear reseñas en tu propio nombre");
            }
        }

        if (resenaRepository.existsByUsuarioIdAndAlbumId(
                resena.getUsuario().getId(), resena.getAlbum().getId())) {
            throw new ReglaNegocioException("El usuario ya ha reseñado este álbum");
        }

        Resena guardada = resenaRepository.save(resena);
        entityManager.refresh(guardada);
        return guardada;
    }

    @Transactional
    public Resena actualizar(Long id, Resena datosActualizados, String emailLlamante, boolean esAdmin) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada"));

        if (!esAdmin && !resena.getUsuario().getEmail().equals(emailLlamante)) {
            throw new AccesoDenegadoException("Solo puedes editar tus propias reseñas");
        }

        validarPuntuacion(datosActualizados.getPuntuacion());

        resena.setPuntuacion(datosActualizados.getPuntuacion());
        resena.setComentario(datosActualizados.getComentario());

        return resenaRepository.save(resena);
    }

    @Transactional
    public void eliminar(Long id, String emailLlamante, boolean esAdmin) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada"));

        if (!esAdmin && !resena.getUsuario().getEmail().equals(emailLlamante)) {
            throw new AccesoDenegadoException("Solo puedes borrar tus propias reseñas");
        }

        resenaRepository.deleteById(id);
    }

    private Long idDelEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AccesoDenegadoException("Sesión inválida"))
                .getId();
    }

    private void validarPuntuacion(Double puntuacion) {
        if (puntuacion < 0.5 || puntuacion > 5) {
            throw new ReglaNegocioException("La puntuación debe estar entre 0.5 y 5");
        }
    }
}
