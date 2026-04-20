# MusicReviews - Backend

API REST desarrollada con **Java 21 + Spring Boot 3.4.4 + Maven**.
Base de datos: **MySQL en la nube via [Aiven](https://aiven.io)** — BD: `defaultdb`, usuario: `avnadmin`. Las credenciales de conexión se configuran en `application.properties` (no incluido en el repositorio).

---

## Arquitectura en capas

```
HTTP Request
     │
     ▼
┌─────────────┐
│ Controller  │  Recibe las peticiones HTTP y devuelve respuestas JSON
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service   │  Contiene la lógica de negocio (validaciones, reglas, etc.)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │  Accede a la base de datos (consultas, inserciones, etc.)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│    MySQL    │
└─────────────┘
```

---

## Paquetes del proyecto

```
com.musicreviews.backend/
├── model/          ← Entidades JPA (tablas de la BD)
├── repository/     ← Acceso a la BD (Spring Data JPA)
├── service/        ← Lógica de negocio
├── controller/     ← Endpoints REST
├── exception/      ← Excepciones personalizadas + GlobalExceptionHandler
├── security/       ← JwtUtil, JwtFilter, UserDetailsServiceImpl
├── dto/            ← RegisterRequest, LoginRequest, AuthResponse
└── SecurityConfig  ← Configuración de Spring Security + rutas por rol
```

---

## exception/ — Manejo de errores centralizado ✅

Gestiona de forma uniforme todos los errores de la API. Antes de añadir este paquete, cada controlador tenía sus propios bloques `try-catch` que devolvían respuestas inconsistentes. Ahora los servicios lanzan excepciones tipadas y el `GlobalExceptionHandler` las intercepta y devuelve siempre el mismo formato JSON.

### Excepciones personalizadas

| Clase | HTTP | Cuándo se lanza |
|---|---|---|
| `RecursoNoEncontradoException` | 404 | Entidad no encontrada en BD (usuario, artista, álbum, reseña, favorito) |
| `ReglaNegocioException` | 400 | Violación de regla de negocio (duplicados, puntuación inválida) |

### GlobalExceptionHandler

Clase anotada con `@RestControllerAdvice` que intercepta las excepciones lanzadas por cualquier controlador y devuelve siempre el mismo formato JSON:

```json
{
  "timestamp": "2026-04-20T11:28:48.659",
  "status": 404,
  "mensaje": "Reseña no encontrada"
}
```

---

## dto/ — Data Transfer Objects

Los DTOs son clases que definen la estructura exacta de los datos que se envían en las respuestas JSON. Se usan cuando la respuesta no corresponde directamente a una entidad.

| Clase | Descripción |
|---|---|
| `RegisterRequest` | Datos de registro: username, email, password |
| `LoginRequest` | Datos de login: email, password |
| `AuthResponse` | Respuesta de autenticación: token JWT |
| `ResumenDTO` | Totales globales: totalAlbumes, totalArtistas, totalResenas, totalUsuarios |
| `AlbumEstadisticaDTO` | Álbum + valor numérico (puntuación media o número de reseñas) |
| `ArtistaEstadisticaDTO` | Artista + puntuación media calculada desde sus álbumes |
| `GeneroEstadisticaDTO` | Género musical + total de álbumes de ese género |

---

## model/ — Entidades

Las entidades representan las **tablas de la base de datos**. Cada campo de la clase es una columna en MySQL. Se anotan con `@Entity` y usan JPA para mapear las relaciones entre tablas.

### Usuario
**Tabla:** `usuario`

Representa a los usuarios registrados en la aplicación.

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria, autoincremental |
| username | String | Nombre de usuario, único |
| email | String | Email, único |
| password | String | Contraseña (se guardará encriptada con BCrypt) |
| fotoPerfil | String | Ruta local al archivo subido (`uploads/`) |
| bio | String (TEXT) | Descripción corta del perfil del usuario |
| fechaRegistro | LocalDateTime | Se asigna automáticamente al crear el usuario |
| rol | Enum (Rol) | `USER` por defecto, `ADMIN` para gestión |

- El enum `Rol` está definido dentro de la propia clase.
- `@PrePersist` asigna `fechaRegistro` y `rol = USER` automáticamente al guardar.

---

### Artista
**Tabla:** `artista`

Representa a los artistas musicales cuyos álbumes aparecen en la app.

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria, autoincremental |
| nombre | String | Nombre del artista |
| foto | String | URL de la imagen del artista |
| biografia | String (TEXT) | Texto largo con la biografía |
| genero | String | Género musical principal |
| pais | String | País de origen |

---

### Album
**Tabla:** `album`

Representa un álbum musical. Solo el admin puede añadir álbumes. Las portadas son URLs de Spotify.

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria, autoincremental |
| titulo | String | Título del álbum |
| portada | String | URL de la portada (Spotify API) |
| fechaLanzamiento | LocalDate | Fecha de lanzamiento |
| genero | String | Género musical |
| descripcion | String (TEXT) | Descripción del álbum |
| artista | Artista | Relación `@ManyToOne` → tabla `artista` |

---

### Resena
**Tabla:** `resena`

Representa la reseña que un usuario escribe sobre un álbum. Un usuario solo puede escribir una reseña por álbum.

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria, autoincremental |
| usuario | Usuario | Relación `@ManyToOne` → tabla `usuario` |
| album | Album | Relación `@ManyToOne` → tabla `album` |
| puntuacion | int | Puntuación del 1 al 5 (validada en servicio) |
| comentario | String (TEXT) | Texto de la reseña |
| fechaCreacion | LocalDateTime | Se asigna automáticamente al crear |
| fechaEdicion | LocalDateTime | Se actualiza automáticamente al editar |

- **Restricción UNIQUE** en `(usuario_id, album_id)` — un usuario, una reseña por álbum.
- `@PrePersist` asigna `fechaCreacion` automáticamente.
- `@PreUpdate` actualiza `fechaEdicion` automáticamente.

---

### Favorito
**Tabla:** `favorito`

Representa que un usuario ha marcado un álbum como favorito.

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria, autoincremental |
| usuario | Usuario | Relación `@ManyToOne` → tabla `usuario` |
| album | Album | Relación `@ManyToOne` → tabla `album` |
| fechaAgregado | LocalDateTime | Se asigna automáticamente al guardar |

- **Restricción UNIQUE** en `(usuario_id, album_id)` — un usuario no puede duplicar un favorito.
- `@PrePersist` asigna `fechaAgregado` automáticamente.

---

## repository/ — Repositorios

Los repositorios son interfaces que extienden `JpaRepository<Entidad, TipoClave>`. Spring Boot genera automáticamente todo el acceso a la base de datos: no hay que escribir SQL para las operaciones básicas.

**Métodos que Spring proporciona gratis:**
- `save(entidad)` — insertar o actualizar
- `findById(id)` — buscar por ID
- `findAll()` — obtener todos
- `deleteById(id)` — eliminar por ID
- `existsById(id)` — comprobar si existe

Además de los métodos base, cada repositorio añade consultas específicas según las necesidades de la app.

---

### UsuarioRepository ✅
Extiende `JpaRepository<Usuario, Long>`.

| Método | Descripción |
|---|---|
| `findByEmail(email)` | Buscar usuario por email (usado en login) |
| `findByUsername(username)` | Buscar usuario por nombre de usuario |
| `existsByEmail(email)` | Comprobar si el email ya está registrado |
| `existsByUsername(username)` | Comprobar si el username ya está en uso |

---

### ArtistaRepository ✅
Extiende `JpaRepository<Artista, Long>`.

| Método | Descripción |
|---|---|
| `findByNombreContainingIgnoreCase(nombre)` | Búsqueda de artistas por nombre (buscador, ignora mayúsculas) |

---

### AlbumRepository ✅
Extiende `JpaRepository<Album, Long>`.

Todos los métodos de búsqueda aceptan un `Pageable` y devuelven `Page<Album>` para soportar paginación.

| Método | Descripción |
|---|---|
| `findByTituloContainingIgnoreCase(titulo, pageable)` | Búsqueda de álbumes por título (buscador, ignora mayúsculas) |
| `findByArtistaId(artistaId, pageable)` | Obtener álbumes de un artista (paginado) |
| `findByArtistaId(artistaId)` | Obtener todos los álbumes de un artista — uso interno de SpotifyService |
| `findByGeneroIgnoreCase(genero, pageable)` | Filtrar álbumes por género (paginado) |
| `findTop10ByOrderByIdDesc()` | Últimos 10 álbumes añadidos al catálogo |
| `findDistribucionPorGenero()` | Distribución de álbumes agrupados por género |

---

### ResenaRepository ✅
Extiende `JpaRepository<Resena, Long>`.

| Método | Descripción |
|---|---|
| `findByAlbumId(albumId)` | Obtener todas las reseñas de un álbum |
| `findByUsuarioId(usuarioId)` | Obtener todas las reseñas de un usuario |
| `findByUsuarioIdAndAlbumId(usuarioId, albumId)` | Obtener la reseña de un usuario sobre un álbum concreto |
| `existsByUsuarioIdAndAlbumId(usuarioId, albumId)` | Comprobar si un usuario ya ha reseñado ese álbum |
| `findTopAlbumesPorPuntuacion(pageable)` | Top álbumes con mejor puntuación media |
| `findAlbumesMasResenados(pageable)` | Top álbumes con más reseñas |
| `findTopArtistasPorPuntuacion(pageable)` | Top artistas con mejor puntuación media en sus álbumes |
| `findTopAlbumesPorGenero(genero, pageable)` | Top álbumes de un género concreto por puntuación |
| `findTop10ByOrderByFechaCreacionDesc()` | Últimas 10 reseñas escritas |

---

### FavoritoRepository ✅
Extiende `JpaRepository<Favorito, Long>`.

| Método | Descripción |
|---|---|
| `findByIdConRelaciones(id)` | Carga un favorito por ID con usuario y álbum completos mediante JOIN FETCH (evita el caché de Hibernate) |
| `findByUsuarioId(usuarioId)` | Obtener todos los favoritos de un usuario |
| `existsByUsuarioIdAndAlbumId(usuarioId, albumId)` | Comprobar si un álbum ya es favorito del usuario |
| `deleteByUsuarioIdAndAlbumId(usuarioId, albumId)` | Eliminar un favorito concreto (quitar de favoritos) |

---

## service/ — Servicios

Los servicios contienen la **lógica de negocio**. Se anotan con `@Service` y son llamados por los controladores. Aquí se aplican las reglas de la app: validaciones, comprobaciones de duplicados, restricciones de negocio, etc.

Cada servicio inyecta su repositorio correspondiente con `@Autowired` y expone métodos que el controlador puede llamar.

---

### UsuarioService ✅

| Método | Descripción |
|---|---|
| `obtenerTodos()` | Devuelve todos los usuarios |
| `obtenerPorId(id)` | Busca un usuario por ID |
| `obtenerPorEmail(email)` | Busca un usuario por email |
| `obtenerPorUsername(username)` | Busca un usuario por username |
| `guardar(usuario)` | Crea un usuario nuevo. Lanza error si el email o username ya existen |
| `actualizar(id, datos)` | Actualiza username, foto de perfil y bio |
| `eliminar(id)` | Elimina un usuario. Lanza error si no existe |

---

### ArtistaService ✅

| Método | Descripción |
|---|---|
| `obtenerTodos()` | Devuelve todos los artistas |
| `obtenerPorId(id)` | Busca un artista por ID |
| `buscarPorNombre(nombre)` | Búsqueda parcial e insensible a mayúsculas |
| `guardar(artista)` | Crea un artista nuevo |
| `actualizar(id, datos)` | Actualiza todos los campos del artista |
| `eliminar(id)` | Elimina un artista. Lanza error si no existe |

---

### AlbumService ✅

Los métodos de listado aceptan `Pageable` y devuelven `Page<Album>`.

| Método | Descripción |
|---|---|
| `obtenerTodos(pageable)` | Devuelve álbumes paginados |
| `obtenerPorId(id)` | Busca un álbum por ID |
| `buscarPorTitulo(titulo, pageable)` | Búsqueda parcial e insensible a mayúsculas (paginado) |
| `obtenerPorArtista(artistaId, pageable)` | Filtra álbumes por artista (paginado) |
| `obtenerPorGenero(genero, pageable)` | Filtra álbumes por género (paginado) |
| `guardar(album)` | Crea un álbum nuevo |
| `actualizar(id, datos)` | Actualiza todos los campos del álbum |
| `eliminar(id)` | Elimina un álbum. Lanza error si no existe |

---

### ResenaService ✅

| Método | Descripción |
|---|---|
| `obtenerPorAlbum(albumId)` | Devuelve todas las reseñas de un álbum |
| `obtenerPorUsuario(usuarioId)` | Devuelve todas las reseñas de un usuario |
| `obtenerPorUsuarioYAlbum(usuarioId, albumId)` | Obtiene la reseña concreta de un usuario sobre un álbum |
| `crear(resena)` | Crea una reseña. Valida puntuación (1-5) y que no exista ya una del mismo usuario para ese álbum |
| `actualizar(id, datos)` | Actualiza puntuación y comentario. Vuelve a validar puntuación |
| `eliminar(id)` | Elimina una reseña. Lanza error si no existe |

- **Validación de puntuación:** si se envía un valor fuera del rango 1-5 se lanza una excepción.

---

### EstadisticasService ✅

| Método | Descripción |
|---|---|
| `obtenerResumen()` | Devuelve totales globales: álbumes, artistas, reseñas y usuarios |
| `obtenerTopAlbumes()` | Top 10 álbumes con mejor puntuación media |
| `obtenerMasResenados()` | Top 10 álbumes con más reseñas |
| `obtenerTopArtistas()` | Top 10 artistas con mejor puntuación media en sus álbumes |
| `obtenerDistribucionGeneros()` | Distribución de álbumes por género |
| `obtenerTopPorGenero(genero)` | Top 10 álbumes de un género concreto |
| `obtenerActividadReciente()` | Últimas 10 reseñas escritas |
| `obtenerAlbumesRecientes()` | Últimos 10 álbumes añadidos al catálogo |

---

### FavoritoService ✅

| Método | Descripción |
|---|---|
| `obtenerPorUsuario(usuarioId)` | Devuelve todos los favoritos de un usuario |
| `esFavorito(usuarioId, albumId)` | Comprueba si un álbum ya es favorito del usuario |
| `agregar(favorito)` | Añade un álbum a favoritos. Lanza error si ya estaba añadido |
| `eliminar(usuarioId, albumId)` | Quita un álbum de favoritos. Lanza error si no estaba. Usa `@Transactional` |

- `@Transactional` en `agregar` es necesario para que `EntityManager.refresh()` pueda recargar la entidad con sus relaciones completas desde la BD después del `save()`.
- `EntityManager.refresh(guardado)` en `agregar` fuerza la recarga desde la BD, evitando que Spring devuelva la instancia cacheada con campos nulos (problema causado por `open-in-view=true` de Spring Boot).
- `@Transactional` en `eliminar` es necesario porque `deleteBy...` es una operación de escritura personalizada que JPA necesita dentro de una transacción.

---

## controller/ — Controladores

Los controladores son el punto de entrada de la API REST. Se anotan con `@RestController` y reciben las peticiones HTTP del frontend, las delegan al servicio correspondiente y devuelven la respuesta en JSON.

Cada método devuelve un `ResponseEntity` para poder controlar el código de estado HTTP de la respuesta (200, 204, 400, 404...).

---

### UsuarioController ✅
**Ruta base:** `/api/usuarios`

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/usuarios` | Lista todos los usuarios | 200 + lista |
| GET | `/api/usuarios/{id}` | Busca un usuario por ID | 200 o 404 |
| POST | `/api/usuarios` | Crea un usuario nuevo | 200 o 400 si email/username duplicado |
| PUT | `/api/usuarios/{id}` | Actualiza username, fotoPerfil y bio | 200 o 404 |
| DELETE | `/api/usuarios/{id}` | Elimina un usuario | 204 o 404 |

---

### ArtistaController ✅
**Ruta base:** `/api/artistas`

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/artistas` | Lista todos los artistas. Con `?nombre=` filtra por nombre | 200 + lista |
| GET | `/api/artistas/{id}` | Busca un artista por ID | 200 o 404 |
| POST | `/api/artistas` | Crea un artista nuevo | 200 |
| PUT | `/api/artistas/{id}` | Actualiza todos los campos del artista | 200 o 404 |
| DELETE | `/api/artistas/{id}` | Elimina un artista | 204 o 404 |

---

### AlbumController ✅
**Ruta base:** `/api/albumes`

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/albumes` | Lista álbumes paginados. Con `?titulo=`, `?genero=` o `?artistaId=` filtra. Con `?page=0&size=12` pagina | 200 + Page |
| GET | `/api/albumes/{id}` | Busca un álbum por ID | 200 o 404 |
| POST | `/api/albumes` | Crea un álbum nuevo | 200 |
| PUT | `/api/albumes/{id}` | Actualiza todos los campos del álbum | 200 o 404 |
| DELETE | `/api/albumes/{id}` | Elimina un álbum | 204 o 404 |

Los resultados se ordenan por título ascendente por defecto. Valores por defecto: `page=0`, `size=12`.

---

### ResenaController ✅
**Ruta base:** `/api/resenas`

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/resenas?albumId=` | Lista las reseñas de un álbum | 200 + lista |
| GET | `/api/resenas?usuarioId=` | Lista las reseñas de un usuario | 200 + lista |
| GET | `/api/resenas/usuario/{usuarioId}/album/{albumId}` | Obtiene la reseña concreta de un usuario sobre un álbum | 200 o 404 |
| POST | `/api/resenas` | Crea una reseña. Valida puntuación y duplicados | 200 o 400 con mensaje de error |
| PUT | `/api/resenas/{id}` | Actualiza puntuación y comentario | 200 o 400 con mensaje de error |
| DELETE | `/api/resenas/{id}` | Elimina una reseña | 204 o 404 |

---

### FavoritoController ✅
**Ruta base:** `/api/favoritos`

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/favoritos?usuarioId=` | Lista los favoritos de un usuario | 200 + lista |
| GET | `/api/favoritos/existe?usuarioId=&albumId=` | Comprueba si un álbum es favorito | 200 + true/false |
| POST | `/api/favoritos` | Añade un álbum a favoritos | 200 o 400 si ya existe |
| DELETE | `/api/favoritos?usuarioId=&albumId=` | Elimina un álbum de favoritos | 204 o 404 |

---

### EstadisticasController ✅
**Ruta base:** `/api/estadisticas`

Todos los endpoints son públicos (sin autenticación).

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/estadisticas/resumen` | Totales globales: álbumes, artistas, reseñas y usuarios | 200 + ResumenDTO |
| GET | `/api/estadisticas/top-albumes` | Top 10 álbumes con mejor puntuación media | 200 + lista |
| GET | `/api/estadisticas/mas-resenados` | Top 10 álbumes con más reseñas | 200 + lista |
| GET | `/api/estadisticas/top-artistas` | Top 10 artistas con mejor puntuación media | 200 + lista |
| GET | `/api/estadisticas/generos` | Distribución de álbumes por género | 200 + lista |
| GET | `/api/estadisticas/top-por-genero?genero=` | Top 10 álbumes de un género concreto | 200 + lista |
| GET | `/api/estadisticas/actividad-reciente` | Últimas 10 reseñas escritas | 200 + lista |
| GET | `/api/estadisticas/albumes-recientes` | Últimos 10 álbumes añadidos al catálogo | 200 + lista |

---

### AuthController ✅
**Ruta base:** `/api/auth`

Gestiona el registro y login. Rutas públicas — no requieren token JWT.

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/auth/register` | Registra un nuevo usuario. Hashea contraseña con BCrypt y devuelve token JWT | 200 + token, o 400 JSON si email/username ya existen |
| POST | `/api/auth/login` | Autentica al usuario y actualiza `fechaUltimoLogin` | 200 + token, o 401/403 |

- Las validaciones de duplicado las gestiona `UsuarioService` (lanza `ReglaNegocioException` → `GlobalExceptionHandler` → JSON uniforme con status 400).

---

### SpotifyController ✅
**Ruta base:** `/api/spotify`

Controlador de uso interno para poblar la base de datos con artistas y álbumes reales desde la API de Spotify y Last.fm. No forma parte del flujo normal de la aplicación — se usa únicamente durante el proceso de importación y mantenimiento de datos.

| Método HTTP | Ruta | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/spotify/importar?artista=` | Importa un artista concreto y sus álbumes desde Spotify | 200 + mensaje |
| GET | `/api/spotify/importar-lista` | Importa la lista curada de artistas (omite los que ya existen) | 200 + resumen |
| GET | `/api/spotify/importar-playlist?id=` | Importa artistas desde una playlist pública *(sin soporte desde 2024)* | — |
| GET | `/api/spotify/actualizar-metadatos` | Rellena género (Spotify) y biografía (Last.fm) de artistas con esos campos vacíos | 200 + resumen |
| GET | `/api/spotify/actualizar-portadas` | Busca en Spotify la portada de cada álbum que la tenga vacía y la actualiza | 200 + resumen |

---

## service/ adicional — SpotifyService ✅

Gestiona la integración con la API de Spotify (importación) y Last.fm (biografías). Usa el flujo **Client Credentials** de Spotify (sin autenticación de usuario). Las credenciales de ambas APIs se leen de `application.properties`.

| Método | Descripción |
|---|---|
| `importarArtista(nombre)` | Busca un artista por nombre en Spotify e importa su información y álbumes. Si ya existe con álbumes, devuelve mensaje informativo |
| `importarLista()` | Importa una lista curada de artistas. Comprueba la BD antes de llamar a Spotify para evitar duplicados |
| `importarDesdePlaylist(playlistId)` | Importa artistas desde una playlist pública *(no funcional desde 2024 — Spotify retiró acceso con Client Credentials)* |
| `actualizarMetadatos()` | Recorre todos los artistas y rellena género (Spotify) y biografía (Last.fm) si están vacíos. También actualiza el género de sus álbumes |
| `actualizarPortadas()` | Recorre todos los álbumes sin portada y busca su imagen en Spotify por `"título artista"`. Actualiza los que encuentre |

### Manejo de rate limiting

Todas las llamadas a Spotify pasan por el método privado `spotifyGet(Supplier<Map>)`, que implementa reintentos automáticos ante respuestas 429:

- Lee el header `Retry-After` de la respuesta de Spotify para saber exactamente cuántos segundos esperar
- Reintenta hasta **5 veces** con el tiempo de espera indicado por Spotify + 2 segundos de margen
- Si se agotan los intentos, lanza excepción con mensaje claro

El método `manejarRateLimit(ClientResponse)` extrae el valor de `Retry-After` y lo codifica en la excepción como `RATE_LIMIT:{segundos}` para que `spotifyGet` pueda leerlo.

### Otros detalles

- La búsqueda de artistas usa `UriBuilder` para codificar correctamente nombres con tildes, comas u otros caracteres especiales (ej: `La Habitación Roja`)
- La fecha de lanzamiento de Spotify puede venir en 3 formatos (`1997-05-21`, `1997-05`, `1997`) — el método `parsearFecha` los normaliza todos a `LocalDate`
- Las biografías de Last.fm incluyen un enlace HTML al final que se elimina con una expresión regular antes de guardar
- Si el rate limit de Spotify está activo por uso intensivo previo, esperar 1-2 horas antes de reintentar

Ver proceso completo de importación en `docs/importacion/proceso_importacion.md`.

---

## Tests unitarios ✅

Tests escritos con **JUnit 5 + Mockito**. No arrancan Spring ni conectan a la BD — usan mocks para simular los repositorios.

Ejecutar desde `backend/backend/`:
```bash
mvnw.cmd test
```

| Clase de test | Servicio probado | Nº tests | Casos cubiertos |
|---|---|---|---|
| `ResenaServiceTest` | `ResenaService` | 11 | Puntuación inválida (0, 6), reseña duplicada, crear/actualizar/eliminar correctos, consultas |
| `UsuarioServiceTest` | `UsuarioService` | 7 | Email duplicado, username duplicado, actualizar/eliminar correctos e inexistentes |
| `FavoritoServiceTest` | `FavoritoService` | 7 | Favorito duplicado, eliminar inexistente, esFavorito, obtenerPorUsuario |
| `ArtistaServiceTest` | `ArtistaService` | 7 | Actualizar/eliminar correctos e inexistentes, buscarPorNombre, obtenerPorId |
| `EstadisticasServiceTest` | `EstadisticasService` | 5 | Totales del resumen, distribución de géneros, listas vacías sin reseñas |
| `BackendApplicationTests` | Contexto Spring | 1 | Carga correcta del contexto de aplicación (requiere BD activa) |

**Total: 38 tests**

---

## Seguridad y Autenticación ✅

Implementada con **Spring Security + JWT (jjwt 0.12.6) + BCrypt**.

### Endpoints públicos
- `POST /api/auth/register` — registro de usuario (devuelve token JWT)
- `POST /api/auth/login` — login (devuelve token JWT)
- `GET /api/artistas/**`, `GET /api/albumes/**`, `GET /api/resenas/**` — consultas sin autenticación

### Uso del token
Incluir en el header de cada petición protegida:
```
Authorization: Bearer <token>
```

### Roles
- `USER` — puede crear/editar/borrar sus reseñas y favoritos
- `ADMIN` — puede además crear/editar/borrar artistas, álbumes e importar desde Spotify

Ver documentación completa en `docs/seguridad_autenticacion.md`.

---

## Optimizaciones finales ✅ (20/04/2026)

- **`password` nunca expuesta** — `@JsonProperty(WRITE_ONLY)` + `@JsonAutoDetect` en `Usuario`. El hash BCrypt no aparece en ninguna respuesta JSON.
- **`fechaUltimoLogin` correctamente guardada** — nuevo método `UsuarioService.actualizarUltimoLogin()` usado en `AuthController.login`. Antes, `actualizar()` solo copiaba username/foto/bio y el campo se perdía.
- **CORS configurado** — `SecurityConfig` expone `http://localhost:5173` para el frontend React.
- **Constructor injection en `security/`** — `UserDetailsServiceImpl` y `JwtFilter` migrados de `@Autowired` en campo a `@RequiredArgsConstructor`.
- **404 con body JSON en GET por ID** — todos los controllers usan `orElseThrow(RecursoNoEncontradoException)` en lugar de `orElse(notFound().build())`.
- **400 con body JSON en `GET /api/resenas`** sin parámetros — lanza `ReglaNegocioException` en lugar de devolver `badRequest().build()` vacío.

---

## Refactorización y optimización ✅

Tras la implementación inicial se realizó una revisión completa del código aplicando:

- **Constructor injection** con `@RequiredArgsConstructor` en todos los servicios y controladores — reemplaza `@Autowired` en campo (patrón antiguo)
- **`@Transactional(readOnly = true)`** en todos los métodos de lectura — Hibernate no rastrea cambios, ahorrando memoria y CPU
- **`FetchType.LAZY`** en todas las relaciones `@ManyToOne` — evita queries innecesarias al cargar entidades relacionadas
- **Eliminación de queries redundantes** en `ResenaService` — `save()` ya devuelve la entidad, el `findById` posterior era innecesario
- **Eliminación de `POST /api/usuarios`** — agujero de seguridad (contraseñas sin BCrypt); el registro va por `/api/auth/register`
- **`AuthController` usa `UsuarioService`** en lugar del repositorio directamente — respeta la separación de capas
- **`open-in-view=false`** y **`show-sql=false`** en `application.properties` — mejoras de rendimiento en producción

Ver documentación detallada con ejemplos en `docs/refactorizacion_backend.md`.
