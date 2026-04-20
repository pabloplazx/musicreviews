# Pruebas Postman — MusicReviews Backend

Documentación de todas las pruebas realizadas sobre la API REST con Postman.
Base URL: `http://localhost:8080`

---

## ArtistaController ✅

| Método | Ruta | Body | Resultado |
|---|---|---|---|
| POST | `/api/artistas` | `{ "nombre": "Radiohead", "foto": "https://...", "biografia": "...", "genero": "Rock", "pais": "Reino Unido" }` | 200 ✅ |
| GET | `/api/artistas` | — | 200 + lista ✅ |
| GET | `/api/artistas/1` | — | 200 + artista ✅ |
| GET | `/api/artistas?nombre=radio` | — | 200 + lista filtrada ✅ |
| PUT | `/api/artistas/1` | campos actualizados | 200 ✅ |
| DELETE | `/api/artistas/{id}` | — | 204 ✅ |
| GET | `/api/artistas/999` | — | 404 ✅ |

---

## AlbumController ✅

| Método | Ruta | Body | Resultado |
|---|---|---|---|
| POST | `/api/albumes` | `{ "titulo": "OK Computer", "portada": "https://...", "fechaLanzamiento": "1997-05-21", "genero": "Rock", "descripcion": "...", "artista": { "id": 1 } }` | 200 ✅ |
| GET | `/api/albumes` | — | 200 + Page (12 álbumes, totalElements 469, totalPages 40) ✅ |
| GET | `/api/albumes?page=1&size=12` | — | 200 + Page página 2 ✅ |
| GET | `/api/albumes?titulo=the&page=0&size=5` | — | 200 + Page filtrada paginada ✅ |
| GET | `/api/albumes?artistaId=37&page=0` | — | 200 + Page filtrada por artista ✅ |
| GET | `/api/albumes/3` | — | 200 + álbum ✅ |
| GET | `/api/albumes?titulo=ok` | — | 200 + lista filtrada ✅ |
| GET | `/api/albumes?genero=Rock` | — | 200 + lista filtrada ✅ |
| PUT | `/api/albumes/3` | campos actualizados | 200 ✅ |
| DELETE | `/api/albumes/{id}` | — | 204 ✅ |

**Nota:** La respuesta de listado ahora es un objeto `Page` con `content` (álbumes), `page.totalElements`, `page.totalPages` y `page.number`. Valores por defecto: `page=0`, `size=12`, ordenado por título ascendente.

---

## UsuarioController ✅

| Método | Ruta | Body | Resultado |
|---|---|---|---|
| POST | `/api/usuarios` | `{ "username": "pablo", "email": "pablo@example.com", "password": "1234" }` | 200 ✅ |
| GET | `/api/usuarios` | — | 200 + lista ✅ |
| GET | `/api/usuarios/2` | — | 200 + usuario ✅ |
| PUT | `/api/usuarios/2` | `{ "username": "pablo2", "bio": "..." }` | 200 ✅ |
| DELETE | `/api/usuarios/{id}` | — | 204 ✅ |
| POST | `/api/usuarios` | email ya existente | 400 ✅ |
| GET | `/api/usuarios/999` | — | 404 ✅ |

---

## ResenaController ✅

| Método | Ruta | Body | Resultado |
|---|---|---|---|
| POST | `/api/resenas` | `{ "usuario": { "id": 2 }, "album": { "id": 3 }, "puntuacion": 5, "comentario": "Una obra maestra del rock alternativo." }` | 200 ✅ |
| GET | `/api/resenas?albumId=3` | — | 200 + lista con relaciones completas ✅ |
| GET | `/api/resenas?usuarioId=2` | — | 200 + lista ✅ |
| GET | `/api/resenas/usuario/2/album/3` | — | 200 + reseña ✅ |
| PUT | `/api/resenas/3` | `{ "puntuacion": 4, "comentario": "..." }` | 200 ✅ |
| DELETE | `/api/resenas/{id}` | — | 204 ✅ |
| POST | `/api/resenas` | mismos usuario+álbum | 400 `El usuario ya ha reseñado este álbum` ✅ |
| POST | `/api/resenas` | `"puntuacion": 6` | 400 `La puntuación debe estar entre 1 y 5` ✅ |

### Bugs encontrados y corregidos

**Bug 1 — `Resena.comentario` mapeado a columna incorrecta**
- **Síntoma:** Error SQL al crear una reseña. La columna en la BD se llama `contenido` pero el campo Java era `comentario` sin mapeo explícito.
- **Causa:** Hibernate intentaba buscar/insertar en una columna `comentario` que no existía.
- **Solución:** Añadir `@Column(name = "contenido")` sobre el campo `comentario` en `Resena.java`.

**Bug 2 — POST devuelve relaciones con campos nulos**
- **Síntoma:** El `POST /api/resenas` devolvía el objeto creado pero con `usuario` y `album` con todos los campos a `null` excepto el `id`.
- **Causa:** `save()` devuelve la misma instancia que recibió, que solo tenía los IDs de las relaciones. Hibernate la cachea en el `EntityManager` y `findById` posterior devuelve la instancia cacheada.
- **Solución:** Llamar a `findById(guardada.getId())` tras el `save()` en `ResenaService.crear` y `actualizar`.

---

## FavoritoController ✅

| Método | Ruta | Body | Resultado |
|---|---|---|---|
| POST | `/api/favoritos` | `{ "usuario": { "id": 2 }, "album": { "id": 3 } }` | 200 ✅ |
| GET | `/api/favoritos?usuarioId=2` | — | 200 + lista con relaciones completas ✅ |
| GET | `/api/favoritos/existe?usuarioId=2&albumId=3` | — | 200 + `true` ✅ |
| DELETE | `/api/favoritos?usuarioId=2&albumId=3` | — | 204 ✅ |
| POST | `/api/favoritos` | mismo usuario+álbum | 400 `El álbum ya está en favoritos` ✅ |
| DELETE | `/api/favoritos?usuarioId=2&albumId=99` | — | 404 ✅ |

### Bugs encontrados y corregidos

**Bug 1 — `400 Bad Request` al hacer POST**
- **Síntoma:** `{ "timestamp": "...", "status": 400, "error": "Bad Request", "path": "/api/favoritos" }` con el formato de error estándar de Spring (no el mensaje personalizado del catch).
- **Causa:** El body enviado en Postman no tenía el formato correcto. El endpoint espera objetos anidados `{ "usuario": { "id": 2 }, "album": { "id": 3 } }`.
- **Solución:** Corregir el body en Postman y asegurarse de tener seleccionado `Body → raw → JSON`.

**Bug 2 — `Unknown column 'f1_0.id' in 'field list'`**
- **Síntoma:** Error JDBC al hacer POST. La query generada por JPA buscaba `f1_0.id` en la tabla `favorito` y MySQL respondía que la columna no existía.
- **Causa:** La tabla `favorito` en `schema.sql` fue creada con clave primaria compuesta `(usuario_id, album_id)` y sin columna `id`. La entidad `Favorito.java` tiene `@Id @GeneratedValue private Long id`, por lo que JPA la busca.
- **Solución:**
  1. Ejecutar en MySQL: `ALTER TABLE favorito DROP PRIMARY KEY, ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;`
  2. Actualizar `database/schema.sql` para añadir la columna `id` y cambiar la PK compuesta por una UNIQUE constraint.

**Bug 3 — POST devuelve relaciones con campos nulos**
- **Síntoma:** Igual que en ResenaService — el POST devolvía `usuario` y `album` con campos nulos.
- **Causa:** Spring Boot mantiene el `EntityManager` abierto durante toda la petición HTTP (`open-in-view=true` por defecto). Cuando `findById` se llama después de `save`, Hibernate devuelve la instancia cacheada en el `EntityManager` (que tiene los campos nulos) en lugar de ir a la BD. El mismo patrón `save → findById` que funciona en `ResenaService` no era suficiente aquí.
- **Solución:** Añadir `@Transactional` al método `agregar` e inyectar `EntityManager` para llamar a `entityManager.refresh(guardado)` tras el `save()`. El `refresh()` fuerza la recarga completa desde la BD ignorando el caché.

---

## SpotifyController ✅

Base URL: `http://localhost:8080/api/spotify`

| Método | Ruta | Params | Resultado |
|---|---|---|---|
| GET | `/api/spotify/importar` | `?artista=Fangoria` | 200 + `"Importado: Fangoria con 8 álbumes."` ✅ |
| GET | `/api/spotify/importar` | `?artista=Arctic Monkeys` | 200 + `"Arctic Monkeys ya existe en la base de datos con álbumes importados."` ✅ |
| GET | `/api/spotify/importar-lista` | — | 200 + resumen con totales ✅ |
| GET | `/api/spotify/actualizar-metadatos` | — | 200 + `"Metadatos actualizados para N artistas."` ✅ |

### Bugs encontrados y corregidos

**Bug 1 — Nombres con tildes o caracteres especiales no se codifican**
- **Síntoma:** `La Habitación Roja` no se encontraba en Spotify. La petición HTTP enviaba la `ó` sin codificar.
- **Causa:** Las URLs de búsqueda se construían con concatenación directa (`"/v1/search?q=" + nombre`), sin codificación de caracteres especiales.
- **Solución:** Cambiar todas las búsquedas a `UriBuilder` con `.queryParam("q", nombre)`, que codifica automáticamente cualquier carácter especial.

**Bug 2 — Rate limiting (429 Too Many Requests)**
- **Síntoma:** Spotify devuelve 429 al importar artistas en cadena o al relanzar el endpoint poco después de un uso intensivo.
- **Causa:** La API de Spotify con Client Credentials tiene un límite de peticiones por ventana de 30 segundos. No se respetaba el header `Retry-After` que Spotify envía indicando cuántos segundos esperar.
- **Solución:** Implementar el método `spotifyGet(Supplier<Map>)` que envuelve todas las llamadas a Spotify con hasta 5 reintentos automáticos, leyendo el header `Retry-After` para esperar exactamente el tiempo indicado. El método `manejarRateLimit(ClientResponse)` extrae ese valor y lo pasa a `spotifyGet` codificado en el mensaje de excepción (`RATE_LIMIT:{segundos}`).
- **Nota:** Si se hacen decenas de peticiones en poco tiempo (por ejemplo, pruebas repetidas), el cooldown acumulado puede durar 1-2 horas. En uso normal (importar un artista de vez en cuando), el rate limiting no ocurre.

**Bug 3 — Mensaje confuso al reimportar artista ya existente**
- **Síntoma:** `GET /importar?artista=Arctic Monkeys` devolvía `"Importado: Arctic Monkeys con 0 álbumes."` cuando ya existía.
- **Causa:** `importarArtistaPorId` devolvía 0 al detectar duplicado, y el mensaje no distinguía entre "importado con 0 álbumes" y "omitido por ya existir".
- **Solución:** Añadir comprobación previa en `importarArtista` antes de llamar a `importarArtistaPorId`. Si el artista ya existe con álbumes, se devuelve directamente el mensaje `"X ya existe en la base de datos con álbumes importados."`.

### Integración Last.fm — `actualizar-metadatos`

El endpoint `GET /api/spotify/actualizar-metadatos` combina dos APIs:
- **Spotify** — para rellenar el campo `genero` del artista y sus álbumes
- **Last.fm** — para rellenar el campo `biografia` del artista

Solo actualiza los artistas que tengan esos campos vacíos (`NULL`). Los artistas ya completos se saltan.
Credencial Last.fm almacenada en `application.properties` como `lastfm.api-key`.

---

## EstadisticasController ✅

| Método | Ruta | Params | Resultado |
|---|---|---|---|
| GET | `/api/estadisticas/resumen` | — | 200 + `{ totalAlbumes: 469, totalArtistas: 99, totalResenas: N, totalUsuarios: N }` ✅ |
| GET | `/api/estadisticas/generos` | — | 200 + lista de géneros con total de álbumes ✅ |
| GET | `/api/estadisticas/top-albumes` | — | 200 + top 10 álbumes por puntuación media ✅ |
| GET | `/api/estadisticas/mas-resenados` | — | 200 + top 10 álbumes por número de reseñas ✅ |
| GET | `/api/estadisticas/top-artistas` | — | 200 + top 10 artistas por puntuación media ✅ |
| GET | `/api/estadisticas/top-por-genero` | `?genero=hip-hop` | 200 + top 10 álbumes del género ✅ |
| GET | `/api/estadisticas/actividad-reciente` | — | 200 + últimas 10 reseñas ✅ |
| GET | `/api/estadisticas/albumes-recientes` | — | 200 + últimos 10 álbumes añadidos ✅ |

**Nota:** Los endpoints de ranking (`top-albumes`, `top-artistas`, `mas-resenados`) devuelven lista vacía si no hay reseñas suficientes. Se llenarán con datos de ejemplo en fases posteriores.

---

## Optimizaciones finales del backend ✅ (20/04/2026)

Revisión completa antes de arrancar el frontend. Cambios aplicados:

| Fichero | Cambio | Motivo |
|---|---|---|
| `Usuario.java` | `@JsonProperty(WRITE_ONLY)` + `@JsonAutoDetect` en `password` | El hash BCrypt ya no se expone en ninguna respuesta JSON |
| `UsuarioService` | Nuevo método `actualizarUltimoLogin(id)` | `fechaUltimoLogin` no se guardaba — `actualizar()` solo copia username/foto/bio |
| `AuthController` | Login usa `actualizarUltimoLogin()` en lugar de `actualizar()` | Corrige el bug de `fechaUltimoLogin` |
| `SecurityConfig` | CORS configurado para `http://localhost:5173` | Sin esto el frontend React no puede llamar a la API |
| `UserDetailsServiceImpl` | Constructor injection con `@RequiredArgsConstructor` | Consistencia con el resto del proyecto |
| `JwtFilter` | Constructor injection con `@RequiredArgsConstructor` | Consistencia con el resto del proyecto |
| `ArtistaController`, `AlbumController`, `UsuarioController`, `ResenaController` | `orElseThrow(RecursoNoEncontradoException)` en GET por ID | Los 404 ahora devuelven JSON uniforme en lugar de respuesta vacía |
| `ResenaController.obtener` | `throw ReglaNegocioException` si no hay params | El 400 ahora devuelve JSON en lugar de respuesta vacía |

---

## GlobalExceptionHandler — Manejo de errores centralizado ✅

Añadido el 20/04/2026. Antes de este cambio, cada controlador tenía sus propios `try-catch` y los errores podían devolver texto plano o el formato por defecto de Spring. Ahora todos los errores devuelven siempre el mismo formato JSON.

### Formato unificado de error

```json
{
  "timestamp": "2026-04-20T11:28:48.659",
  "status": 404,
  "mensaje": "Reseña no encontrada"
}
```

### Pruebas realizadas con curl (20/04/2026)

| Caso | Petición | Respuesta esperada | Resultado |
|---|---|---|---|
| Puntuación inválida | `POST /api/resenas` con `"puntuacion": 10` | 400 + `"La puntuación debe estar entre 1 y 5"` | ✅ |
| Reseña no encontrada | `DELETE /api/resenas/99999` | 404 + `"Reseña no encontrada"` | ✅ |
| Favorito duplicado | `POST /api/favoritos` mismo usuario+álbum dos veces | 400 + `"El álbum ya está en favoritos"` | ✅ |
| Favorito inexistente | `DELETE /api/favoritos?usuarioId=4&albumId=999` | 404 + `"El álbum no está en favoritos"` | ✅ |

### Cambio en respuestas de error anteriores

Las respuestas de error en `ResenaController` y `FavoritoController` que antes devolvían texto plano (`e.getMessage()`) ahora devuelven el JSON uniforme de arriba. Los status HTTP no cambian (400 sigue siendo 400, 404 sigue siendo 404).

---

## AuthController — fix GlobalExceptionHandler ✅

Corregido el 20/04/2026. El endpoint `POST /api/auth/register` devolvía texto plano (`"Ya existe un usuario con ese email"`) en lugar del JSON uniforme del `GlobalExceptionHandler`. Se eliminaron las validaciones inline del controller y se delegan a `UsuarioService`, que ya lanza `ReglaNegocioException`.

| Caso | Antes | Después |
|---|---|---|
| Email duplicado en register | `400` + texto plano | `400` + `{"timestamp":"...","status":400,"mensaje":"Ya existe un usuario con ese email"}` |
| Username duplicado en register | `400` + texto plano | `400` + JSON uniforme |

---

## Datos de ejemplo — `database/seed_data.py` ✅

Script Python ejecutado el 20/04/2026 que puebla la BD usando la API REST.

### Usuarios creados

| username | email | rol |
|---|---|---|
| maria_indie | maria@musicreviews.com | USER |
| carlos_rap | carlos@musicreviews.com | USER |
| ana_electronica | ana@musicreviews.com | USER |
| jorge_clasicos | jorge@musicreviews.com | USER |
| lucia_urban | lucia@musicreviews.com | USER |

### Estado de la BD tras el script (20/04/2026)

| Entidad | Total |
|---|---|
| Usuarios | 8 |
| Álbumes | 469 |
| Artistas | 99 |
| Reseñas | 31 |

### Verificación de estadísticas tras cargar datos

| Endpoint | Resultado |
|---|---|
| `GET /api/estadisticas/resumen` | `totalResenas: 31, totalUsuarios: 8` ✅ |
| `GET /api/estadisticas/top-albumes` | Top 5 con puntuaciones reales (4:44, El Mal Querer, Random Access Memories...) ✅ |

---

## Resumen general

| Controlador | Endpoints probados | Bugs encontrados | Estado |
|---|---|---|---|
| ArtistaController | 7 | 0 | ✅ |
| AlbumController | 10 | 0 | ✅ |
| UsuarioController | 7 | 0 | ✅ |
| ResenaController | 8 | 2 | ✅ (corregidos) |
| FavoritoController | 6 | 3 | ✅ (corregidos) |
| SpotifyController | 4 | 3 | ✅ (corregidos) |
| EstadisticasController | 8 | 0 | ✅ |
| GlobalExceptionHandler | 4 | 0 | ✅ |
