package com.musicreviews.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Datos que llegan en el body de POST /api/auth/reset-password.
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres, un número y un carácter especial"
    )
    private String nuevaPassword;
}
