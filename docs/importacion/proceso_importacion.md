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

El backend expone tres endpoints de importación en `/api/spotify/`:

| Endpoint | Descripción |
|---|---|
| `GET /api/spotify/importar?artista=Radiohead` | Importa un artista concreto por nombre |
| `GET /api/spotify/importar-lista` | Importa la lista completa de 108 artistas curados |
| `GET /api/spotify/importar-playlist?id=...` | Importa artistas desde una playlist pública *(requiere OAuth desde 2024)* |

### Flujo interno de `importar-lista`

1. Para cada artista de la lista, comprueba primero en la BD si ya existe con álbumes → si sí, lo salta sin llamar a Spotify
2. Si no existe o existe sin álbumes, busca el artista en Spotify (`/v1/search`)
3. Obtiene los datos completos del artista (`/v1/artists/{id}`)
4. Obtiene sus álbumes de estudio (`/v1/artists/{id}/albums?include_groups=album`)
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

---

## Resultado final

- **~95 artistas** importados con sus álbumes de estudio
- **~440 álbumes** disponibles en la base de datos
- Datos incluyen: nombre, foto, género (artista) y título, portada, fecha de lanzamiento, género (álbum)
- Campos pendientes de completar manualmente: biografía del artista, descripción del álbum
