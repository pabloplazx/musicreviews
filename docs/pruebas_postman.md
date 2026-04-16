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
| GET | `/api/albumes` | — | 200 + lista ✅ |
| GET | `/api/albumes/3` | — | 200 + álbum ✅ |
| GET | `/api/albumes?titulo=ok` | — | 200 + lista filtrada ✅ |
| GET | `/api/albumes?artistaId=1` | — | 200 + lista filtrada ✅ |
| GET | `/api/albumes?genero=Rock` | — | 200 + lista filtrada ✅ |
| PUT | `/api/albumes/3` | campos actualizados | 200 ✅ |
| DELETE | `/api/albumes/{id}` | — | 204 ✅ |

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

## Resumen general

| Controlador | Endpoints probados | Bugs encontrados | Estado |
|---|---|---|---|
| ArtistaController | 7 | 0 | ✅ |
| AlbumController | 8 | 0 | ✅ |
| UsuarioController | 7 | 0 | ✅ |
| ResenaController | 8 | 2 | ✅ (corregidos) |
| FavoritoController | 6 | 3 | ✅ (corregidos) |
