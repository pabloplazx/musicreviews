package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con los álbumes.
// Solo el administrador puede crear o modificar álbumes, controlado por Spring Security.
@Service
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class AlbumService {

    // final + @RequiredArgsConstructor reemplaza @Autowired. Los campos inmutables son más seguros y fáciles de testear.
    private final AlbumRepository albumRepository;

    // readOnly=true indica a Hibernate que no rastree cambios en esta consulta, mejorando el rendimiento.
    @Transactional(readOnly = true)
    public List<Album> obtenerTodos() {
        return albumRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Album> obtenerPorId(Long id) {
        return albumRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Album> buscarPorTitulo(String titulo) {
        return albumRepository.findByTituloContainingIgnoreCase(titulo);
    }

    @Transactional(readOnly = true)
    public List<Album> obtenerPorArtista(Long artistaId) {
        return albumRepository.findByArtistaId(artistaId);
    }

    @Transactional(readOnly = true)
    public List<Album> obtenerPorGenero(String genero) {
        return albumRepository.findByGeneroIgnoreCase(genero);
    }

    @Transactional
    public Album guardar(Album album) {
        return albumRepository.save(album);
    }

    @Transactional
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

    @Transactional
    public void eliminar(Long id) {
        if (!albumRepository.existsById(id)) {
            throw new RuntimeException("Álbum no encontrado");
        }
        albumRepository.deleteById(id);
    }
}
