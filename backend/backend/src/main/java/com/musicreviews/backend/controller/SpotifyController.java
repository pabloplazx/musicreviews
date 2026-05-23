package com.musicreviews.backend.controller;

import com.musicreviews.backend.service.SpotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Este controlador expone el endpoint para importar artistas y álbumes desde Spotify.
// Se usa para poblar la base de datos con datos reales antes de arrancar el frontend.
@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    // Esto inyecta el servicio de Spotify para delegar en él la lógica de importación.
    @Autowired
    private SpotifyService spotifyService;

    // GET /api/spotify/importar?artista=Radiohead
    // → esto busca el artista en Spotify y guarda en la BD el artista y sus álbumes.
    // Devuelve un mensaje con el resultado de la importación.
    @GetMapping("/importar")
    public ResponseEntity<String> importar(@RequestParam String artista) {
        try {
            String resultado = spotifyService.importarArtista(artista);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al importar: " + e.getMessage());
        }
    }

    // GET /api/spotify/importar-playlist?id=4aJVWA1dJA3S4uh0tH0XjT
    // → esto importa todos los artistas únicos de una playlist pública de Spotify
    // junto con sus álbumes de estudio. Puede tardar varios minutos si hay muchos artistas.
    @GetMapping("/importar-playlist")
    public ResponseEntity<String> importarPlaylist(@RequestParam String id) {
        try {
            String resultado = spotifyService.importarDesdePlaylist(id);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al importar playlist: " + e.getMessage());
        }
    }

    // GET /api/spotify/importar-lista
    // → importa una lista curada de artistas relevantes (favoritos del usuario + clásicos universales).
    // Puede tardar varios minutos. No requiere parámetros.
    @GetMapping("/importar-lista")
    public ResponseEntity<String> importarLista() {
        try {
            String resultado = spotifyService.importarLista();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al importar lista: " + e.getMessage());
        }
    }

    // GET /api/spotify/completar-todos
    // → recorre todos los artistas de la BD, busca cada uno en Spotify y añade los
    // álbumes que faltasen. Útil tras añadir paginación para recuperar álbumes que
    // la importación original dejó fuera. Tarda varios minutos.
    @GetMapping("/completar-todos")
    public ResponseEntity<String> completarTodos() {
        try {
            String resultado = spotifyService.completarTodos();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al completar todos: " + e.getMessage());
        }
    }

    // GET /api/spotify/comprobar?artista=Radiohead
    // → compara los álbumes que hay en la BD de ese artista con los que Spotify lista
    // y devuelve los que faltan por importar. Endpoint de diagnóstico, no modifica datos.
    @GetMapping("/comprobar")
    public ResponseEntity<?> comprobar(@RequestParam String artista) {
        try {
            return ResponseEntity.ok(spotifyService.comprobarArtista(artista));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al comprobar: " + e.getMessage());
        }
    }

    // GET /api/spotify/actualizar-metadatos
    // → actualiza el género (Spotify) y la biografía (Last.fm) de todos los artistas
    // que tengan esos campos vacíos. También actualiza el género de sus álbumes.
    @GetMapping("/actualizar-metadatos")
    public ResponseEntity<String> actualizarMetadatos() {
        try {
            String resultado = spotifyService.actualizarMetadatos();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar metadatos: " + e.getMessage());
        }
    }

    // GET /api/spotify/actualizar-portadas
    // → busca en Spotify la portada de cada álbum que la tenga vacía y la actualiza en la BD.
    @GetMapping("/actualizar-portadas")
    public ResponseEntity<String> actualizarPortadas() {
        try {
            String resultado = spotifyService.actualizarPortadas();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar portadas: " + e.getMessage());
        }
    }

    // GET /api/spotify/actualizar-ids
    // → rellena el campo spotifyId de los álbumes que lo tienen vacío buscándolos por
    // título + artista. Necesario para álbumes importados antes de añadir ese campo.
    @GetMapping("/actualizar-ids")
    public ResponseEntity<String> actualizarIds() {
        try {
            String resultado = spotifyService.actualizarSpotifyIds();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar IDs: " + e.getMessage());
        }
    }
}
