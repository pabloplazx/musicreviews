package com.musicreviews.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Esta clase representa la tabla "usuario" en la base de datos.
// Cada instancia de esta clase es un usuario registrado en la aplicación.
@Entity
@Table(name = "usuario")
@Data
public class Usuario {

    // Identificador único del usuario, generado automáticamente por la BD.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre de usuario visible en la app. No puede repetirse entre usuarios.
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Email del usuario. Se usa para el login y no puede repetirse.
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Contraseña del usuario. Se guardará encriptada con BCrypt antes de persistir.
    @Column(nullable = false)
    private String password;

    // Ruta local al archivo de foto de perfil subido por el usuario (carpeta uploads/).
    @Column(name = "foto_perfil")
    private String fotoPerfil;

    // Descripción corta del perfil del usuario.
    @Column(columnDefinition = "TEXT")
    private String bio;

    // Fecha y hora en que el usuario se registró. Se asigna automáticamente en prePersist.
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // Rol del usuario dentro de la app. Por defecto es USER; ADMIN tiene permisos extra.
    @Enumerated(EnumType.STRING)
    private Rol rol;

    // Indica si la cuenta está activa. Permite desactivar usuarios sin borrarlos.
    @Column(nullable = false)
    private boolean activo = true;

    // Fecha y hora del último login. Se actualiza cada vez que el usuario inicia sesión.
    @Column(name = "fecha_ultimo_login")
    private LocalDateTime fechaUltimoLogin;

    // Antes de guardar por primera vez, esto asigna la fecha de registro y el rol por defecto.
    @PrePersist
    public void prePersist() {
        fechaRegistro = LocalDateTime.now();
        if (rol == null) rol = Rol.USER;
    }

    // Esto define los dos roles posibles dentro de la propia entidad.
    public enum Rol {
        USER, ADMIN
    }
}
