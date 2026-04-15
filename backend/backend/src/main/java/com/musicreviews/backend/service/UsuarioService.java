package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con los usuarios.
// Se encarga de validar que no haya duplicados de email o username antes de guardar.
@Service
public class UsuarioService {

    // Esto inyecta el repositorio de usuarios para poder acceder a la base de datos.
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Esto devuelve todos los usuarios registrados en la aplicación.
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    // Esto busca un usuario por su ID. Devuelve Optional porque puede no existir.
    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    // Esto busca un usuario por su email. Se usa principalmente durante el login.
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // Esto busca un usuario por su username. Se usa para mostrar perfiles públicos.
    public Optional<Usuario> obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Esto registra un usuario nuevo. Antes comprueba que el email y el username no estén ya en uso.
    public Usuario guardar(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new RuntimeException("Ya existe un usuario con ese username");
        }
        return usuarioRepository.save(usuario);
    }

    // Esto actualiza los datos del perfil de un usuario: username, foto de perfil y bio.
    // No permite cambiar el email ni la contraseña desde aquí.
    public Usuario actualizar(Long id, Usuario datosActualizados) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setUsername(datosActualizados.getUsername());
        usuario.setFotoPerfil(datosActualizados.getFotoPerfil());
        usuario.setBio(datosActualizados.getBio());

        return usuarioRepository.save(usuario);
    }

    // Esto elimina un usuario por su ID. Lanza excepción si no existe.
    public void eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}
