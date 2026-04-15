# MusicReviews - Backend

API REST desarrollada con **Java 21 + Spring Boot 3.4.4 + Maven**.
Base de datos: **MySQL** (BD: `musicreviews`, usuario: `root`, pass: `1234`, puerto: `8080`).

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
└── SecurityConfig  ← Configuración de seguridad (JWT pendiente)
```

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

| Método | Descripción |
|---|---|
| `findByTituloContainingIgnoreCase(titulo)` | Búsqueda de álbumes por título (buscador, ignora mayúsculas) |
| `findByArtistaId(artistaId)` | Obtener todos los álbumes de un artista |
| `findByGeneroIgnoreCase(genero)` | Filtrar álbumes por género |

---

### ResenaRepository ✅
Extiende `JpaRepository<Resena, Long>`.

| Método | Descripción |
|---|---|
| `findByAlbumId(albumId)` | Obtener todas las reseñas de un álbum |
| `findByUsuarioId(usuarioId)` | Obtener todas las reseñas de un usuario |
| `findByUsuarioIdAndAlbumId(usuarioId, albumId)` | Obtener la reseña de un usuario sobre un álbum concreto |
| `existsByUsuarioIdAndAlbumId(usuarioId, albumId)` | Comprobar si un usuario ya ha reseñado ese álbum |

---

### FavoritoRepository ✅
Extiende `JpaRepository<Favorito, Long>`.

| Método | Descripción |
|---|---|
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

| Método | Descripción |
|---|---|
| `obtenerTodos()` | Devuelve todos los álbumes |
| `obtenerPorId(id)` | Busca un álbum por ID |
| `buscarPorTitulo(titulo)` | Búsqueda parcial e insensible a mayúsculas |
| `obtenerPorArtista(artistaId)` | Filtra álbumes por artista |
| `obtenerPorGenero(genero)` | Filtra álbumes por género |
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

### FavoritoService ✅

| Método | Descripción |
|---|---|
| `obtenerPorUsuario(usuarioId)` | Devuelve todos los favoritos de un usuario |
| `esFavorito(usuarioId, albumId)` | Comprueba si un álbum ya es favorito del usuario |
| `agregar(favorito)` | Añade un álbum a favoritos. Lanza error si ya estaba añadido |
| `eliminar(usuarioId, albumId)` | Quita un álbum de favoritos. Lanza error si no estaba. Usa `@Transactional` |

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
| GET | `/api/albumes` | Lista todos los álbumes. Con `?titulo=`, `?genero=` o `?artistaId=` filtra | 200 + lista |
| GET | `/api/albumes/{id}` | Busca un álbum por ID | 200 o 404 |
| POST | `/api/albumes` | Crea un álbum nuevo | 200 |
| PUT | `/api/albumes/{id}` | Actualiza todos los campos del álbum | 200 o 404 |
| DELETE | `/api/albumes/{id}` | Elimina un álbum | 204 o 404 |

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
