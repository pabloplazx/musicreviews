package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.repository.ResenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con las reseñas.
// Se encarga de validar la puntuación y de impedir reseñas duplicadas.
@Service
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class ResenaService {

    // final + @RequiredArgsConstructor reemplaza @Autowired. Los campos inmutables son más seguros y fáciles de testear.
    private final ResenaRepository resenaRepository;

    // readOnly=true indica a Hibernate que no rastree cambios en esta consulta, mejorando el rendimiento.
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

    @Transactional
    public Resena crear(Resena resena) {
        validarPuntuacion(resena.getPuntuacion());

        if (resenaRepository.existsByUsuarioIdAndAlbumId(
                resena.getUsuario().getId(), resena.getAlbum().getId())) {
            throw new ReglaNegocioException("El usuario ya ha reseñado este álbum");
        }

        // save() devuelve la entidad guardada con el id asignado — el findById posterior era redundante.
        return resenaRepository.save(resena);
    }

    @Transactional
    public Resena actualizar(Long id, Resena datosActualizados) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada"));

        validarPuntuacion(datosActualizados.getPuntuacion());

        resena.setPuntuacion(datosActualizados.getPuntuacion());
        resena.setComentario(datosActualizados.getComentario());

        // save() devuelve la entidad actualizada — el findById posterior era redundante.
        return resenaRepository.save(resena);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!resenaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Reseña no encontrada");
        }
        resenaRepository.deleteById(id);
    }

    private void validarPuntuacion(Double puntuacion) {
        if (puntuacion < 0.5 || puntuacion > 5) {
            throw new ReglaNegocioException("La puntuación debe estar entre 0.5 y 5");
        }
    }
}
