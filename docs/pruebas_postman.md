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
| GET | `/api/spotify/importar` | `?artista=Beach House` (artista nuevo) | 200 + `"Importado: Beach House con 5 álbumes."` ✅ |
| GET | `/api/spotify/importar` | `?artista=Radiohead` (al día) | 200 + `"Radiohead ya está al día. No hay álbumes nuevos en Spotify."` ✅ |
| GET | `/api/spotify/importar` | `?artista=Radiohead` (con nuevos) | 200 + `"Actualizado: Radiohead con 9 álbumes nuevos."` ✅ |
| GET | `/api/spotify/importar-lista` | — | 200 + resumen con totales ✅ |
| GET | `/api/spotify/comprobar` | `?artista=Radiohead` | 200 + JSON con `totalEnBd`, `totalEnSpotify` y lista `faltan` ✅ |
| GET | `/api/spotify/completar-todos` | — | 200 + `"Completado: N álbumes nuevos en M artistas (...)."` ✅ |
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
- **Solución (revisada el 21/04/2026):** En lugar de cortocircuitar cuando el artista ya existe (lo que impediría detectar álbumes nuevos en lanzamientos futuros), `importarArtista` ahora diferencia tres mensajes:
  - `"Importado: X con N álbumes."` — artista nuevo en la BD.
  - `"Actualizado: X con N álbumes nuevos."` — existía y se han añadido álbumes nuevos.
  - `"X ya está al día. No hay álbumes nuevos en Spotify."` — existía y no hay nada nuevo.
- De este modo `/importar?artista=Radiohead` sigue siendo útil cuando Radiohead publica un disco nuevo.

**Bug 4 — Regresión: `400 Invalid limit` al obtener álbumes (21/04/2026)**
- **Síntoma:** `GET /importar?artista=Beach House` devolvía 500 con `"Error al importar: Error Spotify álbumes: {\"error\":{\"status\":400,\"message\":\"Invalid limit\"}}"`.
- **Causa:** El parámetro `limit=50` en `/v1/artists/{id}/albums` (ya documentado como bug en Semana 4 y "arreglado" en su día) había sido reintroducido en el código. Spotify lo rechaza aunque su documentación oficial indique que acepta 1–50.
- **Solución:** Eliminar `queryParam("limit", ...)` y dejar el default de Spotify (20 por página). Añadido comentario en el código advirtiendo de la regresión.

**Bug 5 — Sin paginación, se perdían álbumes de artistas prolíficos (21/04/2026)**
- **Síntoma:** `GET /importar?artista=Radiohead` devolvía `"Radiohead ya está al día"` cuando en realidad faltaban 9 álbumes clásicos (*Pablo Honey*, *The Bends*, *Kid A*, *Amnesiac*, *Hail to the Thief*, *In Rainbows*, *The King of Limbs*, *I Might Be Wrong*). Detectado gracias al nuevo endpoint `/comprobar`, que comparaba 6 en BD vs 15 en Spotify.
- **Causa:** `importarArtistaPorId` solo leía la primera página de `/v1/artists/{id}/albums`. Spotify divide los resultados en varias páginas incluso por debajo del default de 20 (probablemente por el `market` implícito) y expone la URL siguiente en el campo `next`.
- **Solución:** Añadir paginación al bucle de álbumes: seguir el `next` hasta que sea `null`, exactamente igual que ya hacía `importarDesdePlaylist`. Dedup por título en minúsculas para no duplicar si un álbum aparece en varias páginas. Verificación post-fix: Radiohead pasó de 6 a 15 álbumes.

**Bug 6 — `spotifyGet` bloqueaba el proceso 24h ante cuota diaria (21/04/2026)**
- **Síntoma:** Durante `/completar-todos` Spotify devolvió un 429 con `Retry-After: 84687` (≈23.5 h, cuota diaria agotada). `spotifyGet` respetaba ese valor y dormía el hilo casi 24 horas dejando el backend colgado.
- **Causa:** El método confiaba ciegamente en el header `Retry-After` sin distinguir entre un rate limit normal (segundos-minutos) y una cuota diaria (horas).
- **Solución:** Añadir `MAX_ESPERA_RATE_LIMIT = 300` (5 min). Si `Retry-After` lo supera, se aborta con `"Spotify: cuota superada (Retry-After Ns ≈ Xh). Reintenta más tarde."`. Para rate limits normales el comportamiento no cambia.

### Endpoints nuevos — `/comprobar` y `/completar-todos` (21/04/2026)

Añadidos para diagnosticar y reparar el problema que destapó el Bug 5:

- **`GET /api/spotify/comprobar?artista=X`** — diagnóstico puro, no modifica BD. Devuelve `{artista, totalEnBd, totalEnSpotify, faltan[]}`. Útil para verificar antes de importar. Ejemplo Radiohead: detectó 9 álbumes faltantes.
- **`GET /api/spotify/completar-todos`** — recorre los artistas en BD y les añade los álbumes que faltaran (no crea artistas nuevos). Pensado para ejecutarse una vez tras el fix de paginación. Resultado parcial del 21/04/2026: 26 de 99 artistas procesados, **+204 álbumes nuevos añadidos** antes de agotar la cuota diaria de Spotify. Pendiente completar los 73 restantes en días sucesivos.

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
| ResenaController | 8 | 2 + B3 | ✅ (corregidos) |
| FavoritoController | 6 | 3 | ✅ (corregidos) |
| SpotifyController | 4 | 3 | ✅ (corregidos) |
| EstadisticasController | 8 | 0 | ✅ |
| GlobalExceptionHandler | 4 | 0 | ✅ |
| AuthController | 3 | 1 (B4) | ✅ (corregido) |
| **Transversal (configuración + entidades)** | — | 2 (B1, B2) + 1 test (B5) | ✅ (corregidos) |

---

## Bugs encontrados durante la integración con el frontend (28/04/2026)

Durante el inicio de la fase 4 (integración del frontend con el backend) afloraron 5 bugs que no se habían detectado en las pruebas iniciales de cada controlador. La causa común es que las pruebas iniciales solo verificaban códigos HTTP (200/400/404) y no inspeccionaban el cuerpo JSON completo, ni replicaban el flujo "login → captura de token → uso del token en peticiones siguientes" como lo hace un cliente real.

### Bug B1 — `LazyInitializationException` por `open-in-view=false`

**Síntoma:** desde Postman, `POST /api/resenas` con un token válido devolvía **401 Unauthorized**, no 200. Lo mismo con `GET /api/resenas/usuario/5/album/373`. Endpoints muy parecidos como `GET /api/albumes/3` sí funcionaban.

**Diagnóstico:** redirigir los logs del backend a fichero (`./mvnw spring-boot:run > backend.log`) y reproducir el fallo. El log mostraba:

```
HttpMessageNotWritableException: Could not write JSON:
Could not initialize proxy [com.musicreviews.backend.model.Album#373] - no session
```

Es un `LazyInitializationException` clásico: Jackson intenta serializar la respuesta, accede a `resena.album` que es un `@ManyToOne(fetch = FetchType.LAZY)` (un proxy de Hibernate), pero la sesión de Hibernate ya está cerrada → fallo.

**Causa raíz:** en `application.properties` había:

```
spring.jpa.open-in-view=false
```

Se había puesto a `false` en una sesión anterior "para mejorar el rendimiento". Lo que hace ese parámetro es cerrar la sesión Hibernate al salir del `@Transactional`, antes de que Jackson serialice. Para entidades sin relaciones LAZY (como `Album`) no afecta. Para `Resena` y `Favorito`, que tienen `@ManyToOne(fetch = LAZY)` a `usuario` y `album`, rompe la serialización.

El **misterio del 401 (en lugar del 500 esperado)** es que Spring Security 7 (Spring Boot 4.0.5) traduce los fallos durante la escritura del response como `Authentication` errors y dispara el `authenticationEntryPoint` configurado, que devuelve 401. Por eso el síntoma despistaba: parecía un problema de auth cuando en realidad era de serialización.

**Solución:** revertir a `open-in-view=true` (el default de Spring Boot por algo). El supuesto coste de rendimiento es despreciable en este TFG y permite que Jackson cargue las relaciones LAZY al serializar:

```
spring.jpa.open-in-view=true
```

**Verificación:** los mismos endpoints volvieron a 200 inmediatamente.

### Bug B2 — `@JsonAutoDetect` rompía los proxies de Hibernate

**Síntoma:** después de arreglar el B1, la respuesta del POST llegaba con código 200 pero el campo `usuario` aparecía con todos los valores a `null`:

```json
"usuario": {
  "$$_hibernate_interceptor": {},
  "activo": true,
  "bio": null,
  "email": null,
  "id": null,
  "username": null,
  ...
}
```

A la vez, aparecía basura interna de Hibernate (`$$_hibernate_interceptor`, `hibernateLazyInitializer`).

**Causa raíz:** en `Usuario.java` se había añadido en una sesión anterior:

```java
@JsonAutoDetect(
  fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  ...
)
```

Esta anotación fuerza a Jackson a leer **los campos directamente** en lugar de llamar a los getters. El problema es que los proxies de Hibernate **no inicializan los campos** — solo se inicializan al llamar al getter. Resultado: Jackson lee los campos antes de que el proxy los rellene → todo `null`.

La anotación se había añadido para que `@JsonProperty(WRITE_ONLY)` sobre `password` ocultara la contraseña en las respuestas. Pero esa anotación funciona perfectamente sin `@JsonAutoDetect` porque Jackson, por defecto, mezcla las anotaciones de campo con el getter al introspeccionar la propiedad.

**Solución:**

1. Quitar `@JsonAutoDetect` de `Usuario.java`. La ocultación de `password` sigue funcionando con la `@JsonProperty(access = WRITE_ONLY)` que ya estaba en el campo.
2. Añadir `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` en todas las entidades que se serialicen (`Usuario`, `Album`, `Artista`, `Resena`, `Favorito`) para evitar que esos campos internos del proxy se cuelen en el JSON.

**Verificación:** la siguiente respuesta del POST tenía `usuario.username: "maria_indie"`, `usuario.email`, etc., todos rellenos, y sin basura de Hibernate.

### Bug B3 — `refresh()` antes del flush descartaba los cambios en `actualizar()`

**Síntoma:** `PUT /api/resenas/{id}` devolvía siempre los valores **antiguos** de la reseña, no los nuevos. Si la reseña tenía `puntuacion=4.0` y se hacía PUT con `puntuacion=5.0`, la respuesta seguía mostrando 4.0 (aunque la BD luego acababa con 5.0 al final del request).

**Causa raíz:** el método `ResenaService.actualizar` tenía:

```java
resena.setPuntuacion(datosActualizados.getPuntuacion());
resena.setComentario(datosActualizados.getComentario());

resenaRepository.save(resena);
entityManager.refresh(resena);   // ← problema aquí
return resena;
```

`refresh()` recarga la entidad desde la BD descartando los cambios en memoria. Pero `save()` **no fuerza el flush** — los cambios solo se persisten al hacer commit de la transacción, al final del método. Así que el orden temporal real es:

1. `setPuntuacion(5.0)` — en memoria, no en BD.
2. `save()` — Hibernate lo agenda pero no lo persiste todavía.
3. `refresh()` — lee la BD (que sigue con 4.0) y **sobrescribe** los cambios en memoria.
4. `return resena` — con 4.0.
5. Commit — el dirty checking ve que `resena` tiene 4.0 (igual que en BD) y no genera UPDATE.

El refresh tenía sentido en `crear()` porque ahí la entidad se persiste con `INSERT` inmediato (por la estrategia `IDENTITY` que necesita el id auto-generado para devolverlo) y `refresh` convierte los stubs `{id:5}` del request body en proxies gestionados. En `actualizar()` la entidad ya viene gestionada de `findById()` y `refresh` solo estorba.

**Solución:** quitar el `refresh()` en `actualizar()`. El dirty checking de Hibernate al commit ya genera el UPDATE; el objeto en memoria se devuelve con los valores nuevos:

```java
return resenaRepository.save(resena);
```

**Verificación:** `PUT` ahora devuelve los valores nuevos y `fechaEdicion` se rellena correctamente (la lleva el `@PreUpdate`).

### Bug B4 — Login/register devolvían `text/plain` en errores → frontend no podía parsear

**Síntoma:** al probar el `Login.jsx` con un password incorrecto, en lugar de mostrar "Email o contraseña incorrectos" salía un error técnico de JSON parsing.

**Diagnóstico:** `curl -i -X POST /api/auth/login` con credenciales malas devolvía:

```
HTTP/1.1 401
Content-Type: text/plain;charset=UTF-8

Email o contraseña incorrectos
```

Pero el frontend hace:

```js
const data = await res.json();   // ← falla porque el body no es JSON
```

El `res.json()` peta con `SyntaxError: Unexpected token E in JSON at position 0`, el `catch` lo atrapa y muestra ese mensaje técnico al usuario.

**Causa raíz:** `AuthController.java` devolvía manualmente texto plano:

```java
return ResponseEntity.status(401).body("Email o contraseña incorrectos");
```

mientras que el resto de la API usa el `GlobalExceptionHandler` para devolver siempre JSON uniforme con `{status, mensaje, timestamp}`.

**Solución:** que `AuthController` lance excepciones (`ReglaNegocioException`) en vez de devolver bodies manuales. El `GlobalExceptionHandler` las convierte a JSON automáticamente:

```java
if (usuario == null || !passwordEncoder.matches(...)) {
    throw new ReglaNegocioException("Email o contraseña incorrectos");
}
```

Cambia el código HTTP de 401 a 400 (porque `ReglaNegocioException` se mapea a 400), pero eso no es un problema funcional: el frontend no diferenciaba entre 400 y 401 para errores de login, solo lee el campo `mensaje`. Y semánticamente "credenciales mal" está más cerca de "petición incorrecta" que de "no autenticado".

**Verificación:**

```
$ curl -i -X POST /api/auth/login -d '{"email":"x@x.com","password":"WRONG"}'
HTTP/1.1 400
Content-Type: application/json

{"mensaje":"Email o contraseña incorrectos","status":400,"timestamp":"..."}
```

### Bug B5 — Test unitario `ResenaServiceTest.crear_*` roto desde la sesión anterior

**Síntoma:** al ejecutar `./mvnw test` después de los arreglos anteriores, fallaba un único test:

```
ResenaServiceTest.crear_conDatosValidos_guardaYDevuelveResena
NullPointerException: Cannot invoke "EntityManager.refresh(Object)"
because "this.entityManager" is null
```

**Causa raíz:** en una sesión anterior se había añadido el campo `private final EntityManager entityManager` a `ResenaService` (para el `refresh()` documentado como Bug 2/3 en este mismo documento), pero **no se actualizó el test** que usa `@InjectMocks`. Mockito necesita un `@Mock EntityManager` para inyectar; sin él, el campo queda a `null`.

Este bug **ya estaba ahí** antes de empezar la fase 4, simplemente nadie había vuelto a correr los tests.

**Solución:** añadir el mock al test:

```java
@Mock
private EntityManager entityManager;
```

Mockito lo inyecta automáticamente en `ResenaService` por tipo. El test no necesita stubbear `entityManager.refresh(...)` porque es un método `void` y Mockito hace nothing por defecto.

**Verificación:** `./mvnw test` pasa los 38 tests sin errores.

### Tabla resumen de los bugs B1–B5

| Bug | Recurso afectado | Síntoma | Causa raíz | Fix |
|---|---|---|---|---|
| **B1** | `Resena`, `Favorito` | `POST /api/resenas` con token válido devolvía 401 | `spring.jpa.open-in-view=false` cerraba la sesión Hibernate antes de que Jackson serializara los proxies LAZY. Spring Security 7 traduce el fallo de escritura como 401. | `application.properties`: `open-in-view=true` |
| **B2** | `Usuario` (y por arrastre, todas las relaciones) | Respuestas con `usuario.id: null`, `usuario.username: null` y campos `$$_hibernate_interceptor` en el JSON | `@JsonAutoDetect(fieldVisibility=ANY)` en `Usuario` forzaba a Jackson a leer campos en lugar de getters; los proxies de Hibernate solo se inicializan al llamar al getter. | Quitar `@JsonAutoDetect` y añadir `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` en `Usuario`, `Album`, `Artista`, `Resena`, `Favorito`. |
| **B3** | `ResenaService.actualizar` | `PUT /api/resenas/{id}` devolvía siempre los valores **antiguos** | `entityManager.refresh()` antes del flush sobrescribía los cambios en memoria con los datos antiguos de BD. | Quitar `refresh()` en `actualizar`. El dirty checking ya genera el UPDATE al commit. |
| **B4** | `AuthController` | Login con password mal devolvía 401 + `text/plain`. El frontend petaba al hacer `res.json()`. | `AuthController` devolvía `ResponseEntity.status(401).body("...")` con texto plano en vez de delegar en el `GlobalExceptionHandler`. | Lanzar `ReglaNegocioException` desde `AuthController.login`. Cambia el código a 400 pero ahora devuelve JSON uniforme. |
| **B5** | `ResenaServiceTest` | El test `crear_conDatosValidos_guardaYDevuelveResena` reventaba con `NullPointerException: this.entityManager is null` | Se había inyectado `EntityManager` en `ResenaService` pero el test no se actualizó con un `@Mock`. | Añadir `@Mock private EntityManager entityManager;` en el test. |

### Lo que se aprendió

- **Las pruebas Postman iniciales eran insuficientes** porque solo miraban status codes. Faltaba inspeccionar `Content-Type` y el contenido del body JSON.
- **El símbolo del 401 puede engañar.** En Spring Security 7 cualquier excepción durante la escritura de la respuesta acaba en el `authenticationEntryPoint` y devuelve 401 al cliente. Si parece un problema de autenticación, hay que mirar los logs del backend antes de asumirlo.
- **La regla de oro para entidades JPA serializables**: `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` siempre, y NO usar `@JsonAutoDetect(fieldVisibility=ANY)` con relaciones LAZY.
- **`refresh()` después de `save()`** solo es válido en métodos `crear` (donde IDENTITY ya forzó INSERT). En `actualizar` es perjudicial.
- **Cualquier cambio en la firma de un service obliga a revisar sus tests** — añadir un campo inyectado y olvidar el `@Mock` en el test es un error silencioso.

### Verificación tras los arreglos

- `./mvnw test` → 38/38 verdes.
- 6 lotes de pruebas Postman (login + CRUD reseñas + CRUD favoritos + casos de error) → todos OK con `usuario` y `album` poblados, sin basura de Hibernate, sin password filtrado.
