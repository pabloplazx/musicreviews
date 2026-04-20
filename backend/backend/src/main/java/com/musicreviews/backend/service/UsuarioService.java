package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Esta clase contiene la lógica de negocio relacionada con los usuarios.
// Se encarga de validar que no haya duplicados de email o username antes de guardar.
@Service
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class UsuarioService {

    // final + @RequiredArgsConstructor reemplaza @Autowired. Los campos inmutables son más seguros y fáciles de testear.
    private final UsuarioRepository usuarioRepository;

    // readOnly=true indica a Hibernate que no rastree cambios en esta consulta, mejorando el rendimiento.
    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ReglaNegocioException("Ya existe un usuario con ese email");
        }
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new ReglaNegocioException("Ya existe un usuario con ese username");
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Long id, Usuario datosActualizados) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        usuario.setUsername(datosActualizados.getUsername());
        usuario.setFotoPerfil(datosActualizados.getFotoPerfil());
        usuario.setBio(datosActualizados.getBio());

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void actualizarUltimoLogin(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setFechaUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}
