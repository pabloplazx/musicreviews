# Diario de bitácora — MusicReviews TFG

Registro cronológico del desarrollo del proyecto.

---

## Semana 1 — Definición del proyecto

**Fecha:** inicio del TFG
**Fase:** FASE 1 — Definir proyecto y bases

- Definido el concepto: aplicación web de reseñas de álbumes musicales, similar a Letterboxd pero para música.
- Elegido el stack tecnológico: Java 21 + Spring Boot (backend), React + Vite (frontend), MySQL en Aiven (BD).
- Creado el repositorio en GitHub: pabloplazx/MusicReviews.
- Diseñado el modelo de datos inicial: Usuario, Artista, Album, Resena, Favorito.
- Creado el diagrama de BD, diagrama de clases y diagrama de casos de uso.
- Entregado el anteproyecto al centro (tutora: Raquel Gómez Sánchez).

---

## Semana 2-3 — Backend: entidades y API REST

**Fase:** FASE 2 — Backend

- Creado el proyecto Spring Boot con Maven.
- Implementadas las 5 entidades JPA: Usuario, Artista, Album, Resena, Favorito.
- Implementados los repositorios con Spring Data JPA.
- Implementada la lógica de negocio en los servicios:
  - `ResenaService`: validación de puntuación (1-5) y control de duplicados.
  - `FavoritoService`: control de duplicados, `EntityManager.refresh()` para cargar relaciones.
  - `UsuarioService`: validación de email y username únicos.
  - `ArtistaService` y `AlbumService`: CRUD completo.
- Implementados los controladores REST con endpoints bajo `/api/`.
- Configurada la conexión a MySQL local inicialmente.

---

## Semana 4 — Integración con Spotify y Last.fm

**Fase:** FASE 2 — Backend (continuación)

- Creada app en Spotify Developer Dashboard.
- Implementado `SpotifyService` con autenticación Client Credentials.
- Implementado `SpotifyController` con endpoints de importación:
  - `GET /api/spotify/importar?artista=` — importa un artista concreto.
  - `GET /api/spotify/importar-lista` — importa lista curada de 108 artistas.
  - `GET /api/spotify/actualizar-metadatos` — rellena géneros (Spotify) y biografías (Last.fm).
  - `GET /api/spotify/actualizar-portadas` — rellena portadas vacías.
- Integrada API de Last.fm para obtener biografías de artistas.
- Exportada playlist de Spotify con Exportify (CSV) al no poder acceder con Client Credentials desde 2024.
- Importados 99 artistas y 469 álbumes a la BD.
- Completados manualmente géneros (UPDATE CASE), países (UPDATE CASE) y algunas biografías.

---

## Semana 5 — Migración a Aiven y autenticación JWT

**Fase:** FASE 2 — Backend (continuación)

- Migrada la BD de MySQL local a **Aiven** (MySQL cloud, plan gratuito permanente).
- Implementado sistema de autenticación con **JWT + BCrypt**:
  - `POST /api/auth/register` — registro con hasheo de contraseña.
  - `POST /api/auth/login` — login con validación BCrypt y generación de token.
  - `JwtFilter` — intercepta todas las peticiones y valida el token.
  - `UserDetailsServiceImpl` — carga el usuario por email para Spring Security.
- Configuradas rutas públicas y protegidas por rol (USER / ADMIN) en `SecurityConfig`.
- Añadidos campos `activo` y `fechaUltimoLogin` a la entidad Usuario.
- Probado todo con Postman: registro, login, rutas protegidas y acceso por roles.

---

## Semana 6 — Refactorización, optimización y tests

**Fase:** FASE 2 — Backend (finalización)

### Refactorización (primera ronda)
- `@Autowired` → `@RequiredArgsConstructor` con campos `final` en todos los servicios y controladores.
- `@Transactional(readOnly = true)` en todos los métodos de lectura.
- `FetchType.LAZY` → `FetchType.EAGER` en `Album.artista` (necesario para serialización JSON).
- Eliminadas queries `findById` redundantes tras `save()`.
- Eliminado `POST /api/usuarios` (creaba usuarios con contraseña en texto plano).
- `AuthController` refactorizado para usar `UsuarioService` en lugar del repositorio directamente.
- `spring.jpa.open-in-view=false` y `spring.jpa.show-sql=false`.

### Funcionalidades añadidas
- `EstadisticasController` con 8 endpoints: resumen, top-albumes, mas-resenados, top-artistas, generos, top-por-genero, actividad-reciente, albumes-recientes.
- Paginación en `AlbumController`: devuelve `Page<Album>`, 12 por página, ordenado por título.
- `@EnableSpringDataWebSupport` en `BackendApplication` para serialización correcta de `Page<T>`.

### Tests unitarios
- Escritos 37 tests unitarios con JUnit 5 + Mockito (sin Spring, sin BD):
  - `ResenaServiceTest` — 11 tests
  - `UsuarioServiceTest` — 7 tests
  - `FavoritoServiceTest` — 7 tests (incluye mock de EntityManager)
  - `ArtistaServiceTest` — 7 tests
  - `EstadisticasServiceTest` — 5 tests
- `BackendApplicationTests` — 1 test de contexto (requiere BD activa).
- **Total: 38 tests — todos passing.**

### Optimizaciones adicionales (segunda ronda)
- `GlobalExceptionHandler` (`@RestControllerAdvice`): manejo centralizado de errores con JSON uniforme `{timestamp, status, mensaje}`.
- Excepciones tipadas: `RecursoNoEncontradoException` (404) y `ReglaNegocioException` (400).
- `@JsonProperty(WRITE_ONLY)` + `@JsonAutoDetect` en `Usuario.password`: el hash BCrypt nunca se expone en respuestas JSON.
- `actualizarUltimoLogin()`: método dedicado en `UsuarioService` — `fechaUltimoLogin` ahora se persiste correctamente en cada login.
- CORS configurado en `SecurityConfig` para `http://localhost:5173` (Vite).
- Constructor injection en `JwtFilter` y `UserDetailsServiceImpl`.
- Script `database/seed_data.py`: carga 5 usuarios, 30 reseñas y 25 favoritos vía API.

### Documentación generada
- `backend/README.md` — API REST completa.
- `docs/pruebas_postman.md` — registro de pruebas.
- `docs/migracion_aiven.md` — proceso de migración.
- `docs/seguridad_autenticacion.md` — JWT, BCrypt, CORS.
- `docs/refactorizacion_backend.md` — dos rondas de optimizaciones.
- `docs/tests_unitarios.md` — documentación completa de los 38 tests.
- `docs/importacion/proceso_importacion.md` — proceso de importación desde Spotify.

**Estado al finalizar FASE 2:** backend 100% completo. ✅

---

## Próxima fase

**FASE 3 — Frontend React + Vite** (inicio previsto: 20/04/2026)

Objetivo: crear la interfaz de usuario conectada al backend ya desarrollado.

Páginas planificadas:
- Home — álbumes destacados y actividad reciente
- Explorar — búsqueda y filtrado de álbumes con paginación
- Detalle de álbum — información, reseñas y botón de favorito
- Perfil de usuario — reseñas y favoritos del usuario
- Login / Registro
- Rankings / Estadísticas

Stack: React + Vite, con decisiones de librerías pendientes de definir.

---

## Semana 7 — Inicio del frontend (20/04/2026)

**Fase:** FASE 3 — Frontend React + Vite

### Puesta a punto del entorno

- Node.js v24.14.1 y npm 11 ya instalados.
- Creado el proyecto frontend en `C:\Users\plaza\Desktop\musicreviews-frontend` con:
  ```bash
  npm create vite@latest musicreviews-frontend -- --template react
  ```
- Instalado Tailwind CSS v4 con el plugin oficial para Vite:
  ```bash
  npm install -D tailwindcss @tailwindcss/vite
  ```
- Configurado `vite.config.js` con el plugin de Tailwind.
- `src/index.css` reducido a `@import "tailwindcss"` (sintaxis de Tailwind v4).
- Verificado que Tailwind funciona: fondo negro + texto blanco con clases de utilidad.

### Decisiones de diseño tomadas

- **Estilo:** oscuro (dark mode) — referencia principal Letterboxd.
- **Tipografía:** Bebas Neue (títulos) + Inter (texto general) — máximo 2 fuentes.
- **Librería de estilos:** Tailwind CSS — clases de utilidad directamente en JSX.
- **Iconos:** Heroicons (pendiente de instalar).
- **Paleta de colores:** pendiente de definir en Realtime Colors.
- **Diseño Figma:** pendiente — se diseñará antes de codificar las pantallas.

### Herramientas de diseño seleccionadas

| Herramienta | Uso |
|---|---|
| Figma | Diseño de pantallas y design system |
| Realtime Colors | Definir y previsualizar la paleta de colores |
| Google Fonts | Elegir tipografías (Bebas Neue + Inter) |
| Heroicons | Iconos SVG para React |

### Estructura de carpetas creada

```
src/
├── components/
│   ├── ui/        ← componentes reutilizables (botones, tarjetas, badges...)
│   └── layout/    ← navbar, footer, estructura de página
├── pages/         ← cada pantalla de la app
├── services/      ← llamadas al backend (API REST)
├── hooks/         ← lógica reutilizable entre componentes
├── context/       ← datos globales (usuario logueado, token JWT)
├── assets/
│   ├── fonts/     ← tipografías
│   └── images/    ← imágenes estáticas
├── App.jsx
└── index.css
```

### Pasos previos antes de codificar (en orden)

1. Definir paleta de colores en Realtime Colors
2. Elegir tipografías en Google Fonts
3. Crear Design System en Figma (paleta + tipografías + componentes base)
4. Diseñar las 6 pantallas en Figma usando esos componentes

### Estado actual

- Entorno configurado y funcionando ✅
- Estructura de carpetas creada ✅
- Diseño Figma pendiente ⏳

---

## Semana 7 — Corrección de la importación desde Spotify (21/04/2026)

**Fase:** vuelta puntual a FASE 2 para arreglar regresiones detectadas al intentar añadir artistas nuevos.

### Punto de partida
- 469 álbumes / 99 artistas en Aiven.
- `GET /api/spotify/importar?artista=...` devolvía 500 con error `400 Invalid limit` de Spotify.

### Problemas detectados y solucionados

1. **Regresión del parámetro `limit=50`** (Bug 4 en `pruebas_postman.md`). Ya había sido corregido en Semana 4 pero se había reintroducido. Eliminado de nuevo y comentario añadido en el código para evitar que vuelva a aparecer.
2. **Falta de paginación en `/v1/artists/{id}/albums`** (Bug 5). Detectado al añadir el endpoint nuevo `/api/spotify/comprobar`: Radiohead tenía 6 álbumes en BD pero Spotify listaba 15. `importarArtistaPorId` solo leía la primera página. Solucionado siguiendo el campo `next` igual que `importarDesdePlaylist`.
3. **Mensaje confuso "0 álbumes nuevos"** (Bug 3 revisado). La solución anterior cortocircuitaba Spotify si el artista ya existía, impidiendo detectar lanzamientos nuevos. Sustituida por 3 mensajes diferenciados: `Importado` / `Actualizado` / `ya está al día`.
4. **`spotifyGet` se quedaba dormido 24 h ante cuota diaria** (Bug 6). El método respetaba ciegamente `Retry-After`; al agotar la cuota diaria Spotify responde con ≈86400 s. Añadido tope `MAX_ESPERA_RATE_LIMIT = 300 s` con abort explícito.

### Endpoints nuevos

- **`GET /api/spotify/comprobar?artista=`** — diagnóstico puro, no modifica BD. Responde `{artista, totalEnBd, totalEnSpotify, faltan[]}`.
- **`GET /api/spotify/completar-todos`** — recorre todos los artistas de la BD y añade los álbumes que faltaban en Spotify (aprovechando la paginación nueva). No crea artistas nuevos.

### Ejecución y resultado

- `/completar-todos` procesó 26 de los 99 artistas antes de que Spotify devolviera el 429 con cuota diaria agotada. **+204 álbumes nuevos añadidos**. Artistas que más contribuyeron: Pink Floyd (+20), The Beatles (+16), Yung Beef (+16), $uicideboy$ (+16), Kanye West (+14), Drake (+13), Mac DeMarco (+12), The Weeknd (+10), Radiohead (+9), Tyler The Creator (+8), Daft Punk (+8).
- Detectados 3 artistas "zombie" con 0 álbumes (Mitski, Japanese Breakfast, SX3) — restos de intentos previos cuando el bug del `limit=50` cortaba la importación después de crear el artista pero antes de guardar álbumes. SX3 eliminado por ser un match erróneo. Mitski y Japanese Breakfast pendientes de repoblar con el nuevo flujo.

### Estado final del día

- 673 álbumes / 102 artistas.
- Spotify bloqueado por cuota diaria hasta el día siguiente.
- Pendientes: repoblar Mitski y Japanese Breakfast; completar los 73 artistas restantes de `/completar-todos` (probablemente en 2-3 sesiones por la cuota diaria).

### Documentación actualizada

- `backend/backend/src/main/java/com/musicreviews/backend/README.md` — nuevos endpoints y métodos del `SpotifyService`.
- `docs/pruebas_postman.md` — Bugs 4, 5, 6 y nuevas filas de pruebas.
- `docs/importacion/proceso_importacion.md` — tabla de problemas ampliada y nuevos endpoints.
