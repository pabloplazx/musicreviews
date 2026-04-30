package com.musicreviews.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Datos que llegan en el body de POST /api/auth/login.
// Email y password obligatorios; el formato del email se valida antes de llegar al service.
@Data
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
