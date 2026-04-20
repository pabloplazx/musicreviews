package com.musicreviews.backend.exception;

// Excepción lanzada cuando un recurso no existe en la base de datos. Se mapea a HTTP 404.
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
