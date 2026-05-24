package com.musicreviews.backend.service;

import com.musicreviews.backend.exception.AccesoDenegadoException;
import com.musicreviews.backend.exception.RecursoNoEncontradoException;
import com.musicreviews.backend.exception.ReglaNegocioException;
import com.musicreviews.backend.model.Usuario;
import com.musicreviews.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

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

    // Genera un token UUID de reset, lo persiste vía JDBC puro y devuelve el token.
    // Caduca en 30 minutos — tiempo suficiente sin dejar la ventana abierta.
    @Transactional
    public String generarTokenReset(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ReglaNegocioException("No existe ninguna cuenta con ese email"));

        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("Esta cuenta está desactivada");
        }

        String token = UUID.randomUUID().toString();

        jdbcTemplate.update(
                "UPDATE usuario SET token_restablecimiento = ?, fecha_expiracion_reset = ? WHERE id = ?",
                token, LocalDateTime.now().plusMinutes(30), usuario.getId()
        );

        return token;
    }

    // Valida el token, comprueba que la nueva contraseña es distinta y actualiza.
    // Recibe la contraseña en crudo para poder comparar con el hash almacenado.
    @Transactional
    public void restablecerPassword(String token, String nuevaPasswordRaw) {
        Usuario usuario = usuarioRepository.findByTokenRestablecimiento(token)
                .orElseThrow(() -> new ReglaNegocioException("Enlace de restablecimiento inválido o ya utilizado"));

        if (usuario.getFechaExpiracionReset() == null ||
                LocalDateTime.now().isAfter(usuario.getFechaExpiracionReset())) {
            throw new ReglaNegocioException("El enlace ha expirado. Solicita uno nuevo.");
        }

        if (passwordEncoder.matches(nuevaPasswordRaw, usuario.getPassword())) {
            throw new ReglaNegocioException("La nueva contraseña no puede ser igual a la actual");
        }

        jdbcTemplate.update(
                "UPDATE usuario SET password = ?, token_restablecimiento = NULL, fecha_expiracion_reset = NULL WHERE id = ?",
                passwordEncoder.encode(nuevaPasswordRaw), usuario.getId()
        );
    }

    // Guarda la imagen en disco, borra la anterior si era local y actualiza foto_perfil en BD.
    public String guardarFotoPerfil(Long id, MultipartFile file) {
        try {
            String extension = obtenerExtension(file.getOriginalFilename());
            String nombreArchivo = id + "_" + UUID.randomUUID() + "." + extension;

            Path dirFotos = Paths.get(uploadDir, "fotos");
            Files.createDirectories(dirFotos);

            // Borrar foto anterior si era un archivo local subido
            Usuario usuario = usuarioRepository.findById(id).orElseThrow();
            if (usuario.getFotoPerfil() != null && usuario.getFotoPerfil().startsWith("/uploads/fotos/")) {
                String nombreAnterior = usuario.getFotoPerfil().substring("/uploads/fotos/".length());
                Files.deleteIfExists(dirFotos.resolve(nombreAnterior));
            }

            Files.copy(file.getInputStream(), dirFotos.resolve(nombreArchivo));

            String urlFoto = "/uploads/fotos/" + nombreArchivo;
            jdbcTemplate.update("UPDATE usuario SET foto_perfil = ? WHERE id = ?", urlFoto, id);
            return urlFoto;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage());
        }
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
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
