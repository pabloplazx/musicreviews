package com.musicreviews.backend.repository;

import com.musicreviews.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Esta interfaz gestiona el acceso a la tabla "usuario" en la base de datos.
// Esto extiende JpaRepository para obtener las operaciones CRUD básicas de forma gratuita.
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Esto busca un usuario por su email. Se usa principalmente en el proceso de login.
    Optional<Usuario> findByEmail(String email);

    // Esto busca un usuario por su username. Útil para mostrar perfiles públicos.
    Optional<Usuario> findByUsername(String username);

    // Esto comprueba si ya existe un usuario con ese email antes de registrar uno nuevo.
    boolean existsByEmail(String email);

    // Esto comprueba si ya existe un usuario con ese username antes de registrar uno nuevo.
    boolean existsByUsername(String username);

    // Busca un usuario por el token de verificación de email generado en el registro.
    Optional<Usuario> findByTokenVerificacion(String tokenVerificacion);

    // UPDATE directo — garantiza que token_verificacion llegue a la BD independientemente del estado JPA.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE usuario SET token_verificacion = :token WHERE id = :id", nativeQuery = true)
    void setTokenVerificacion(@Param("id") Long id, @Param("token") String token);

    // Busca un usuario por el token de restablecimiento de contraseña.
    Optional<Usuario> findByTokenRestablecimiento(String tokenRestablecimiento);
}
