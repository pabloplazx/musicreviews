package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.repository.ArtistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con los artistas.
@Service
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class ArtistaService {

    // final + @RequiredArgsConstructor reemplaza @Autowired. Los campos inmutables son más seguros y fáciles de testear.
    private final ArtistaRepository artistaRepository;

    // readOnly=true indica a Hibernate que no rastree cambios en esta consulta, mejorando el rendimiento.
    @Transactional(readOnly = true)
    public List<Artista> obtenerTodos() {
        return artistaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Artista> obtenerPorId(Long id) {
        return artistaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Artista> buscarPorNombre(String nombre) {
        return artistaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Transactional
    public Artista guardar(Artista artista) {
        return artistaRepository.save(artista);
    }

    @Transactional
    public Artista actualizar(Long id, Artista datosActualizados) {
        Artista artista = artistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artista no encontrado"));

        artista.setNombre(datosActualizados.getNombre());
        artista.setFoto(datosActualizados.getFoto());
        artista.setBiografia(datosActualizados.getBiografia());
        artista.setGenero(datosActualizados.getGenero());
        artista.setPais(datosActualizados.getPais());

        return artistaRepository.save(artista);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!artistaRepository.existsById(id)) {
            throw new RuntimeException("Artista no encontrado");
        }
        artistaRepository.deleteById(id);
    }
}
