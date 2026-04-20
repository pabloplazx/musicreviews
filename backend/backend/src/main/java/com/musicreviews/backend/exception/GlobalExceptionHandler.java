package com.musicreviews.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

// Centraliza el manejo de excepciones para todos los controllers.
// Devuelve siempre un JSON uniforme con timestamp, status y mensaje.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RecursoNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(404, e.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(ReglaNegocioException e) {
        return ResponseEntity.badRequest().body(errorBody(400, e.getMessage()));
    }

    private Map<String, Object> errorBody(int status, String mensaje) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "mensaje", mensaje
        );
    }
}
