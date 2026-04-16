package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.repository.AlbumRepository;
import com.musicreviews.backend.repository.ArtistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.*;

// Este servicio gestiona la integración con la API de Spotify.
// Se encarga de obtener el token de acceso, buscar artistas y álbumes,
// y guardarlos en la base de datos local.
@Service
public class SpotifyService {

    // Credenciales de la app de Spotify, cargadas desde application.properties.
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Autowired
    private ArtistaRepository artistaRepository;

    @Autowired
    private AlbumRepository albumRepository;

    // Esto obtiene un token de acceso temporal de Spotify usando el flujo Client Credentials.
    // El token es necesario para todas las llamadas posteriores a la API.
    private String obtenerToken() {
        WebClient client = WebClient.create("https://accounts.spotify.com");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        Map response = client.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    // Esto importa todos los artistas únicos de una playlist pública de Spotify.
    // Recorre todas las páginas de la playlist, extrae los artistas únicos
    // y para cada uno importa su información y sus álbumes en la BD.
    // Devuelve un resumen con el total de artistas y álbumes importados.
    public String importarDesdePlaylist(String playlistId) {
        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");

        // Recoger todos los artistas únicos de la playlist (puede tener varias páginas)
        Set<String> artistaIdsVistos = new LinkedHashSet<>();
        Map<String, String> artistaIdANombre = new LinkedHashMap<>();
        String url = "/v1/playlists/" + playlistId + "/tracks?limit=100";

        while (url != null) {
            final String currentUrl = url;
            Map response = client.get()
                    .uri(currentUrl)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> items = (List<Map>) response.get("items");
            if (items != null) {
                for (Map item : items) {
                    Map track = (Map) item.get("track");
                    if (track == null) continue;
                    List<Map> artistas = (List<Map>) track.get("artists");
                    if (artistas == null) continue;
                    for (Map artista : artistas) {
                        String id = (String) artista.get("id");
                        String nombre = (String) artista.get("name");
                        if (id != null && !artistaIdsVistos.contains(id)) {
                            artistaIdsVistos.add(id);
                            artistaIdANombre.put(id, nombre);
                        }
                    }
                }
            }

            // Pasar a la siguiente página si existe
            url = (String) response.get("next");
            // Quitar la base URL para usar solo el path
            if (url != null) url = url.replace("https://api.spotify.com", "");
        }

        // Importar cada artista único con sus álbumes
        int totalArtistas = 0;
        int totalAlbumes = 0;
        for (String artistaId : artistaIdsVistos) {
            try {
                int albumsImportados = importarArtistaPorId(artistaId, token, client);
                totalArtistas++;
                totalAlbumes += albumsImportados;
            } catch (Exception e) {
                // Si falla un artista concreto, continúa con el siguiente
                System.out.println("Error importando artista " + artistaIdANombre.get(artistaId) + ": " + e.getMessage());
            }
        }

        return "Importados " + totalArtistas + " artistas y " + totalAlbumes + " álbumes desde la playlist.";
    }

    // Esto importa una lista curada de artistas: favoritos del usuario + clásicos universales.
    // Devuelve un resumen con el total de artistas y álbumes importados.
    public String importarLista() {
        List<String> artistas = List.of(
            // Favoritos del usuario
            "Rojuu", "Bad Bunny", "C. Tangana", "Yung Beef", "Love of Lesbian",
            "Drake", "Carolina Durante", "Kanye West", "Playboi Carti", "Post Malone",
            "The Strokes", "Travis Scott", "Daft Punk", "Tyler, The Creator", "Rosalía",
            "Joji", "Pink Floyd", "Duki", "Mac DeMarco", "Kendrick Lamar",
            "Lil Peep", "Bizarrap", "Radiohead", "The Smiths", "Cigarettes After Sex",
            "The Weeknd", "Peso Pluma", "100 gecs", "Extremoduro", "Canserbero",
            "Eladio Carrion", "$uicideboy$", "21 Savage", "Estopa", "Los Planetas",

            // Clásicos universales del rock y pop
            "The Beatles", "Led Zeppelin", "David Bowie", "Queen", "The Rolling Stones",
            "Nirvana", "Arctic Monkeys", "Tame Impala", "Frank Ocean", "Jay-Z",
            "Eminem", "J. Cole", "SZA", "Taylor Swift", "Billie Eilish",
            "Coldplay", "Oasis", "The Cure", "Joy Division", "Arcade Fire",
            "Bon Iver", "Vampire Weekend", "The National", "Interpol", "Beach Boys",
            "Bob Dylan", "Fleetwood Mac", "Talking Heads", "R.E.M.", "Bruce Springsteen",
            "Michael Jackson", "Prince", "Madonna", "Amy Winehouse", "Adele",

            // Hip-hop y R&B
            "Nas", "Wu-Tang Clan", "A Tribe Called Quest", "Outkast", "Lauryn Hill",
            "Childish Gambino", "Anderson Paak", "Chance The Rapper", "Earl Sweatshirt",

            // Electrónica
            "The Chemical Brothers", "Aphex Twin", "LCD Soundsystem", "Four Tet", "Burial",
            "Massive Attack", "Portishead", "Boards of Canada",

            // Artistas españoles
            "Vetusta Morla", "Bunbury", "Fito y Fitipaldis", "Leiva", "Hinds",
            "Izal", "Sidonie", "La Habitación Roja", "El Columpio Asesino", "Supersubmarina"
        );

        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");
        int totalArtistas = 0;
        int totalAlbumes = 0;

        for (String nombre : artistas) {
            try {
                // Comprobar primero en la BD para no llamar a Spotify innecesariamente
                java.util.Optional<Artista> existente = artistaRepository.findByNombreIgnoreCase(nombre);
                if (existente.isPresent() && !albumRepository.findByArtistaId(existente.get().getId()).isEmpty()) {
                    System.out.println("Omitiendo (ya existe con álbumes): " + nombre);
                    continue;
                }

                Map busqueda = client.get()
                        .uri("/v1/search?q=" + nombre + "&type=artist&limit=1")
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                Map artistasMap = (Map) busqueda.get("artists");
                List<Map> items = (List<Map>) artistasMap.get("items");
                if (items == null || items.isEmpty()) continue;

                String artistaId = (String) items.get(0).get("id");
                int albums = importarArtistaPorId(artistaId, token, client);
                totalArtistas++;
                totalAlbumes += albums;
                System.out.println("Importado: " + nombre + " (" + albums + " álbumes)");

                // Pausa entre peticiones para no superar el límite de Spotify
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("Error con " + nombre + ": " + e.getMessage());
            }
        }

        return "Importados " + totalArtistas + " artistas y " + totalAlbumes + " álbumes.";
    }

    // Esto busca un artista en Spotify por nombre, lo guarda en la BD si no existe,
    // y a continuación importa todos sus álbumes de estudio.
    // Devuelve un mensaje con el resultado de la importación.
    public String importarArtista(String nombreArtista) {
        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");

        // Buscar el artista en Spotify por nombre
        Map busqueda = client.get()
                .uri("/v1/search?q=" + nombreArtista + "&type=artist&limit=1")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map artistas = (Map) busqueda.get("artists");
        List<Map> items = (List<Map>) artistas.get("items");

        if (items == null || items.isEmpty()) {
            return "Artista no encontrado en Spotify: " + nombreArtista;
        }

        String artistaId = (String) items.get(0).get("id");
        int albumsImportados = importarArtistaPorId(artistaId, token, client);
        String nombre = (String) items.get(0).get("name");
        return "Importado: " + nombre + " con " + albumsImportados + " álbumes.";
    }

    // Esto obtiene los datos completos de un artista por su ID de Spotify,
    // lo guarda en la BD y guarda también todos sus álbumes de estudio.
    // Devuelve el número de álbumes importados.
    private int importarArtistaPorId(String artistaId, String token, WebClient client) {
        // Obtener datos completos del artista
        Map artistaSpotify = client.get()
                .uri("/v1/artists/" + artistaId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String nombre = (String) artistaSpotify.get("name");

        List<Map> imagenes = (List<Map>) artistaSpotify.get("images");
        String foto = imagenes != null && !imagenes.isEmpty()
                ? (String) imagenes.get(0).get("url") : null;

        List<String> generos = (List<String>) artistaSpotify.get("genres");
        String genero = generos != null && !generos.isEmpty() ? generos.get(0) : null;

        // Si el artista ya existe en la BD con álbumes, lo omitimos para evitar duplicados.
        // Si existe pero sin álbumes (importación previa incompleta), intentamos importar sus álbumes.
        Artista artista;
        java.util.Optional<Artista> existente = artistaRepository.findByNombreIgnoreCase(nombre);
        if (existente.isPresent()) {
            if (!albumRepository.findByArtistaId(existente.get().getId()).isEmpty()) {
                System.out.println("Artista ya existe con álbumes, omitiendo: " + nombre);
                return 0;
            }
            System.out.println("Artista existe sin álbumes, reimportando álbumes: " + nombre);
            artista = existente.get();
        } else {
            // Guardar el artista nuevo en la BD
            artista = new Artista();
            artista.setNombre(nombre);
            artista.setFoto(foto);
            artista.setGenero(genero);
            artista.setBiografia(null);
            artista.setPais(null);
            artista = artistaRepository.save(artista);
        }

        // Obtener los álbumes de estudio del artista
        Map responseAlbumes = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/artists/{id}/albums")
                        .queryParam("include_groups", "album")
                        .build(artistaId))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class).map(body -> {
                            throw new RuntimeException("Error Spotify álbumes: " + body);
                        }))
                .bodyToMono(Map.class)
                .block();

        List<Map> albumesSpotify = (List<Map>) responseAlbumes.get("items");
        int contador = 0;

        for (Map albumSpotify : albumesSpotify) {
            String titulo = (String) albumSpotify.get("name");
            String portada = null;
            List<Map> imagenesAlbum = (List<Map>) albumSpotify.get("images");
            if (imagenesAlbum != null && !imagenesAlbum.isEmpty()) {
                portada = (String) imagenesAlbum.get(0).get("url");
            }

            String fechaStr = (String) albumSpotify.get("release_date");
            LocalDate fechaLanzamiento = parsearFecha(fechaStr);

            Album album = new Album();
            album.setTitulo(titulo);
            album.setPortada(portada);
            album.setFechaLanzamiento(fechaLanzamiento);
            album.setGenero(genero);
            album.setDescripcion(null);
            album.setArtista(artista);
            albumRepository.save(album);
            contador++;
        }

        return contador;
    }

    // Esto convierte la fecha de Spotify al tipo LocalDate.
    // Spotify puede devolver la fecha en tres formatos: completo (1997-05-21),
    // solo mes y año (1997-05), o solo año (1997).
    private LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null) return null;
        try {
            if (fechaStr.length() == 10) return LocalDate.parse(fechaStr);
            if (fechaStr.length() == 7) return LocalDate.parse(fechaStr + "-01");
            if (fechaStr.length() == 4) return LocalDate.parse(fechaStr + "-01-01");
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
