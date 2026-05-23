package com.musicreviews.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccesoDenegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(403, e.getMessage()));
    }

    // Maneja los errores de @Valid en los @RequestBody. Devuelve 400 con un mapa
    // campo → mensaje para que el frontend pueda señalizar cada input que ha fallado.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(
                err -> errores.put(err.getField(), err.getDefaultMessage())
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("mensaje", "Errores de validación");
        body.put("errores", errores);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(500, e.getMessage()));
    }

    private Map<String, Object> errorBody(int status, String mensaje) {
        return Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "mensaje", mensaje
        );
    }
}
