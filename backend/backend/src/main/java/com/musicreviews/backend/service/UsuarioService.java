package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Esta clase contiene la lógica de negocio relacionada con los usuarios.
// Se encarga de validar que no haya duplicados de email o username antes de guardar.
@Service
@RequiredArgsConstructor // Genera el constructor con los campos final — inyección por constructor
public class UsuarioService {

    // final + @RequiredArgsConstructor reemplaza @Autowired. Los campos inmutables son más seguros y fáciles de testear.
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;

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

    // Crea un nuevo usuario y persiste su token de verificación vía JDBC directo,
    // evitando cualquier interferencia de Hibernate con ese campo.
    @Transactional
    public String registrar(String username, String email, String encodedPassword) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese email");
        }
        if (usuarioRepository.existsByUsername(username)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese username");
        }

        // Primero guardamos el usuario sin token (JPA lo inserta como NULL)
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(encodedPassword);
        usuario.setEmailVerificado(false);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        Long userId = guardado.getId();

        // Luego actualizamos el token vía JDBC puro — bypassa Hibernate completamente
        String token = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "UPDATE usuario SET token_verificacion = ? WHERE id = ?",
                token, userId
        );

        return token;
    }

    // Verifica que el usuario autenticado solo edita su propio perfil (o es ADMIN).
    // Sin esta comprobación, maría con su token podría editar el perfil de carlos.
    @Transactional
    public Usuario actualizar(Long id, Usuario datosActualizados, String emailLlamante, boolean esAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!esAdmin && !usuario.getEmail().equals(emailLlamante)) {
            throw new AccesoDenegadoException("Solo puedes editar tu propio perfil");
        }

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

    // Cambia el flag "activo" del usuario sin tocar el resto de campos.
    // Pensado para uso desde el panel de administración (PATCH /api/usuarios/{id}/activo).
    @Transactional
    public Usuario cambiarActivo(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setActivo(activo);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id, String emailLlamante, boolean esAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!esAdmin && !usuario.getEmail().equals(emailLlamante)) {
            throw new AccesoDenegadoException("Solo puedes borrar tu propia cuenta");
        }

        usuarioRepository.deleteById(id);
    }
}
