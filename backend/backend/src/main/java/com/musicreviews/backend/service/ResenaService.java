package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Resena;
import com.musicreviews.backend.repository.ResenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con las reseñas.
// Se encarga de validar la puntuación y de impedir reseñas duplicadas.
@Service
public class ResenaService {

    // Esto inyecta el repositorio de reseñas para poder acceder a la base de datos.
    @Autowired
    private ResenaRepository resenaRepository;

    // Esto devuelve todas las reseñas de un álbum concreto. Se usa en la ficha del álbum.
    public List<Resena> obtenerPorAlbum(Long albumId) {
        return resenaRepository.findByAlbumId(albumId);
    }

    // Esto devuelve todas las reseñas escritas por un usuario. Se usa en su perfil.
    public List<Resena> obtenerPorUsuario(Long usuarioId) {
        return resenaRepository.findByUsuarioId(usuarioId);
    }

    // Esto busca la reseña de un usuario sobre un álbum concreto.
    public Optional<Resena> obtenerPorUsuarioYAlbum(Long usuarioId, Long albumId) {
        return resenaRepository.findByUsuarioIdAndAlbumId(usuarioId, albumId);
    }

    // Esto crea una reseña nueva. Valida que la puntuación esté entre 1 y 5
    // y que el usuario no haya reseñado ya ese álbum.
    public Resena crear(Resena resena) {
        validarPuntuacion(resena.getPuntuacion());

        if (resenaRepository.existsByUsuarioIdAndAlbumId(
                resena.getUsuario().getId(), resena.getAlbum().getId())) {
            throw new RuntimeException("El usuario ya ha reseñado este álbum");
        }

        Resena guardada = resenaRepository.save(resena);
        return resenaRepository.findById(guardada.getId()).orElse(guardada);
    }

    // Esto actualiza la puntuación y el comentario de una reseña existente.
    // Vuelve a validar la puntuación por si el usuario envía un valor incorrecto.
    public Resena actualizar(Long id, Resena datosActualizados) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));

        validarPuntuacion(datosActualizados.getPuntuacion());

        resena.setPuntuacion(datosActualizados.getPuntuacion());
        resena.setComentario(datosActualizados.getComentario());

        resenaRepository.save(resena);
        return resenaRepository.findById(id).orElse(resena);
    }

    // Esto elimina una reseña por su ID. Lanza excepción si no existe.
    public void eliminar(Long id) {
        if (!resenaRepository.existsById(id)) {
            throw new RuntimeException("Reseña no encontrada");
        }
        resenaRepository.deleteById(id);
    }

    // Esto valida que la puntuación esté entre 1 y 5. Se llama internamente antes de crear o actualizar.
    private void validarPuntuacion(int puntuacion) {
        if (puntuacion < 1 || puntuacion > 5) {
            throw new RuntimeException("La puntuación debe estar entre 1 y 5");
        }
    }
}
