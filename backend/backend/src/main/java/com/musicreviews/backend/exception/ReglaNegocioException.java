package com.musicreviews.backend.exception;

// Excepción lanzada cuando se viola una regla de negocio (duplicados, validaciones). Se mapea a HTTP 400.
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
