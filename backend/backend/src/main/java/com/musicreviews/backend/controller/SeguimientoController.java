package com.musicreviews.backend.controller;

import com.musicreviews.backend.dto.UsuarioResumenDTO;
import com.musicreviews.backend.service.SeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    // POST /api/usuarios/{id}/seguir — el usuario autenticado sigue al usuario {id}
    @PostMapping("/{id}/seguir")
    public ResponseEntity<Void> seguir(@PathVariable Long id, Authentication auth) {
        seguimientoService.seguir(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    // DELETE /api/usuarios/{id}/seguir — el usuario autenticado deja de seguir al usuario {id}
    @DeleteMapping("/{id}/seguir")
    public ResponseEntity<Void> dejarDeSeguir(@PathVariable Long id, Authentication auth) {
        seguimientoService.dejarDeSeguir(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // GET /api/usuarios/{id}/sigue — devuelve si el usuario autenticado sigue al usuario {id}
    @GetMapping("/{id}/sigue")
    public ResponseEntity<Map<String, Boolean>> estaSiguiendo(@PathVariable Long id, Authentication auth) {
        boolean sigue = seguimientoService.estaSiguiendo(id, auth.getName());
        return ResponseEntity.ok(Map.of("sigue", sigue));
    }

    // GET /api/usuarios/{id}/seguidores — lista pública de seguidores
    @GetMapping("/{id}/seguidores")
    public ResponseEntity<List<UsuarioResumenDTO>> getSeguidores(@PathVariable Long id) {
        return ResponseEntity.ok(seguimientoService.getSeguidores(id));
    }

    // GET /api/usuarios/{id}/siguiendo — lista pública de usuarios que sigue
    @GetMapping("/{id}/siguiendo")
    public ResponseEntity<List<UsuarioResumenDTO>> getSiguiendo(@PathVariable Long id) {
        return ResponseEntity.ok(seguimientoService.getSiguiendo(id));
    }

    // GET /api/usuarios/{id}/contadores — seguidores y siguiendo en una sola llamada
    @GetMapping("/{id}/contadores")
    public ResponseEntity<Map<String, Long>> getContadores(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "seguidores", seguimientoService.contarSeguidores(id),
                "siguiendo",  seguimientoService.contarSiguiendo(id)
        ));
    }
}
