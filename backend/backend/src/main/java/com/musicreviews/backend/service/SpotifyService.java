package com.musicreviews.backend.service;

import com.musicreviews.backend.model.Album;
import com.musicreviews.backend.model.Artista;
import com.musicreviews.backend.repository.AlbumRepository;
import com.musicreviews.backend.repository.ArtistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

// Este servicio gestiona la integración con la API de Spotify y Last.fm.
// Todas las llamadas a Spotify pasan por spotifyGet(), que reintenta automáticamente
// leyendo el header Retry-After cuando Spotify devuelve 429.
@Service
public class SpotifyService {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${lastfm.api-key}")
    private String lastfmApiKey;

    @Autowired
    private ArtistaRepository artistaRepository;

    @Autowired
    private AlbumRepository albumRepository;

    // Lee el header Retry-After de la respuesta 429 de Spotify y lo codifica en la excepción
    // para que spotifyGet() sepa exactamente cuántos segundos esperar.
    private Mono<? extends Throwable> manejarRateLimit(ClientResponse response) {
        String retryAfter = response.headers().asHttpHeaders().getFirst("Retry-After");
        int segundos = 30;
        try {
            if (retryAfter != null) segundos = Integer.parseInt(retryAfter.trim());
        } catch (NumberFormatException ignored) {}
        return Mono.error(new RuntimeException("RATE_LIMIT:" + segundos));
    }

    // Ejecuta cualquier llamada a Spotify con reintentos automáticos ante un 429.
    // Respeta el tiempo de espera que indica Spotify en Retry-After (hasta 5 intentos).
    private Map spotifyGet(Supplier<Map> llamada) {
        int maxIntentos = 5;
        for (int intento = 0; intento < maxIntentos; intento++) {
            try {
                return llamada.get();
            } catch (RuntimeException e) {
                String msg = e.getMessage();
                if (msg != null && msg.startsWith("RATE_LIMIT:") && intento < maxIntentos - 1) {
                    int espera = Integer.parseInt(msg.split(":")[1]);
                    System.out.println("Rate limit, esperando " + espera + "s... (intento " + (intento + 1) + "/" + maxIntentos + ")");
                    try {
                        Thread.sleep((espera + 2) * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Spotify: petición interrumpida");
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Spotify: máximo de reintentos alcanzado");
    }

    // Obtiene un token de acceso temporal de Spotify usando el flujo Client Credentials.
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

    // Importa todos los artistas únicos de una playlist pública de Spotify junto con sus álbumes.
    public String importarDesdePlaylist(String playlistId) {
        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");

        Set<String> artistaIdsVistos = new LinkedHashSet<>();
        Map<String, String> artistaIdANombre = new LinkedHashMap<>();
        String url = "/v1/playlists/" + playlistId + "/tracks?limit=100";

        while (url != null) {
            final String currentUrl = url;
            Map response = spotifyGet(() -> client.get()
                    .uri(currentUrl)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                    .bodyToMono(Map.class)
                    .block());

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

            url = (String) response.get("next");
            if (url != null) url = url.replace("https://api.spotify.com", "");
        }

        int totalArtistas = 0;
        int totalAlbumes = 0;
        for (String artistaId : artistaIdsVistos) {
            try {
                int albumsImportados = importarArtistaPorId(artistaId, token, client);
                totalArtistas++;
                totalAlbumes += albumsImportados;
            } catch (Exception e) {
                System.out.println("Error importando artista " + artistaIdANombre.get(artistaId) + ": " + e.getMessage());
            }
        }

        return "Importados " + totalArtistas + " artistas y " + totalAlbumes + " álbumes desde la playlist.";
    }

    // Importa una lista curada de artistas favoritos y clásicos. Omite los que ya existen con álbumes.
    public String importarLista() {
        List<String> artistas = List.of(
            "Rojuu", "Bad Bunny", "C. Tangana", "Yung Beef", "Love of Lesbian",
            "Drake", "Carolina Durante", "Kanye West", "Playboi Carti", "Post Malone",
            "The Strokes", "Travis Scott", "Daft Punk", "Tyler, The Creator", "Rosalía",
            "Joji", "Pink Floyd", "Duki", "Mac DeMarco", "Kendrick Lamar",
            "Lil Peep", "Bizarrap", "Radiohead", "The Smiths", "Cigarettes After Sex",
            "The Weeknd", "Peso Pluma", "100 gecs", "Extremoduro", "Canserbero",
            "Eladio Carrion", "$uicideboy$", "21 Savage", "Estopa", "Los Planetas",
            "The Beatles", "Led Zeppelin", "David Bowie", "Queen", "The Rolling Stones",
            "Nirvana", "Arctic Monkeys", "Tame Impala", "Frank Ocean", "Jay-Z",
            "Eminem", "J. Cole", "SZA", "Taylor Swift", "Billie Eilish",
            "Coldplay", "Oasis", "The Cure", "Joy Division", "Arcade Fire",
            "Bon Iver", "Vampire Weekend", "The National", "Interpol", "Beach Boys",
            "Bob Dylan", "Fleetwood Mac", "Talking Heads", "R.E.M.", "Bruce Springsteen",
            "Michael Jackson", "Prince", "Madonna", "Amy Winehouse", "Adele",
            "Nas", "Wu-Tang Clan", "A Tribe Called Quest", "Outkast", "Lauryn Hill",
            "Childish Gambino", "Anderson Paak", "Chance The Rapper", "Earl Sweatshirt",
            "The Chemical Brothers", "Aphex Twin", "LCD Soundsystem", "Four Tet", "Burial",
            "Massive Attack", "Portishead", "Boards of Canada",
            "Vetusta Morla", "Bunbury", "Fito y Fitipaldis", "Leiva", "Hinds",
            "Izal", "Sidonie", "La Habitación Roja", "El Columpio Asesino", "Supersubmarina"
        );

        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");
        int totalArtistas = 0;
        int totalAlbumes = 0;

        for (String nombre : artistas) {
            try {
                java.util.Optional<Artista> existente = artistaRepository.findByNombreIgnoreCase(nombre);
                if (existente.isPresent() && !albumRepository.findByArtistaId(existente.get().getId()).isEmpty()) {
                    System.out.println("Omitiendo (ya existe con álbumes): " + nombre);
                    continue;
                }

                Map busqueda = spotifyGet(() -> client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/search")
                                .queryParam("q", nombre)
                                .queryParam("type", "artist")
                                .queryParam("limit", 1)
                                .build())
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                        .bodyToMono(Map.class)
                        .block());

                Map artistasMap = (Map) busqueda.get("artists");
                List<Map> items = (List<Map>) artistasMap.get("items");
                if (items == null || items.isEmpty()) continue;

                String artistaId = (String) items.get(0).get("id");
                int albums = importarArtistaPorId(artistaId, token, client);
                totalArtistas++;
                totalAlbumes += albums;
                System.out.println("Importado: " + nombre + " (" + albums + " álbumes)");

                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("Error con " + nombre + ": " + e.getMessage());
            }
        }

        return "Importados " + totalArtistas + " artistas y " + totalAlbumes + " álbumes.";
    }

    // Busca un artista en Spotify por nombre e importa su información y álbumes en la BD.
    public String importarArtista(String nombreArtista) {
        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");

        Map busqueda = spotifyGet(() -> client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search")
                        .queryParam("q", nombreArtista)
                        .queryParam("type", "artist")
                        .queryParam("limit", 1)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                .bodyToMono(Map.class)
                .block());

        Map artistas = (Map) busqueda.get("artists");
        List<Map> items = (List<Map>) artistas.get("items");

        if (items == null || items.isEmpty()) {
            return "Artista no encontrado en Spotify: " + nombreArtista;
        }

        String artistaId = (String) items.get(0).get("id");
        String nombre = (String) items.get(0).get("name");

        java.util.Optional<Artista> existente = artistaRepository.findByNombreIgnoreCase(nombre);
        if (existente.isPresent() && !albumRepository.findByArtistaId(existente.get().getId()).isEmpty()) {
            return nombre + " ya existe en la base de datos con álbumes importados.";
        }

        int albumsImportados = importarArtistaPorId(artistaId, token, client);
        return "Importado: " + nombre + " con " + albumsImportados + " álbumes.";
    }

    // Obtiene los datos de un artista por su ID de Spotify, lo guarda en la BD
    // y guarda también todos sus álbumes de estudio.
    private int importarArtistaPorId(String artistaId, String token, WebClient client) {
        Map artistaSpotify = spotifyGet(() -> client.get()
                .uri("/v1/artists/" + artistaId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                .bodyToMono(Map.class)
                .block());

        String nombre = (String) artistaSpotify.get("name");
        List<Map> imagenes = (List<Map>) artistaSpotify.get("images");
        String foto = imagenes != null && !imagenes.isEmpty() ? (String) imagenes.get(0).get("url") : null;
        List<String> generos = (List<String>) artistaSpotify.get("genres");
        String genero = generos != null && !generos.isEmpty() ? generos.get(0) : null;

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
            artista = new Artista();
            artista.setNombre(nombre);
            artista.setFoto(foto);
            artista.setGenero(genero);
            artista.setBiografia(null);
            artista.setPais(null);
            artista = artistaRepository.save(artista);
        }

        try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        Map responseAlbumes = spotifyGet(() -> client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/artists/{id}/albums")
                        .queryParam("include_groups", "album")
                        .build(artistaId))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                .onStatus(status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Error Spotify álbumes: " + body))))
                .bodyToMono(Map.class)
                .block());

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

    // Actualiza el género (desde Spotify) y la biografía (desde Last.fm) de los artistas
    // que tengan esos campos vacíos. También actualiza el género de sus álbumes.
    public String actualizarMetadatos() {
        String token = obtenerToken();
        WebClient spotifyClient = WebClient.create("https://api.spotify.com");
        WebClient lastfmClient = WebClient.create("https://ws.audioscrobbler.com");

        List<Artista> artistas = artistaRepository.findAll();
        int actualizados = 0;

        for (Artista artista : artistas) {
            try {
                boolean modificado = false;

                if (artista.getGenero() == null || artista.getGenero().isBlank()) {
                    final String nombreArtista = artista.getNombre();
                    Map busqueda = spotifyGet(() -> spotifyClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/v1/search")
                                    .queryParam("q", nombreArtista)
                                    .queryParam("type", "artist")
                                    .queryParam("limit", 1)
                                    .build())
                            .header("Authorization", "Bearer " + token)
                            .retrieve()
                            .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                            .bodyToMono(Map.class)
                            .block());

                    Map artistasMap = (Map) busqueda.get("artists");
                    List<Map> items = (List<Map>) artistasMap.get("items");
                    if (items != null && !items.isEmpty()) {
                        List<String> generos = (List<String>) items.get(0).get("genres");
                        if (generos != null && !generos.isEmpty()) {
                            String genero = generos.get(0);
                            artista.setGenero(genero);
                            List<Album> albumes = albumRepository.findByArtistaId(artista.getId());
                            for (Album album : albumes) {
                                if (album.getGenero() == null || album.getGenero().isBlank()) {
                                    album.setGenero(genero);
                                    albumRepository.save(album);
                                }
                            }
                            modificado = true;
                        }
                    }
                    try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }

                if (artista.getBiografia() == null || artista.getBiografia().isBlank()) {
                    final String nombreArtista = artista.getNombre();
                    Map lastfmResponse = lastfmClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/2.0/")
                                    .queryParam("method", "artist.getinfo")
                                    .queryParam("artist", nombreArtista)
                                    .queryParam("api_key", lastfmApiKey)
                                    .queryParam("format", "json")
                                    .build())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                    Map artistaMap = (Map) lastfmResponse.get("artist");
                    if (artistaMap != null) {
                        Map bio = (Map) artistaMap.get("bio");
                        if (bio != null) {
                            String summary = (String) bio.get("summary");
                            if (summary != null && !summary.isBlank()) {
                                summary = summary.replaceAll("<a href.*</a>", "").trim();
                                artista.setBiografia(summary);
                                modificado = true;
                            }
                        }
                    }
                    try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }

                if (modificado) {
                    artistaRepository.save(artista);
                    actualizados++;
                    System.out.println("Actualizado: " + artista.getNombre());
                }
            } catch (Exception e) {
                System.out.println("Error actualizando " + artista.getNombre() + ": " + e.getMessage());
            }
        }

        return "Metadatos actualizados para " + actualizados + " artistas.";
    }

    // Busca en Spotify la portada de cada álbum que la tenga vacía y la actualiza en la BD.
    public String actualizarPortadas() {
        String token = obtenerToken();
        WebClient client = WebClient.create("https://api.spotify.com");

        List<Album> albumes = albumRepository.findAll().stream()
                .filter(a -> a.getPortada() == null || a.getPortada().isBlank())
                .toList();

        int actualizados = 0;

        for (Album album : albumes) {
            try {
                final String query = album.getTitulo() + " " + album.getArtista().getNombre();
                Map busqueda = spotifyGet(() -> client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/search")
                                .queryParam("q", query)
                                .queryParam("type", "album")
                                .queryParam("limit", 1)
                                .build())
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, this::manejarRateLimit)
                        .bodyToMono(Map.class)
                        .block());

                Map albumesMap = (Map) busqueda.get("albums");
                List<Map> items = (List<Map>) albumesMap.get("items");
                if (items != null && !items.isEmpty()) {
                    List<Map> imagenes = (List<Map>) items.get(0).get("images");
                    if (imagenes != null && !imagenes.isEmpty()) {
                        album.setPortada((String) imagenes.get(0).get("url"));
                        albumRepository.save(album);
                        actualizados++;
                        System.out.println("Portada actualizada: " + album.getTitulo());
                    }
                }
                try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                System.out.println("Error con " + album.getTitulo() + ": " + e.getMessage());
            }
        }

        return "Portadas actualizadas para " + actualizados + " álbumes.";
    }

    // Convierte la fecha de Spotify al tipo LocalDate.
    // Spotify puede devolver: fecha completa (1997-05-21), mes y año (1997-05), o solo año (1997).
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
