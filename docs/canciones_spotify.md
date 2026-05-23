# Listado de canciones desde Spotify

**Fecha:** 2026-05-23  
**Alcance:** Backend Spring Boot + Frontend React (MusicReviews)

---

## Descripción

Cada página de detalle de álbum muestra ahora la lista completa de canciones obtenida en tiempo real desde la API de Spotify. El sistema almacena el ID de Spotify de cada álbum para evitar búsquedas repetidas.

---

## Cambios en el backend

### `model/Album.java`
Campo nuevo `spotifyId` (`VARCHAR(50)`, columna `spotify_id`). Hibernate lo crea automáticamente en la BD con `ddl-auto=update` al arrancar. No necesita migración manual.

### `service/SpotifyService.java`

**`importarArtistaPorId()`** — guarda ya el `spotifyId` de cada álbum en el momento de la importación (`albumSpotify.get("id")`).

**`getCanciones(Long albumId)`** — método nuevo. Flujo:
1. Carga el álbum por ID.
2. Si no tiene `spotifyId`, busca en Spotify por `título + nombre artista`, guarda el ID y continúa.
3. Llama a `GET /v1/albums/{spotifyId}/tracks?limit=50`.
4. Devuelve un `Map` con dos claves:
   - `tracks` — lista de objetos `{ numero, nombre, duracionMs, spotifyUrl }`.
   - `albumUrl` — enlace directo al álbum en `open.spotify.com`.

Esto garantiza que **todos los álbumes ya existentes** funcionan sin necesidad de migración: el ID se resuelve automáticamente en la primera petición y queda guardado para las siguientes.

**`actualizarSpotifyIds()`** — método auxiliar de backfill. Recorre todos los álbumes sin `spotifyId` y los rellena buscando por título + artista. Endpoint de administración opcional; no es necesario para el funcionamiento normal.

### `controller/AlbumController.java`
Nuevo endpoint público:

```
GET /api/albumes/{id}/canciones
```

Cubierto por el `permitAll()` existente para `GET /api/albumes/**`. No requiere autenticación.

Respuesta:
```json
{
  "tracks": [
    { "numero": 1, "nombre": "Paranoid Android", "duracionMs": 383893, "spotifyUrl": "https://open.spotify.com/track/..." },
    ...
  ],
  "albumUrl": "https://open.spotify.com/album/..."
}
```

### `controller/SpotifyController.java`
Nuevo endpoint de administración (requiere `ROLE_ADMIN`):

```
GET /api/spotify/actualizar-ids
```

Rellena el `spotifyId` de todos los álbumes que lo tengan vacío. Útil como operación puntual de mantenimiento.

---

## Cambios en el frontend

### `services/albumes.js`
Función nueva `getCancionesAlbum(id)` — llama a `GET /api/albumes/{id}/canciones` y devuelve el objeto `{ tracks, albumUrl }`.

### `pages/DetalleAlbum.jsx`

**Sección de canciones** — aparece entre el header del álbum y las reseñas. Muestra:
- Número de pista (gris, se ilumina en verde al hover).
- Nombre de la canción (texto principal, transición a verde al hover).
- Duración en formato `M:SS`.
- Icono SVG de Spotify casi transparente por defecto; se vuelve verde Spotify (`#1DB954`) al hacer hover sobre la fila. Abre la canción en Spotify en pestaña nueva.

**Banner Spotify** — aparece al pie de la lista de canciones. Diseño glassmorphism con fondo verde sutil, icono circular con glow verde, nombre del álbum y flecha animada. Abre el álbum completo en Spotify.

**Scroll al top** — `useEffect` adicional con dependencia `[id]` que hace `window.scrollTo({ top: 0, behavior: "instant" })` al navegar a un álbum distinto. Evita que el usuario aparezca a mitad de página al llegar desde el catálogo.

---

## Comportamiento para álbumes ya importados

| Situación | Primer acceso | Accesos siguientes |
|---|---|---|
| Álbum importado tras este cambio | Usa `spotifyId` guardado directamente | Igual |
| Álbum importado antes de este cambio | Busca en Spotify (≈1-2 s extra), guarda el ID | Usa `spotifyId` guardado |
| Álbum no encontrado en Spotify | Devuelve lista vacía, no muestra sección | Igual |

---

## Dependencias externas

- API de Spotify (client credentials flow, ya configurado en `application.properties`).
- No se requiere ninguna nueva dependencia en `pom.xml` ni en `package.json`.
