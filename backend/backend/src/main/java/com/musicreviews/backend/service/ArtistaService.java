package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.repository.ArtistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con los artistas.
@Service
public class ArtistaService {

    // Esto inyecta el repositorio de artistas para poder acceder a la base de datos.
    @Autowired
    private ArtistaRepository artistaRepository;

    // Esto devuelve todos los artistas disponibles en la aplicación.
    public List<Artista> obtenerTodos() {
        return artistaRepository.findAll();
    }

    // Esto busca un artista por su ID. Devuelve Optional porque puede no existir.
    public Optional<Artista> obtenerPorId(Long id) {
        return artistaRepository.findById(id);
    }

    // Esto busca artistas cuyo nombre contenga el texto recibido. Se usa para el buscador.
    public List<Artista> buscarPorNombre(String nombre) {
        return artistaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    // Esto guarda un artista nuevo en la base de datos.
    public Artista guardar(Artista artista) {
        return artistaRepository.save(artista);
    }

    // Esto actualiza todos los campos de un artista existente.
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

    // Esto elimina un artista por su ID. Lanza excepción si no existe.
    public void eliminar(Long id) {
        if (!artistaRepository.existsById(id)) {
            throw new RuntimeException("Artista no encontrado");
        }
        artistaRepository.deleteById(id);
    }
}
