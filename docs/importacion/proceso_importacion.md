# Proceso de importación de artistas y álbumes desde Spotify

Este documento explica paso a paso cómo se pobló la base de datos de MusicReviews con artistas y álbumes reales usando la API de Spotify.

---

## Resumen del proceso

```
Playlist de Spotify
       │
       ▼
Exportify (CSV)
       │
       ▼
Scripts PowerShell → Lista de artistas top
       │
       ▼
API de Spotify (Client Credentials)
       │
       ▼
Backend Spring Boot (SpotifyService)
       │
       ▼
Base de datos MySQL
```

---

## Paso 1 — Crear la app en Spotify Developer

1. Acceder a **developer.spotify.com/dashboard**
2. Crear una nueva app con:
   - Redirect URI: `http://127.0.0.1:8080`
   - API: Web API
3. Anotar el **Client ID** y **Client Secret**
4. Añadirlos a `application.properties`:
   ```properties
   spotify.client-id=TU_CLIENT_ID
   spotify.client-secret=TU_CLIENT_SECRET
   ```

> **Importante:** `application.properties` está en el `.gitignore` — las credenciales nunca se suben a GitHub.

---

## Paso 2 — Exportar la playlist

La API de Spotify con Client Credentials **no permite acceder a playlists** (ni públicas ni privadas) desde 2024. Para obtener los artistas de una playlist se usó la herramienta externa **Exportify** (exportify.net), que genera un CSV con todas las canciones.

El CSV contiene una columna `Artist Name(s)` con los artistas de cada canción separados por `;`.

---

## Paso 3 — Filtrar los artistas más relevantes

Con los scripts PowerShell de esta carpeta se procesó el CSV:

```powershell
# Extraer todos los artistas únicos
.\extraer_artistas.ps1   → artistas.txt (1.391 artistas únicos)

# Extraer los 80 artistas más escuchados (los que más canciones tienen en la playlist)
.\top_artistas.ps1       → top_artistas.txt
```

Se seleccionaron los artistas más populares de la playlist y se combinaron con una lista curada de **clásicos universales** (The Beatles, Pink Floyd, Radiohead, etc.) y **artistas españoles relevantes** (Vetusta Morla, Extremoduro, etc.).

La lista final tiene **108 artistas**.

---

## Paso 4 — Importar desde el backend

El backend expone varios endpoints de importación y diagnóstico en `/api/spotify/`:

| Endpoint | Descripción |
|---|---|
| `GET /api/spotify/importar?artista=Radiohead` | Importa un artista concreto por nombre. Diferencia alta, actualización y "ya al día" en el mensaje |
| `GET /api/spotify/importar-lista` | Importa la lista completa de 108 artistas curados |
| `GET /api/spotify/importar-playlist?id=...` | Importa artistas desde una playlist pública *(requiere OAuth desde 2024)* |
| `GET /api/spotify/comprobar?artista=Radiohead` | **Diagnóstico** (no modifica BD): compara álbumes del artista en BD vs Spotify y devuelve los que faltan |
| `GET /api/spotify/completar-todos` | Recorre todos los artistas de la BD y añade los álbumes que faltaran en Spotify |
| `GET /api/spotify/actualizar-metadatos` | Rellena género (Spotify) y biografía (Last.fm) de artistas que los tengan vacíos |
| `GET /api/spotify/actualizar-portadas` | Busca en Spotify la portada de cada álbum que la tenga vacía y la actualiza |

### Flujo interno de `importar-lista`

1. Para cada artista de la lista, comprueba primero en la BD si ya existe con álbumes → si sí, lo salta sin llamar a Spotify
2. Si no existe o existe sin álbumes, busca el artista en Spotify (`/v1/search`)
3. Obtiene los datos completos del artista (`/v1/artists/{id}`)
4. Obtiene sus álbumes de estudio (`/v1/artists/{id}/albums?include_groups=album`) **paginando por el campo `next`** hasta agotar todas las páginas (añadido el 21/04/2026 — ver Bug "Paginación" abajo)
5. Guarda el artista y sus álbumes en MySQL
6. Espera 500ms entre artistas para no superar el límite de Spotify

### Autenticación con Spotify (Client Credentials)

```
Backend → POST https://accounts.spotify.com/api/token
          (Basic Auth con ClientID:ClientSecret)
        ← access_token (válido 1 hora)

Backend → GET https://api.spotify.com/v1/...
          (Authorization: Bearer <token>)
```

---

## Problemas encontrados y soluciones

| Problema | Causa | Solución |
|---|---|---|
| `403 Forbidden` al leer playlist | Spotify retiró el acceso a playlists con Client Credentials en 2024 | Usar Exportify para exportar el CSV |
| `400 Invalid limit` al obtener álbumes | El parámetro `limit` daba error en la versión actual de la API | Eliminar el parámetro `limit` (Spotify usa 20 por defecto) |
| `429 Too Many Requests` | Demasiadas peticiones seguidas a la API | Añadir delay de 500ms entre cada artista |
| Artistas guardados sin álbumes | Rate limit cortaba la importación a mitad | Lógica que detecta artistas sin álbumes y reintenta solo esos |
| Duplicados al reimportar | Al relanzar el endpoint se volvían a guardar artistas ya existentes | Comprobar en BD antes de llamar a Spotify |
| **[21/04/2026] Regresión `400 Invalid limit`** | En algún punto se reintrodujo `limit=50` en `/v1/artists/{id}/albums` | Volver a eliminar el parámetro y añadir comentario en el código para que no se re-introduzca |
| **[21/04/2026] Álbumes perdidos por falta de paginación** | `importarArtistaPorId` solo leía la primera página; Spotify divide la discografía en varias páginas incluso por debajo del default de 20. Radiohead tenía 6/15 álbumes en BD | Seguir el campo `next` hasta `null` en el bucle de álbumes (mismo patrón que ya usaba `importarDesdePlaylist`) |
| **[21/04/2026] Mensaje "0 álbumes nuevos" sin contexto** | `importarArtista` devolvía el mismo mensaje tanto si el artista era nuevo como si estaba al día | Diferenciar 3 mensajes: `"Importado: X con N álbumes"`, `"Actualizado: X con N álbumes nuevos"`, `"X ya está al día. No hay álbumes nuevos en Spotify"` |
| **[21/04/2026] `spotifyGet` dormía 24h ante cuota diaria** | El header `Retry-After` puede valer ≈86400s cuando se agota la cuota diaria; el método lo respetaba ciegamente | Añadir `MAX_ESPERA_RATE_LIMIT = 300s`. Por encima de ese valor se aborta con mensaje explícito |

---

## Paso 5 — Completar metadatos con Last.fm

Después de la importación inicial, muchos artistas tenían el campo `biografia` vacío (Spotify no proporciona biografías). Para rellenarlo se integró la API de **Last.fm**.

### Configuración

1. Crear una cuenta en **last.fm/api** y obtener una API Key
2. Añadirla a `application.properties`:
   ```properties
   lastfm.api-key=TU_API_KEY
   ```

### Endpoint

```
GET /api/spotify/actualizar-metadatos
```

Este endpoint actualiza en un solo paso tanto el género (desde Spotify) como la biografía (desde Last.fm) de todos los artistas que tengan esos campos vacíos. También propaga el género actualizado a los álbumes del artista que no lo tengan.

### Flujo interno

1. Obtiene todos los artistas de la BD
2. Para cada artista, si no tiene género:
   - Busca el artista en Spotify (`/v1/search`) con el nombre exacto
   - Si Spotify devuelve géneros, toma el primero y lo asigna al artista y a sus álbumes sin género
3. Para cada artista, si no tiene biografía:
   - Llama a Last.fm: `GET https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=<nombre>&api_key=<key>&format=json`
   - Extrae el campo `artist.bio.summary` de la respuesta
   - Limpia el HTML que Last.fm incluye al final del texto (enlace `<a href...>`)
   - Guarda la biografía limpia en la BD
4. Espera 300ms entre cada petición a Spotify y entre cada petición a Last.fm para no superar los límites

### Autenticación con Last.fm

Last.fm usa autenticación por API Key en la query string, sin OAuth ni tokens:

```
GET https://ws.audioscrobbler.com/2.0/
    ?method=artist.getinfo
    &artist=Radiohead
    &api_key=TU_API_KEY
    &format=json
```

No requiere ningún flujo de autenticación previo — la API Key se pasa directamente en cada petición.

### Resultado

- Géneros completados para los artistas que Spotify no había devuelto en la importación inicial
- Biografías rellenadas para todos los artistas que Last.fm conoce
- Los artistas muy de nicho o recientes que Last.fm no tiene registrados quedan sin biografía

### Géneros asignados manualmente

La mayoría de artistas de la lista (~90) tenían `genero = NULL` tras la importación, ya que Spotify no devuelve géneros para muchos artistas en su API de búsqueda. Tras ejecutar `actualizar-metadatos` solo se completaron 5 artistas automáticamente.

El resto se completó con un único `UPDATE ... CASE` directamente sobre la BD, asignando géneros conocidos a cada artista por su `id`. También se propagó el género a los álbumes de cada artista que lo tuvieran vacío:

```sql
UPDATE album a
JOIN artista ar ON a.artista_id = ar.id
SET a.genero = ar.genero
WHERE a.id > 0
  AND (a.genero IS NULL OR a.genero = '')
  AND ar.genero IS NOT NULL;
-- 437 álbumes actualizados
```

### País asignado manualmente

El campo `pais` no lo rellena ni Spotify ni Last.fm. Los 98 artistas de la base de datos se completaron con un único `UPDATE ... CASE` asignando el país de origen de cada artista por su `id`:

```sql
UPDATE artista SET pais = CASE id
    WHEN 9  THEN 'Reino Unido'  -- The Beatles
    WHEN 8  THEN 'Reino Unido'  -- Radiohead
    -- ... (98 artistas en total)
END
WHERE id IN (...);
-- 98 artistas actualizados
```

---

## Paso 6 — Limpieza y corrección de datos

Tras la importación completa se realizó una revisión exhaustiva de la base de datos para detectar y corregir inconsistencias.

### Artistas duplicados

La reimportación de algunos artistas generó duplicados. Se identificaron con:

```sql
SELECT nombre, COUNT(*) FROM artista GROUP BY nombre HAVING COUNT(*) > 1;
```

Se conservó el registro con datos reales de Spotify (foto, género) y se eliminaron los duplicados, reasignando previamente sus álbumes al id correcto:

```sql
UPDATE album SET artista_id = 8 WHERE artista_id IN (1, 32);  -- Radiohead
UPDATE album SET artista_id = 9 WHERE artista_id = 45;        -- The Beatles
DELETE FROM artista WHERE id IN (1, 32, 45);
```

### Álbumes duplicados

Los mismos artistas tenían álbumes duplicados (ids 116-120 y 177-181). Se eliminaron directamente:

```sql
DELETE FROM album WHERE id IN (116, 117, 118, 119, 120, 177, 178, 179, 180, 181);
```

### Portadas de álbumes vacías

31 álbumes (principalmente de artistas españoles y Prince) no tenían portada tras la importación. Se añadió el endpoint `GET /api/spotify/actualizar-portadas` que busca cada álbum sin portada en Spotify por `"título artista"` y actualiza la imagen. Actualizó los 31 álbumes correctamente.

### Biografías en idioma incorrecto

Last.fm devolvió algunas biografías en español para artistas españoles y latinos. Se reescribieron manualmente en inglés para mantener consistencia con el resto:

- Bunbury, Fito y Fitipaldis, Carolina Durante, Duki, Eladio Carrion, Canserbero, Leiva, Supersubmarina

Además se corrigieron dos biografías con contenido erróneo que Last.fm había devuelto:
- **Nirvana**: Last.fm devolvía el texto de desambiguación de la página en lugar de la biografía real.
- **JAY-Z**: La biografía hacía referencia únicamente al cambio de nombre del artista.

### Query de verificación final

```sql
SELECT 
    (SELECT COUNT(*) FROM artista WHERE foto      IS NULL OR foto      = '') AS artistas_sin_foto,
    (SELECT COUNT(*) FROM artista WHERE genero    IS NULL OR genero    = '') AS artistas_sin_genero,
    (SELECT COUNT(*) FROM artista WHERE pais      IS NULL OR pais      = '') AS artistas_sin_pais,
    (SELECT COUNT(*) FROM artista WHERE biografia IS NULL OR biografia = '') AS artistas_sin_biografia,
    (SELECT COUNT(*) FROM album   WHERE portada   IS NULL OR portada   = '') AS albumes_sin_portada,
    (SELECT COUNT(*) FROM album   WHERE genero    IS NULL OR genero    = '') AS albumes_sin_genero,
    (SELECT COUNT(*) FROM album   WHERE artista_id NOT IN (SELECT id FROM artista)) AS albumes_huerfanos,
    (SELECT COUNT(*) FROM artista a WHERE (SELECT COUNT(*) FROM artista b WHERE b.nombre = a.nombre) > 1) AS artistas_duplicados,
    (SELECT COUNT(*) FROM album   a WHERE (SELECT COUNT(*) FROM album   b WHERE b.titulo = a.titulo AND b.artista_id = a.artista_id) > 1) AS albumes_duplicados;
```

Resultado final: todos los campos a 0 excepto `artistas_sin_biografia` = 0.

---

## Resultado final

- **~98 artistas** importados con sus álbumes de estudio
- **~440 álbumes** disponibles en la base de datos
- Datos incluyen: nombre, foto, género, país (artista) y título, portada, fecha de lanzamiento, género (álbum)
- Biografías de artista completadas vía Last.fm
- Campos pendientes de completar manualmente: descripción de álbum
