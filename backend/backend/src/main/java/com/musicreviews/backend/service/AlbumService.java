package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.repository.AlbumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con los álbumes.
// Solo el administrador puede crear o modificar álbumes; esto se controlará en el controlador con JWT.
@Service
public class AlbumService {

    // Esto inyecta el repositorio de álbumes para poder acceder a la base de datos.
    @Autowired
    private AlbumRepository albumRepository;

    // Esto devuelve todos los álbumes disponibles en la aplicación.
    public List<Album> obtenerTodos() {
        return albumRepository.findAll();
    }

    // Esto busca un álbum por su ID. Devuelve Optional porque puede no existir.
    public Optional<Album> obtenerPorId(Long id) {
        return albumRepository.findById(id);
    }

    // Esto busca álbumes cuyo título contenga el texto recibido. Se usa para el buscador.
    public List<Album> buscarPorTitulo(String titulo) {
        return albumRepository.findByTituloContainingIgnoreCase(titulo);
    }

    // Esto devuelve todos los álbumes de un artista concreto. Se usa en la ficha del artista.
    public List<Album> obtenerPorArtista(Long artistaId) {
        return albumRepository.findByArtistaId(artistaId);
    }

    // Esto filtra álbumes por género musical.
    public List<Album> obtenerPorGenero(String genero) {
        return albumRepository.findByGeneroIgnoreCase(genero);
    }

    // Esto guarda un álbum nuevo en la base de datos.
    public Album guardar(Album album) {
        return albumRepository.save(album);
    }

    // Esto actualiza todos los campos de un álbum existente.
    public Album actualizar(Long id, Album datosActualizados) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Álbum no encontrado"));

        album.setTitulo(datosActualizados.getTitulo());
        album.setPortada(datosActualizados.getPortada());
        album.setFechaLanzamiento(datosActualizados.getFechaLanzamiento());
        album.setGenero(datosActualizados.getGenero());
        album.setDescripcion(datosActualizados.getDescripcion());
        album.setArtista(datosActualizados.getArtista());

        return albumRepository.save(album);
    }

    // Esto elimina un álbum por su ID. Lanza excepción si no existe.
    public void eliminar(Long id) {
        if (!albumRepository.existsById(id)) {
            throw new RuntimeException("Álbum no encontrado");
        }
        albumRepository.deleteById(id);
    }
}
