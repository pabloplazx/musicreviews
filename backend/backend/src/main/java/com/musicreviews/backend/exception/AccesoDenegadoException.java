package com.musicreviews.backend.exception;

// Excepción de negocio para casos en los que un usuario autenticado intenta
// modificar un recurso que no le pertenece (y no es ADMIN).
// El GlobalExceptionHandler la mapea a HTTP 403 con JSON uniforme.
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
