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
// El token de Spotify se cachea y se renueva automáticamente antes de que expire.
// Todas las llamadas a Spotify pasan por spotifyGet(), que reintenta ante 429 (rate limit)
// y ante 401 (token expirado), invalidando la caché y renovando el token al momento.
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

    // WebClients como campos — se crean una sola vez y se reutilizan en todas las llamadas.
    private final WebClient spotifyClient  = WebClient.create("https://api.spotify.com");
    private final WebClient accountsClient = WebClient.create("https://accounts.spotify.com");
    private final WebClient lastfmClient   = WebClient.create("https://ws.audioscrobbler.com");

    // Token cacheado con su tiempo de expiración (en ms desde epoch).
    private String cachedToken;
    private long   tokenExpiresAt;

    // Devuelve el token cacheado si sigue válido (con 60s de margen); si no, solicita uno nuevo.
    // Sincronizado para evitar condiciones de carrera si se llamara en paralelo.
    private synchronized String obtenerToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) {
            return cachedToken;
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        Map response = accountsClient.post()
                .uri("/api/token")
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        cachedToken = (String) response.get("access_token");
        int expiresIn = (Integer) response.getOrDefault("expires_in", 3600);
        tokenExpiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        return cachedToken;
    }

    // Lee el header Retry-After de la respuesta 429 y lo codifica en la excepción
    // para que spotifyGet() sepa exactamente cuántos segundos esperar.
    private Mono<? extends Throwable> manejarRateLimit(ClientResponse response) {
        String retryAfter = response.headers().asHttpHeaders().getFirst("Retry-After");
        int segundos = 30;
        try {
            if (retryAfter != null) segundos = Integer.parseInt(retryAfter.trim());
        } catch (NumberFormatException ignored) {}
        return Mono.error(new RuntimeException("RATE_LIMIT:" + segundos));
    }

    // Tope máximo de espera ante un 429 de Spotify. Si el header Retry-After indica más
    // de este valor, se aborta en lugar de dormir horas (caso típico: cuota diaria agotada,
    // que Spotify responde con Retry-After ≈ 86400 s ≈ 24 h).
    private static final int MAX_ESPERA_RATE_LIMIT = 300;

    // Ejecuta una llamada a Spotify con reintentos automáticos ante:
    //   429 → espera el tiempo indicado por Retry-After y reintenta. Si la espera supera
    //         MAX_ESPERA_RATE_LIMIT se aborta inmediatamente para no bloquear el proceso.
    //   401 → invalida el token cacheado y reintenta (obtenerToken() renovará el token).
    // Cada lambda del supplier llama a obtenerToken() en el momento de ejecutarse,
    // por lo que siempre usa el token más reciente.
    private Map spotifyGet(Supplier<Map> llamada) {
        int maxIntentos = 6;
        for (int intento = 0; intento < maxIntentos; intento++) {
            try {
                return llamada.get();
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.startsWith("RATE_LIMIT:") && intento < maxIntentos - 1) {
                    int espera = Integer.parseInt(msg.split(":")[1]);
                    if (espera > MAX_ESPERA_RATE_LIMIT) {
                        throw new RuntimeException(String.format(
                                "Spotify: cuota superada (Retry-After %ds ≈ %dh). Reintenta más tarde.",
                                espera, espera / 3600));
                    }
                    System.out.printf("Rate limit — esperando %ds (intento %d/%d)%n", espera, intento + 1, maxIntentos);
                    sleep(espera + 2);
                } else if ("TOKEN_EXPIRED".equals(msg) && intento < 2) {
                    System.out.println("Token expirado — renovando...");
                    synchronized (this) { cachedToken = null; }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Spotify: máximo de reintentos alcanzado");
    }

    // Pausa la ejecución el número de segundos indicado.
    // Si el hilo es interrumpido, lanza RuntimeException para abortar limpiamente.
    private void sleep(int segundos) {
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida");
        }
    }

    // Importa todos los artistas únicos de una playlist pública de Spotify junto con sus álbumes.
    public String importarDesdePlaylist(String playlistId) {
        String url = "/v1/playlists/" + playlistId + "/tracks?limit=100";
        Set<String> artistaIdsVistos = new LinkedHashSet<>();
        Map<String, String> artistaIdANombre = new LinkedHashMap<>();

        while (url != null) {
            final String currentUrl = url;
            Map response = spotifyGet(() -> spotifyClient.get()
                    .uri(currentUrl)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .retrieve()
                    .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                    .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
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
                        if (id != null && artistaIdsVistos.add(id)) {
                            artistaIdANombre.put(id, (String) artista.get("name"));
                        }
                    }
                }
            }

            url = (String) response.get("next");
            if (url != null) url = url.replace("https://api.spotify.com", "");
        }

        int totalArtistas = 0, totalAlbumes = 0;
        for (String artistaId : artistaIdsVistos) {
            try {
                totalAlbumes += importarArtistaPorId(artistaId);
                totalArtistas++;
            } catch (Exception e) {
                System.out.println("Error importando " + artistaIdANombre.get(artistaId) + ": " + e.getMessage());
            }
        }
        return "Importados " + totalArtistas + " artistas y " + totalAlbumes + " álbumes desde la playlist.";
    }

    // Importa una lista curada de artistas. Omite los que ya existen con álbumes en la BD.
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

        int totalArtistas = 0, totalAlbumes = 0;
        for (String nombre : artistas) {
            try {
                Optional<Artista> existente = artistaRepository.findByNombreIgnoreCase(nombre);
                if (existente.isPresent() && !albumRepository.findByArtistaId(existente.get().getId()).isEmpty()) {
                    System.out.println("Omitiendo (ya tiene álbumes): " + nombre);
                    continue;
                }

                final String nombreFinal = nombre;
                Map busqueda = spotifyGet(() -> spotifyClient.get()
                        .uri(b -> b.path("/v1/search")
                                .queryParam("q", nombreFinal)
                                .queryParam("type", "artist")
                                .queryParam("limit", 1)
                                .build())
                        .header("Authorization", "Bearer " + obtenerToken())
                        .retrieve()
                        .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                        .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                        .bodyToMono(Map.class)
                        .block());

                List<Map> items = (List<Map>) ((Map) busqueda.get("artists")).get("items");
                if (items == null || items.isEmpty()) continue;

                int albums = importarArtistaPorId((String) items.get(0).get("id"));
                totalArtistas++;
                totalAlbumes += albums;
                System.out.printf("Importado: %s (%d álbumes)%n", nombre, albums);
                sleep(1);
            } catch (RuntimeException e) {
                if ("Operación interrumpida".equals(e.getMessage())) break;
                System.out.println("Error con " + nombre + ": " + e.getMessage());
            }
        }
        return "Importados " + totalArtistas + " artistas y " + totalAlbumes + " álbumes.";
    }

    // Busca un artista en Spotify por nombre e importa su información y álbumes en la BD.
    public String importarArtista(String nombreArtista) {
        final String nombreFinal = nombreArtista;
        Map busqueda = spotifyGet(() -> spotifyClient.get()
                .uri(b -> b.path("/v1/search")
                        .queryParam("q", nombreFinal)
                        .queryParam("type", "artist")
                        .queryParam("limit", 1)
                        .build())
                .header("Authorization", "Bearer " + obtenerToken())
                .retrieve()
                .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                .bodyToMono(Map.class)
                .block());

        List<Map> items = (List<Map>) ((Map) busqueda.get("artists")).get("items");
        if (items == null || items.isEmpty()) {
            return "Artista no encontrado en Spotify: " + nombreArtista;
        }

        String artistaId = (String) items.get(0).get("id");
        String nombre    = (String) items.get(0).get("name");

        // Se comprueba si el artista ya existía ANTES de importar para poder diferenciar
        // en el mensaje de vuelta: alta nueva, actualización con álbumes nuevos o sin cambios.
        // La llamada a Spotify se hace igualmente: el dedup por título dentro de
        // importarArtistaPorId es lo que evita duplicar álbumes, y así se detectan
        // lanzamientos nuevos de artistas que ya estaban en la BD.
        boolean yaExistia = artistaRepository.findByNombreIgnoreCase(nombre).isPresent();
        int albumsImportados = importarArtistaPorId(artistaId);

        if (!yaExistia) {
            return "Importado: " + nombre + " con " + albumsImportados + " álbumes.";
        }
        if (albumsImportados > 0) {
            return "Actualizado: " + nombre + " con " + albumsImportados + " álbumes nuevos.";
        }
        return nombre + " ya está al día. No hay álbumes nuevos en Spotify.";
    }

    // Obtiene los datos de un artista por su ID de Spotify, lo persiste en la BD
    // y guarda todos sus álbumes de estudio que no existieran ya.
    private int importarArtistaPorId(String artistaId) {
        Map artistaSpotify = spotifyGet(() -> spotifyClient.get()
                .uri("/v1/artists/" + artistaId)
                .header("Authorization", "Bearer " + obtenerToken())
                .retrieve()
                .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                .bodyToMono(Map.class)
                .block());

        String nombre = (String) artistaSpotify.get("name");
        List<Map>    imagenes = (List<Map>)    artistaSpotify.get("images");
        List<String> generos  = (List<String>) artistaSpotify.get("genres");
        String foto   = (imagenes != null && !imagenes.isEmpty()) ? (String) imagenes.get(0).get("url") : null;
        String genero = (generos  != null && !generos.isEmpty())  ? generos.get(0) : null;

        Artista artista = artistaRepository.findByNombreIgnoreCase(nombre).orElseGet(() -> {
            Artista nuevo = new Artista();
            nuevo.setNombre(nombre);
            nuevo.setFoto(foto);
            nuevo.setGenero(genero);
            return artistaRepository.save(nuevo);
        });

        sleep(1);

        // Títulos que ya están en la BD (en minúsculas) — para no duplicar álbumes.
        // Se irá ampliando dentro del bucle a medida que se guarden nuevos,
        // para que si Spotify devuelve el mismo álbum en páginas distintas no se duplique.
        Set<String> titulosExistentes = new HashSet<>();
        albumRepository.findByArtistaId(artista.getId())
                .forEach(a -> titulosExistentes.add(a.getTitulo().toLowerCase()));

        // Paginación completa de álbumes: Spotify divide los resultados en varias páginas
        // y expone la URL de la siguiente en el campo "next" (null cuando ya no hay más).
        // No se usa queryParam("limit", ...) porque Spotify devuelve 400 "Invalid limit"
        // en este endpoint; se deja el default y se sigue "next" hasta agotar páginas.
        int contador = 0;
        String url = "/v1/artists/" + artistaId + "/albums?include_groups=album";
        while (url != null) {
            final String currentUrl = url;
            Map responseAlbumes = spotifyGet(() -> spotifyClient.get()
                    .uri(currentUrl)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .retrieve()
                    .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                    .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                    .onStatus(s -> s.is4xxClientError(), r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Error Spotify álbumes: " + body))))
                    .bodyToMono(Map.class)
                    .block());

            for (Map albumSpotify : (List<Map>) responseAlbumes.get("items")) {
                String titulo = (String) albumSpotify.get("name");
                if (titulosExistentes.contains(titulo.toLowerCase())) continue;

                List<Map> imagenesAlbum = (List<Map>) albumSpotify.get("images");
                String portada = (imagenesAlbum != null && !imagenesAlbum.isEmpty())
                        ? (String) imagenesAlbum.get(0).get("url") : null;

                Album album = new Album();
                album.setTitulo(titulo);
                album.setPortada(portada);
                album.setFechaLanzamiento(parsearFecha((String) albumSpotify.get("release_date")));
                album.setGenero(genero);
                album.setArtista(artista);
                albumRepository.save(album);
                titulosExistentes.add(titulo.toLowerCase());
                contador++;
            }

            url = (String) responseAlbumes.get("next");
            if (url != null) url = url.replace("https://api.spotify.com", "");
        }
        return contador;
    }

    // Recorre todos los artistas de la BD, busca cada uno en Spotify y completa sus
    // álbumes de estudio con los que no tuviera ya (paginando toda la discografía).
    // Útil después de haber añadido paginación, para recuperar los álbumes que la
    // importación original pudo dejar fuera. No crea artistas nuevos.
    public String completarTodos() {
        List<Artista> todos = artistaRepository.findAll();
        int totalNuevos = 0;
        int artistasConNuevos = 0;
        int noEncontrados = 0;
        int fallos = 0;

        for (Artista artistaBd : todos) {
            try {
                final String nombreFinal = artistaBd.getNombre();
                Map busqueda = spotifyGet(() -> spotifyClient.get()
                        .uri(b -> b.path("/v1/search")
                                .queryParam("q", nombreFinal)
                                .queryParam("type", "artist")
                                .queryParam("limit", 1)
                                .build())
                        .header("Authorization", "Bearer " + obtenerToken())
                        .retrieve()
                        .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                        .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                        .bodyToMono(Map.class)
                        .block());

                List<Map> items = (List<Map>) ((Map) busqueda.get("artists")).get("items");
                if (items == null || items.isEmpty()) {
                    noEncontrados++;
                    System.out.println("No encontrado en Spotify: " + artistaBd.getNombre());
                    continue;
                }

                String spotifyId = (String) items.get(0).get("id");
                int nuevos = importarArtistaPorId(spotifyId);
                if (nuevos > 0) {
                    artistasConNuevos++;
                    totalNuevos += nuevos;
                    System.out.printf("%s: +%d álbumes nuevos%n", artistaBd.getNombre(), nuevos);
                }
                sleep(1);
            } catch (RuntimeException e) {
                if ("Operación interrumpida".equals(e.getMessage())) break;
                fallos++;
                System.out.println("Error con " + artistaBd.getNombre() + ": " + e.getMessage());
            }
        }

        return String.format(
                "Completado: %d álbumes nuevos en %d artistas (%d no encontrados, %d fallos).",
                totalNuevos, artistasConNuevos, noEncontrados, fallos);
    }

    // Compara los álbumes que hay en la BD de un artista con los que Spotify lista en
    // /v1/artists/{id}/albums (include_groups=album, paginando por el campo "next")
    // y devuelve un resumen con los que faltan por importar. Este método es de diagnóstico
    // — no modifica la base de datos.
    public Map<String, Object> comprobarArtista(String nombreArtista) {
        final String nombreFinal = nombreArtista;
        Map busqueda = spotifyGet(() -> spotifyClient.get()
                .uri(b -> b.path("/v1/search")
                        .queryParam("q", nombreFinal)
                        .queryParam("type", "artist")
                        .queryParam("limit", 1)
                        .build())
                .header("Authorization", "Bearer " + obtenerToken())
                .retrieve()
                .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                .bodyToMono(Map.class)
                .block());

        List<Map> items = (List<Map>) ((Map) busqueda.get("artists")).get("items");
        if (items == null || items.isEmpty()) {
            return Map.of("error", "Artista no encontrado en Spotify: " + nombreArtista);
        }

        String artistaId = (String) items.get(0).get("id");
        String nombreSpotify = (String) items.get(0).get("name");

        // Carga los títulos que ya están guardados en la BD para ese artista (en minúsculas
        // para que la comparación sea insensible a mayúsculas). Si el artista no existe
        // todavía, titulosBd queda vacío y Spotify listará todos como "faltan".
        Set<String> titulosBd = new HashSet<>();
        int totalEnBd = 0;
        Optional<Artista> artistaBd = artistaRepository.findByNombreIgnoreCase(nombreSpotify);
        if (artistaBd.isPresent()) {
            List<Album> albumes = albumRepository.findByArtistaId(artistaBd.get().getId());
            totalEnBd = albumes.size();
            albumes.forEach(a -> titulosBd.add(a.getTitulo().toLowerCase()));
        }

        // Recorre la paginación completa de álbumes de Spotify siguiendo el enlace "next".
        // Para cada álbum que no esté en la BD por título, lo añade a la lista de faltantes.
        List<Map<String, Object>> faltan = new ArrayList<>();
        int totalEnSpotify = 0;
        String url = "/v1/artists/" + artistaId + "/albums?include_groups=album";
        while (url != null) {
            final String currentUrl = url;
            Map responseAlbumes = spotifyGet(() -> spotifyClient.get()
                    .uri(currentUrl)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .retrieve()
                    .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                    .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                    .bodyToMono(Map.class)
                    .block());

            for (Map albumSpotify : (List<Map>) responseAlbumes.get("items")) {
                String titulo = (String) albumSpotify.get("name");
                totalEnSpotify++;
                if (!titulosBd.contains(titulo.toLowerCase())) {
                    Map<String, Object> falta = new LinkedHashMap<>();
                    falta.put("titulo", titulo);
                    falta.put("fechaLanzamiento", albumSpotify.get("release_date"));
                    faltan.add(falta);
                }
            }

            url = (String) responseAlbumes.get("next");
            if (url != null) url = url.replace("https://api.spotify.com", "");
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("artista", nombreSpotify);
        resultado.put("totalEnBd", totalEnBd);
        resultado.put("totalEnSpotify", totalEnSpotify);
        resultado.put("faltan", faltan);
        return resultado;
    }

    // Actualiza el género (Spotify) y la biografía (Last.fm) de los artistas que tengan esos campos vacíos.
    // También propaga el género a los álbumes del artista que lo tengan vacío.
    public String actualizarMetadatos() {
        int actualizados = 0;
        for (Artista artista : artistaRepository.findAll()) {
            try {
                boolean modificado = false;

                if (artista.getGenero() == null || artista.getGenero().isBlank()) {
                    final String nombre = artista.getNombre();
                    Map busqueda = spotifyGet(() -> spotifyClient.get()
                            .uri(b -> b.path("/v1/search")
                                    .queryParam("q", nombre)
                                    .queryParam("type", "artist")
                                    .queryParam("limit", 1)
                                    .build())
                            .header("Authorization", "Bearer " + obtenerToken())
                            .retrieve()
                            .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                            .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                            .bodyToMono(Map.class)
                            .block());

                    List<Map> items = (List<Map>) ((Map) busqueda.get("artists")).get("items");
                    if (items != null && !items.isEmpty()) {
                        List<String> generos = (List<String>) items.get(0).get("genres");
                        if (generos != null && !generos.isEmpty()) {
                            String genero = generos.get(0);
                            artista.setGenero(genero);
                            albumRepository.findByArtistaId(artista.getId()).stream()
                                    .filter(a -> a.getGenero() == null || a.getGenero().isBlank())
                                    .forEach(a -> { a.setGenero(genero); albumRepository.save(a); });
                            modificado = true;
                        }
                    }
                    sleep(1);
                }

                if (artista.getBiografia() == null || artista.getBiografia().isBlank()) {
                    final String nombre = artista.getNombre();
                    Map lastfmResponse = lastfmClient.get()
                            .uri(b -> b.path("/2.0/")
                                    .queryParam("method", "artist.getinfo")
                                    .queryParam("artist", nombre)
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
                                artista.setBiografia(summary.replaceAll("<a href.*</a>", "").trim());
                                modificado = true;
                            }
                        }
                    }
                    sleep(1);
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
        List<Album> sinPortada = albumRepository.findAll().stream()
                .filter(a -> a.getPortada() == null || a.getPortada().isBlank())
                .toList();

        int actualizados = 0;
        for (Album album : sinPortada) {
            try {
                final String query = album.getTitulo() + " " + album.getArtista().getNombre();
                Map busqueda = spotifyGet(() -> spotifyClient.get()
                        .uri(b -> b.path("/v1/search")
                                .queryParam("q", query)
                                .queryParam("type", "album")
                                .queryParam("limit", 1)
                                .build())
                        .header("Authorization", "Bearer " + obtenerToken())
                        .retrieve()
                        .onStatus(s -> s.value() == 429, this::manejarRateLimit)
                        .onStatus(s -> s.value() == 401, r -> Mono.error(new RuntimeException("TOKEN_EXPIRED")))
                        .bodyToMono(Map.class)
                        .block());

                List<Map> items = (List<Map>) ((Map) busqueda.get("albums")).get("items");
                if (items != null && !items.isEmpty()) {
                    List<Map> imagenes = (List<Map>) items.get(0).get("images");
                    if (imagenes != null && !imagenes.isEmpty()) {
                        album.setPortada((String) imagenes.get(0).get("url"));
                        albumRepository.save(album);
                        actualizados++;
                        System.out.println("Portada actualizada: " + album.getTitulo());
                    }
                }
                sleep(1);
            } catch (Exception e) {
                System.out.println("Error con " + album.getTitulo() + ": " + e.getMessage());
            }
        }
        return "Portadas actualizadas para " + actualizados + " álbumes.";
    }

    // Convierte la fecha de Spotify a LocalDate.
    // Spotify puede devolver: "1997-05-21" (completa), "1997-05" (mes/año), o "1997" (solo año).
    private LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null) return null;
        try {
            return switch (fechaStr.length()) {
                case 10 -> LocalDate.parse(fechaStr);
                case 7  -> LocalDate.parse(fechaStr + "-01");
                case 4  -> LocalDate.parse(fechaStr + "-01-01");
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
}
