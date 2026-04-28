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

---

## Semana 8 — Frontend: páginas principales y componentes UI (26/04/2026)

**Fase:** FASE 3 — Frontend React + Vite (continuación)

### Finalización de la página Inicio

- Completada la sección CTA "¿Listo para compartir tu opinión musical?" con fondo `surface` (`#16261E`), título, botón "Crear cuenta gratis" enlazado a `/registro` y link secundario "¿Ya tienes cuenta?" enlazado a `/login`.
- Creado `Footer.jsx` (`src/components/layout/`) con copyright a la izquierda y links de navegación a la derecha. Integrado en `App.jsx` para que aparezca en todas las páginas.

### Repositorio frontend independiente

- Creado repositorio privado `pabloplazx/musicreviews-frontend` en GitHub mediante `gh repo create`.
- Primer commit con los 35 archivos del proyecto (código, design system, componentes).
- Segundo commit con `README.md` profesional: descripción del proyecto, stack, estructura de carpetas, design system (colores y tipografía), tabla de rutas e instrucciones de instalación.

### Página Login

- Creado `FormInput.jsx` (`src/components/ui/`) — componente reutilizable de campo de formulario con label, input y soporte de estado de error (prop `error={true}` activa borde rojo). Reutilizado en Login y Registro.
- Implementado `Login.jsx` con card centrada verticalmente, cabecera con logo y subtítulo, campos de correo y contraseña, link "¿Olvidaste tu contraseña?", botón "Iniciar sesión" y link a registro. Banner de error preparado para conectar al backend (controlado por variable `error`).

### Página Registro

- Implementado `Registro.jsx` reutilizando `FormInput`. Cuatro campos: nombre de usuario, correo electrónico, contraseña y confirmar contraseña. Estructura idéntica al Login, sin link de contraseña olvidada.

### Componentes UI reutilizables (Catálogo y Búsqueda)

Antes de montar las páginas se extrajeron todos los elementos reutilizables:

| Componente | Ruta | Descripción |
|---|---|---|
| `GenreChip` | `src/components/ui/` | Chip de género/filtro con estado activo (verde) e inactivo. Props: `label`, `active`, `onClick` |
| `CatalogoCard` | `src/components/ui/` | Tarjeta de álbum con portada, badge de género opcional (top-right), nombre, artista y rating numérico |
| `SearchBar` | `src/components/ui/` | Barra de búsqueda con icono SVG de lupa, borde verde al focus. Props: `placeholder`, `value`, `onChange` |
| `SelectOrden` | `src/components/ui/` | Selector `<select>` estilizado con el design system. Props: `opciones` (array `{value, label}`), `value`, `onChange` |
| `Paginacion` | `src/components/ui/` | Paginación con flechas anterior/siguiente, páginas cercanas y `...` para saltos. Props: `paginaActual`, `totalPaginas`, `onPageChange` |
| `AlbumRow` | `src/components/ui/` | Fila compacta horizontal con portada pequeña y "Álbum — Artista". Usada en sección "Añadidos recientemente" de Búsqueda |

### Página Catálogo

- Implementado `Catalogo.jsx` con datos hardcodeados (10 álbumes). Funcionalidades operativas:
  - **Filtrado por género** mediante `GenreChip` — resetea la página al cambiar.
  - **Búsqueda** por nombre de álbum o artista mediante `SearchBar` — resetea la página al escribir.
  - **Ordenación** funcional: "Mejor valorados" (por rating descendente) y "A → Z" (alfabético). "Más recientes" reservado para cuando el backend devuelva fechas.
  - **Paginación** con `Paginacion` — 10 álbumes por página.
  - **Estado vacío** cuando ningún álbum coincide con el filtro o la búsqueda.

### Página Búsqueda

- Creado `Busqueda.jsx` con ruta `/busqueda` registrada en `App.jsx`.
- Comportamiento por estados:
  - **Filtro "Todo" + sin texto** → muestra secciones "Tendencias" (4 tarjetas `CatalogoCard` sin badge de género) y "Añadidos recientemente" (3 filas `AlbumRow`).
  - **Filtro distinto de "Todo" + sin texto** → pantalla vacía (el usuario debe escribir algo).
  - **Cualquier texto escrito** → muestra estado "Sin resultados" con el término buscado. Cuando el backend esté listo, este bloque se condicionará a que la API devuelva array vacío.

### Navbar: acceso a Búsqueda

- Añadido icono SVG de lupa en el Navbar entre los links de navegación y los botones de acción, enlazado a `/busqueda`. Color `text-muted` en reposo, `text-primary` en hover.

### Decisiones técnicas destacadas

- El badge de género en `CatalogoCard` es opcional (`{genero && ...}`) — permite reutilizar la misma card en Búsqueda (Tendencias) sin badge.
- La ordenación en Catálogo usa `[...albumsFiltrados].sort()` — copia del array para no mutar el estado.
- `focus-within:border-primary` en `SearchBar` — el borde se pone verde al hacer click en cualquier parte del contenedor, no solo en el input.

### Estado al finalizar la sesión

- Páginas completadas: Inicio ✅, Login ✅, Registro ✅, Catálogo ✅, Búsqueda ✅
- Páginas pendientes: Rankings, Detalle de álbum, Perfil de usuario, Detalle de artista, Estadísticas, Crear/Editar reseña, Mis favoritos, Panel Admin, 404
- Componentes UI creados en total: 11 (`SectionTitle`, `PortadaPlaceholder`, `Estrellas`, `ResenaCard`, `AlbumCard`, `FormInput`, `GenreChip`, `CatalogoCard`, `SearchBar`, `SelectOrden`, `Paginacion`, `AlbumRow`)

---

## Semana 9 — Frontend: Rankings, Detalles y Crear Reseña (27/04/2026)

**Fase:** FASE 3 — Frontend React + Vite (continuación)

### Metodología de trabajo adoptada

A partir de esta sesión se trabaja conectando directamente con **Figma Desktop Bridge MCP** para consultar el diseño real antes de implementar cada pantalla. Esto garantiza fidelidad pixel a pixel con el prototipo.

### Página Rankings — `/rankings`

- Implementado `Rankings.jsx` fiel al diseño Figma (node `32:332`, página "Estadisticas").
- Estructura:
  - **4 stat cards** en fila: Álbumes (1.2k), Artistas (340), Reseñas (8.4k), Usuarios (521).
  - **2 columnas**: Top Álbumes (lista de filas con posición, portada, artista, rating) + Por género (barras de progreso proporcionales calculadas sobre el máximo).
  - **2 columnas**: Top Artistas (filas con `Link` a `/artista/:id`) + Actividad reciente.
- Todos los datos son mock (arrays hardcodeados). Se reemplazarán con `fetch` al backend cuando esté conectado.

### Página Detalle de Álbum — `/album/:id`

- Creado `DetalleAlbum.jsx`. Estructura:
  - **Header** (`bg-card`): portada `aspect-square` con `PortadaPlaceholder`, chip de género, título, artista enlazado a `/artista/1` (hover `text-primary`), año, rating con estrellas + conteo, dos botones de acción.
  - **Botón "Añadir a favoritos"** con toggle local: alterna entre `♡ Añadir a favoritos` (outline) y `♥ En favoritos` (fondo `primary/10`). Listo para conectar a `POST /api/favoritos`.
  - **Reseñas**: lista con avatar circular (inicial del usuario, `bg-primary`), nombre, fecha, estrellas y texto.
  - **Estado vacío** preparado: condicionado a `RESENAS.length > 0`. Al conectar backend se condicionará a que la API devuelva array vacío.
  - **"Más de [Artista]"**: grid 4 columnas, cada álbum es un `Link` a `/album/:id` con efecto hover.
- Ruta: `/album/:id` registrada en `App.jsx`. El `id` se leerá con `useParams()` al conectar el backend.

### Página Detalle de Artista — `/artista/:id`

- Creado `DetalleArtista.jsx`. Estructura:
  - **Header**: avatar circular con borde `border-primary`, nombre, **botón "Seguir artista"** con toggle local (outline → relleno verde + "✓ Siguiendo").
  - **Stats** en una card horizontal con divisores: Media★, Álbumes, Reseñas.
  - **Biografía**: párrafo de texto con `leading-relaxed`.
  - **Discografía**: grid 5 columnas con portada `aspect-square`, título, año y rating. Cada ítem es `Link` a `/album/:id`.
  - **Reseñas recientes**: grid 2 columnas con avatar, usuario, estrellas, título en negrita, texto y fecha. Enlace "Ver todas →" en la cabecera de sección.
- Ruta: `/artista/:id` registrada en `App.jsx`.

### Página Crear Reseña — `/crear-resena`

- Creado `CrearResena.jsx`. Pantalla de formulario completo sin Navbar/Footer.
- **`App.jsx` refactorizado**: extraído componente `Layout` interno que usa `useLocation` para ocultar Navbar y Footer en las rutas `/crear-resena` y `/editar-resena`.
- Estructura:
  - **Header propio**: logo a la izquierda, "Nueva reseña" centrado con `absolute + -translate-x-1/2`, botón "Cancelar" (`navigate(-1)`) a la derecha.
  - **Card del álbum** (izquierda): portada `aspect-square` (`PortadaPlaceholder` con `iconSize="text-6xl"`), título, artista·año, chip de género.
  - **Formulario** (derecha):
    - **Estrellas interactivas**: componente `EstrellasInteractivas` local. Detecta si el cursor está en la mitad izquierda o derecha de cada estrella (`getBoundingClientRect`) para asignar media o estrella entera. Muestra "X / 5" o "— / 5" si no hay puntuación.
    - **Textarea** con contador `{texto.length} / 2000` en esquina inferior derecha. Truncado con `.slice(0, MAX_CHARS)`.
  - **Botón "Publicar reseña"** deshabilitado (`opacity-40`) si `puntuacion === 0`.

### Mejoras transversales de navegación

- `AlbumCard` y `CatalogoCard` convertidas en `Link` a `/album/:id` (prop `id` con default `1`). Efecto `hover:border-primary`.
- Nombre del artista en `DetalleAlbum` enlazado a `/artista/1`.
- Filas de Top Artistas en `Rankings` envueltas en `Link` a `/artista/:posicion`.
- `PortadaPlaceholder` acepta prop `iconSize` (default `text-4xl`) para escalar el ♪ según el tamaño del contenedor.

### Estado al finalizar la sesión

- Páginas completadas: Inicio ✅, Login ✅, Registro ✅, Catálogo ✅, Búsqueda ✅, Rankings ✅, Detalle Álbum ✅, Detalle Artista ✅, Crear Reseña ✅
- Páginas pendientes: Editar Reseña, Perfil de Usuario, Editar Perfil, Mis Favoritos, Panel Admin, 404

---

## Semana 9 — Frontend: páginas restantes y cierre visual (27/04/2026, sesión 2)

**Fase:** FASE 3 — Frontend React + Vite (finalización)

### Páginas implementadas

#### Editar Reseña — `/editar-resena`

- Creado `EditarResena.jsx`. Pantalla sin Navbar/Footer (añadida a `SIN_NAVBAR` en `App.jsx`).
- Reutiliza el componente `EstrellasInteractivas` (extraído como componente compartido en esta sesión).
- **Izquierda**: card del álbum con badge "Editando" (`bg-primary/20`) en lugar del chip de género, y card "Detalles de tu reseña" con divisor `h-px bg-border` y dos filas: `Publicada` + `Última edición`.
- **Derecha**: puntuación interactiva pre-rellenada con la puntuación existente, textarea pre-rellenado con el texto existente y contador de caracteres.
- **Tres botones**: "Guardar cambios" (`bg-primary`, `navigate(-1)`), "Eliminar reseña" (`border-error`, `hover:bg-error/10`), "Cancelar" (texto muted, `navigate(-1)`).

#### Perfil de Usuario — `/perfil/:username`

- Creado `PerfilUsuario.jsx`. Ruta dinámica `/perfil/:username`.
- **Header**: avatar circular `w-24 h-24` con inicial del username, nombre, "Miembro desde", stats de Reseñas y Favoritos en `text-primary font-bold text-2xl`, botón "Editar perfil" posicionado con `absolute top-0 right-0`.
- **Tabs** con indicador animado: línea `h-0.5 bg-primary` en `absolute bottom-0` debajo del tab activo.
- **Tab Reseñas**: lista vertical de cards con portada `w-24 h-24`, nombre álbum, artista, estrellas, texto en italic y fecha alineada a la derecha. Cada card es `Link` a `/album/:id`.
- **Tab Favoritos**: grid 6 columnas con badge de corazón `bg-primary text-white` en esquina superior derecha. Cada item es `Link` a `/album/:id`.

#### Editar Perfil — `/editar-perfil`

- Creado `EditarPerfil.jsx`. Pantalla sin Navbar/Footer.
- **Header propio**: logo, "Editar perfil" centrado, botón "Guardar" (`bg-primary`).
- **Avatar**: círculo `w-24 h-24 border-2 border-primary` con ♪ + botón de texto "Cambiar foto de perfil".
- **Campos**: Username, Email (inputs `h-12`), Biografía (textarea 4 filas).
- **Seguridad**: botón "Cambiar contraseña" con flecha `›` en `justify-between`.
- **Zona de peligro**: heading `text-error`, botón "Desactivar cuenta" con `border-error hover:bg-error/10`.

#### Mis Favoritos — `/favoritos`

- Creado `MisFavoritos.jsx`. Ruta `/favoritos`.
- **Estado con datos**: grid 4 columnas, badge ♥ `bg-primary text-white` en esquina de cada portada.
- **Estado vacío**: icono ♡ `text-border`, título, subtítulo y `Link` a `/catalogo`.
- Accesible desde el icono ♥ añadido al Navbar.

#### Panel de Administración — `/admin`

- Creado `PanelAdmin.jsx`. Ruta `/admin`.
- **4 stat cards**: Álbumes, Artistas, Usuarios, Inactivos (este último en `text-error`).
- **Gestión de contenido** (columna izquierda): 4 botones-fila con icono circular `bg-primary/20`, título, subtítulo y flecha `›`.
- **Gestión de usuarios** (columna derecha): 3 usuarios con avatar circular, username, email y badge Activo (`bg-primary/20 text-primary`) / Inactivo (`bg-error/20 text-error`).
- **Moderación**: botón-fila "Revisar reseñas reportadas" con flecha `›`.

#### 404 Not Found — `*`

- Creado `NotFound.jsx`. Captura cualquier ruta desconocida (`<Route path="*">`).
- Número "404" en `text-primary font-heading` a `font-size: 10rem`.
- Subtítulo, descripción y `Link` "Ir al inicio" (`bg-primary`).

### Refactorización: extracción de `EstrellasInteractivas`

- `EstrellasInteractivas` estaba duplicado en `CrearResena.jsx` y `EditarResena.jsx`.
- Extraído a `src/components/ui/EstrellasInteractivas.jsx` y ambas páginas actualizadas para importarlo.
- Comportamiento: detecta mitad izquierda/derecha de cada estrella con `getBoundingClientRect` para media estrella. Muestra "X / 5" o "— / 5".

### Mejoras de navegación

- Icono ♥ añadido al Navbar (antes del icono de búsqueda) con `Link` a `/favoritos`.
- Badge corazón de `MisFavoritos.jsx` corregido de `bg-white text-primary` a `bg-primary text-white`.
- Botón "Guardar cambios" de `EditarResena` corregido: ahora ejecuta `navigate(-1)`.

### Estado al finalizar la sesión

**Frontend 100% completado.** ✅

| Pantalla | Ruta | Estado |
|---|---|---|
| Inicio | `/` | ✅ |
| Login | `/login` | ✅ |
| Registro | `/registro` | ✅ |
| Catálogo | `/catalogo` | ✅ |
| Búsqueda | `/busqueda` | ✅ |
| Rankings | `/rankings` | ✅ |
| Detalle de Álbum | `/album/:id` | ✅ |
| Detalle de Artista | `/artista/:id` | ✅ |
| Crear Reseña | `/crear-resena` | ✅ |
| Editar Reseña | `/editar-resena` | ✅ |
| Perfil de Usuario | `/perfil/:username` | ✅ |
| Editar Perfil | `/editar-perfil` | ✅ |
| Mis Favoritos | `/favoritos` | ✅ |
| Panel Admin | `/admin` | ✅ |
| 404 Not Found | `*` | ✅ |

**Componentes UI creados en total: 12**
`SectionTitle`, `PortadaPlaceholder`, `Estrellas`, `EstrellasInteractivas`, `ResenaCard`, `AlbumCard`, `CatalogoCard`, `FormInput`, `GenreChip`, `SearchBar`, `SelectOrden`, `Paginacion`, `AlbumRow`

---

## Semana 10 — Fase 4, sesión 1: integración frontend ↔ backend (28/04/2026)

**Fase:** FASE 4 — Conectar frontend React con backend Spring Boot.

Plan de la fase (9 pasos): AuthContext → Login + Registro → Navbar dinámico → Rutas protegidas → Páginas públicas → Páginas de álbum → Páginas de usuario → Reseñas → Portadas.

### Lo que se cubre en esta sesión

1. **Paso 1 (AuthContext)** y **paso 2 (Login + Registro funcionales)** completos.
2. **5 bugs del backend** detectados al hacer las primeras pruebas reales con Postman desde la perspectiva del frontend, y arreglados.
3. **38 tests unitarios** verdes tras los arreglos.

### Estado de partida del paso 1

`AuthContext.jsx` y `services/auth.js` ya existían de una sesión anterior pero no se habían validado contra el backend. Login y Registro estaban maquetados pero sin lógica.

Tras esta sesión:
- `services/auth.js` — capa pura de red. `fetch` a `/api/auth/login` y `/api/auth/register`. Devuelve `{token, id, username, email, rol}` o lanza `Error` con `mensaje` del backend.
- `AuthContext.jsx` — estado React de `usuario` y `token`, persistido en localStorage. Expone `login()`, `register()`, `logout()`, `useAuth()`.
- `main.jsx` — envuelve `<App />` con `<AuthProvider>`.
- `Login.jsx` y `Registro.jsx` — formularios conectados al contexto, con manejo de errores y estado de carga.

### Bugs detectados en el backend al integrar

#### Bug B1 — `LazyInitializationException` por `open-in-view=false`

**Síntoma:** desde Postman, `POST /api/resenas` con un token válido devolvía **401 Unauthorized**, no 200. Lo mismo con `GET /api/resenas/usuario/5/album/373`. Endpoints muy parecidos como `GET /api/albumes/3` sí funcionaban.

**Diagnóstico:** redirigir los logs del backend a fichero (`./mvnw spring-boot:run > backend.log`) y reproducir el fallo. El log mostraba:

```
HttpMessageNotWritableException: Could not write JSON:
Could not initialize proxy [com.musicreviews.backend.model.Album#373] - no session
```

Es un `LazyInitializationException` clásico: Jackson intenta serializar la respuesta, accede a `resena.album` que es un `@ManyToOne(fetch = FetchType.LAZY)` (un proxy de Hibernate), pero la sesión de Hibernate ya está cerrada → fallo.

**Causa raíz:** en `application.properties` había `spring.jpa.open-in-view=false`, puesto a `false` en una sesión anterior "para mejorar el rendimiento". Este parámetro cierra la sesión Hibernate al salir del `@Transactional`, antes de que Jackson serialice. Para entidades sin relaciones LAZY (como `Album`) no afecta. Para `Resena` y `Favorito`, que tienen `@ManyToOne(fetch = LAZY)` a `usuario` y `album`, rompe la serialización.

El **misterio del 401 (en lugar del 500 esperado)** es que Spring Security 7 (Spring Boot 4.0.5) traduce los fallos durante la escritura del response como `Authentication` errors y dispara el `authenticationEntryPoint` configurado, que devuelve 401. Por eso el síntoma despistaba: parecía un problema de auth cuando en realidad era de serialización.

**Solución:** revertir a `open-in-view=true` (el default de Spring Boot por algo). El supuesto coste de rendimiento es despreciable en este TFG y permite que Jackson cargue las relaciones LAZY al serializar.

**Verificación:** los mismos endpoints volvieron a 200 inmediatamente.

#### Bug B2 — `@JsonAutoDetect` rompía los proxies de Hibernate

**Síntoma:** después de arreglar el B1, la respuesta del POST llegaba con código 200 pero el campo `usuario` aparecía con todos los valores a `null`:

```json
"usuario": {
  "$$_hibernate_interceptor": {},
  "activo": true,
  "id": null,
  "username": null,
  "email": null,
  ...
}
```

A la vez, aparecía basura interna de Hibernate (`$$_hibernate_interceptor`, `hibernateLazyInitializer`).

**Causa raíz:** en `Usuario.java` se había añadido en una sesión anterior `@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, ...)`. Esta anotación fuerza a Jackson a leer los **campos directamente** en lugar de llamar a los getters. El problema es que los proxies de Hibernate **no inicializan los campos** — solo se inicializan al llamar al getter. Resultado: Jackson lee los campos antes de que el proxy los rellene → todo `null`.

La anotación se había añadido para que `@JsonProperty(WRITE_ONLY)` sobre `password` ocultara la contraseña en las respuestas. Pero esa anotación funciona perfectamente sin `@JsonAutoDetect` porque Jackson, por defecto, mezcla las anotaciones de campo con el getter al introspeccionar la propiedad.

**Solución:**

1. Quitar `@JsonAutoDetect` de `Usuario.java`. La ocultación de `password` sigue funcionando con la `@JsonProperty(access = WRITE_ONLY)` que ya estaba en el campo.
2. Añadir `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` en todas las entidades que se serialicen (`Usuario`, `Album`, `Artista`, `Resena`, `Favorito`) para evitar que esos campos internos del proxy se cuelen en el JSON.

**Verificación:** la siguiente respuesta del POST tenía `usuario.username: "maria_indie"`, `usuario.email`, etc., todos rellenos, y sin basura de Hibernate.

#### Bug B3 — `refresh()` antes del flush descartaba los cambios en `actualizar()`

**Síntoma:** `PUT /api/resenas/{id}` devolvía siempre los valores **antiguos** de la reseña, no los nuevos. Si la reseña tenía `puntuacion=4.0` y se hacía PUT con `puntuacion=5.0`, la respuesta seguía mostrando 4.0 (aunque la BD luego acababa con 5.0 al final del request).

**Causa raíz:** el método `ResenaService.actualizar` tenía:

```java
resena.setPuntuacion(datosActualizados.getPuntuacion());
resenaRepository.save(resena);
entityManager.refresh(resena);   // ← problema aquí
return resena;
```

`refresh()` recarga la entidad desde la BD descartando los cambios en memoria. Pero `save()` **no fuerza el flush** — los cambios solo se persisten al hacer commit de la transacción. Orden temporal real:

1. `setPuntuacion(5.0)` — en memoria, no en BD.
2. `save()` — Hibernate lo agenda pero no lo persiste todavía.
3. `refresh()` — lee la BD (que sigue con 4.0) y **sobrescribe** los cambios en memoria.
4. `return resena` — con 4.0.
5. Commit — el dirty checking ve que `resena` tiene 4.0 (igual que en BD) y no genera UPDATE.

El refresh tenía sentido en `crear()` porque ahí la entidad se persiste con `INSERT` inmediato (por la estrategia `IDENTITY`) y `refresh` convierte los stubs `{id:5}` del request body en proxies gestionados. En `actualizar()` la entidad ya viene gestionada de `findById()` y `refresh` solo estorba.

**Solución:** quitar el `refresh()` en `actualizar()`. El dirty checking de Hibernate al commit ya genera el UPDATE; el objeto en memoria se devuelve con los valores nuevos:

```java
return resenaRepository.save(resena);
```

**Verificación:** `PUT` ahora devuelve los valores nuevos y `fechaEdicion` se rellena correctamente.

#### Bug B4 — Login/register devolvían `text/plain` en errores

**Síntoma:** al probar el `Login.jsx` con un password incorrecto, en lugar de mostrar "Email o contraseña incorrectos" salía un error técnico de JSON parsing.

**Diagnóstico:** `curl -i -X POST /api/auth/login` con credenciales malas devolvía:

```
HTTP/1.1 401
Content-Type: text/plain;charset=UTF-8

Email o contraseña incorrectos
```

Pero el frontend hace `const data = await res.json();` que falla porque el body no es JSON. El `catch` lo atrapa y muestra ese mensaje técnico al usuario.

**Causa raíz:** `AuthController.java` devolvía manualmente texto plano:

```java
return ResponseEntity.status(401).body("Email o contraseña incorrectos");
```

mientras que el resto de la API usa el `GlobalExceptionHandler` para devolver siempre JSON uniforme con `{status, mensaje, timestamp}`.

**Solución:** que `AuthController` lance excepciones (`ReglaNegocioException`) en vez de devolver bodies manuales:

```java
if (usuario == null || !passwordEncoder.matches(...)) {
    throw new ReglaNegocioException("Email o contraseña incorrectos");
}
```

Cambia el código HTTP de 401 a 400, pero eso no es un problema funcional: el frontend solo lee el campo `mensaje`. Y semánticamente "credenciales mal" está más cerca de "petición incorrecta" que de "no autenticado".

**Verificación:**

```
$ curl -i -X POST /api/auth/login -d '{"email":"x@x.com","password":"WRONG"}'
HTTP/1.1 400
Content-Type: application/json

{"mensaje":"Email o contraseña incorrectos","status":400,"timestamp":"..."}
```

#### Bug B5 — Test unitario roto desde la sesión anterior

**Síntoma:** al ejecutar `./mvnw test` después de los arreglos anteriores, fallaba un único test:

```
ResenaServiceTest.crear_conDatosValidos_guardaYDevuelveResena
NullPointerException: Cannot invoke "EntityManager.refresh(Object)"
because "this.entityManager" is null
```

**Causa raíz:** en una sesión anterior se había añadido el campo `private final EntityManager entityManager` a `ResenaService` (para el `refresh()` documentado), pero **no se actualizó el test** que usa `@InjectMocks`. Mockito necesita un `@Mock EntityManager` para inyectar; sin él, el campo queda a `null`.

Este bug **ya estaba ahí** antes de empezar la fase 4, simplemente nadie había vuelto a correr los tests.

**Solución:** añadir el mock al test:

```java
@Mock
private EntityManager entityManager;
```

Mockito lo inyecta automáticamente en `ResenaService` por tipo. El test no necesita stubbear `entityManager.refresh(...)` porque es un método `void` y Mockito hace nothing por defecto.

**Verificación:** `./mvnw test` pasa los 38 tests sin errores.

### Pruebas hechas

Tres planos de verificación:

1. **`curl` en consola** para diagnóstico del bug B1 — redirección de logs a fichero, captura del `HttpMessageNotWritableException` que reveló la causa raíz.
2. **6 lotes Postman** cubriendo el flujo end-to-end del frontend: login → captura de token → POST/PUT/DELETE reseñas → POST/DELETE favoritos → casos de error (sin token, duplicado, validación, token corrupto).
3. **Tests unitarios JUnit + Mockito**: 38/38 pasando tras los arreglos.

### Bugs en pruebas a destacar

- En el primer intento, el script `pm.environment.set("token", ...)` estaba en la petición de **reseñas** en vez de en la de **login**. Resultado: la variable `{{token}}` quedaba a `null` y el header `Authorization: Bearer null` provocaba 401. Este 401 inicial fue el que destapó toda la cadena de investigación.
- El encoding de la consola Bash en Windows mandaba `Gran álbum` en Windows-1252 en vez de UTF-8, provocando `HttpMessageNotReadableException: Invalid UTF-8 middle byte`. No era bug del backend, era de la consola; en Postman no pasa porque manda UTF-8 nativo.

### Cambios en el código

**Backend** (`MusicReviews_TFG/backend/backend/`):
- `src/main/resources/application.properties` (B1)
- `src/main/java/.../model/Usuario.java`, `Album.java`, `Artista.java`, `Resena.java`, `Favorito.java` (B2)
- `src/main/java/.../service/ResenaService.java` (B3)
- `src/main/java/.../controller/AuthController.java` (B4)
- `src/test/java/.../service/ResenaServiceTest.java` (B5)

**Frontend** (`musicreviews-frontend/`): nada tocado en esta sesión (los ficheros ya existían).

**Documentación**:
- `docs/integracion.md` — **nuevo fichero** con el plan de la fase 4, AuthContext explicado, los 5 bugs con causa/solución/verificación, los 6 lotes Postman con respuestas reales, tests unitarios y anexo de decisiones técnicas.
- `docs/pruebas_postman.md` — sección añadida con resumen de los bugs B1–B5 y aprendizajes.
- `docs/frontend.md` — nueva sección "Fase 4" con resumen y enlace a `integracion.md`.

### Estado al cerrar la sesión

✅ Paso 1 (AuthContext) y paso 2 (Login + Registro) cerrados. Login con maría desde Postman funciona, devuelve token y datos del usuario, error de credenciales devuelve JSON parseable.
🔜 Paso 3: Navbar dinámico — primera vez que el contexto se va a usar fuera de los formularios de auth, mostrando avatar/logout cuando hay sesión y los botones Entrar/Registrarse cuando no.

---

## Semana 10 — Fase 4, sesión 2: Navbar dinámico (28/04/2026)

**Fase:** FASE 4 — paso 3 del plan.

### Objetivo

Hacer que el `Navbar` reaccione al estado de sesión: mostrar opciones distintas según haya o no `usuario` en `AuthContext`. Es la primera vez que el contexto se consume **fuera** de los formularios de auth.

### Cambios

**Solo se ha tocado un fichero del frontend:** `src/components/layout/Navbar.jsx`.

#### Renderizado condicional

`useAuth()` da acceso a `usuario` y `logout`. Un único operador ternario sobre `usuario` decide qué bloque renderizar:

| Elemento | Sin sesión | Con sesión |
|---|---|---|
| Logo, links Inicio / Explorar / Top Álbumes | ✅ | ✅ |
| Buscador | ✅ | ✅ |
| Botones "Entrar" / "Registrarse" | ✅ | ❌ |
| Icono ♥ Favoritos | ❌ | ✅ |
| Avatar circular con inicial real del usuario | ❌ | ✅ |
| Botón "Salir" | ❌ | ✅ |
| Link "Admin" | ❌ | Solo si `rol === "ADMIN"` |

#### Logout

```jsx
function handleLogout() {
  logout();
  navigate("/");
}
```

`logout()` (del contexto) limpia estado React + localStorage. `navigate("/")` redirige al home para evitar que el usuario se quede en una página privada con datos cacheados. Se usa `<button onClick>` y NO `<Link>` porque es una **acción**, no navegación.

#### Avatar dinámico

Antes: hardcoded `<Link to="/perfil/pablo_music">P</Link>`. Ahora:

```jsx
<Link to={`/perfil/${usuario.username}`}>
  {usuario.username.charAt(0).toUpperCase()}
</Link>
```

Para `maria_indie` muestra "M" y enlaza a `/perfil/maria_indie`.

#### Link Admin condicional

```jsx
{usuario?.rol === "ADMIN" && <Link to="/admin">Admin</Link>}
```

Optional chaining para evitar el `TypeError` cuando `usuario` es `null` en el primer render.

#### Detalles pequeños arreglados de paso

- Logo ahora es `<Link to="/">` (antes era un `<div>` no clicable).
- `aria-label` en iconos sin texto (lupa, corazón, avatar) para lectores de pantalla.

### Verificación

Probado manualmente con `npm run dev` (puerto 5173):

| Paso | Esperado | Cumplido |
|---|---|---|
| Cargar la app sin sesión | Botones "Entrar"/"Registrarse" visibles, sin ♥ ni avatar | ✅ |
| Login con maría → home | Avatar "M" verde, ♥ visible, "Salir" visible, sin botones de auth | ✅ |
| Refrescar la página estando logueado | Sigue logueado (localStorage) | ✅ |
| Click en avatar | Lleva a `/perfil/maria_indie` | ✅ |
| Click en "Salir" | Limpia sesión, vuelve a `/`, reaparecen botones de auth | ✅ |

### Bug B6 detectado al verificar el login real (frontend)

Al hacer el primer intento de login real desde el navegador (no Postman), el backend respondía 400 "Email o contraseña incorrectos" sistemáticamente, aunque las credenciales fueran las correctas y funcionaran desde curl/Postman.

**Causa raíz:** el componente `FormInput.jsx` tenía esta firma:

```jsx
export default function FormInput({ label, type, placeholder, id, error }) {
  return <input id={id} type={type} placeholder={placeholder} ... />;
}
```

No declaraba `value` ni `onChange` como props y tampoco los pasaba al `<input>`. Cuando `Login.jsx` los usaba (`<FormInput value={email} onChange={...} />`), se descartaban silenciosamente. El `<input>` real no era controlado por React: el usuario tecleaba, las letras aparecían en pantalla, pero el estado `email`/`password` en `Login.jsx` seguía siendo `""`. Al hacer submit se enviaba `{"email":"","password":""}` al backend.

**Solución:** que `FormInput` propague al `<input>` cualquier prop estándar mediante el operador rest:

```jsx
export default function FormInput({ label, type = "text", placeholder, id, error = false, ...rest }) {
  return (
    <input id={id} type={type} placeholder={placeholder} className="..." {...rest} />
  );
}
```

Esto incluye `value`, `onChange`, `name`, `autoComplete`, `required`, etc. Es la convención estándar de cualquier librería de UI (MUI, Chakra, Radix).

**Aprendizaje:** las pruebas visuales no son pruebas funcionales. El formulario "se veía bien" pero no enviaba lo que el usuario tecleaba. Solo se descubrió al hacer un flujo end-to-end real. F12 → Network → Payload reveló el body real con campos vacíos.

Ver detalle completo en [`integracion.md` § 5](integracion.md) y [`pruebas_postman.md`](pruebas_postman.md) (B6).

### Estado al cerrar la sesión

✅ Pasos 1, 2 y 3 completos. Bug B6 (FormInput) arreglado.
✅ Login end-to-end **verificado en el navegador** (no solo Postman): maría inicia sesión desde el formulario, el navbar reacciona, la sesión persiste al refrescar y el logout funciona.
🔜 Paso 4: Rutas protegidas. Hoy `/favoritos`, `/admin`, `/crear-resena`, `/editar-perfil` son accesibles escribiendo la URL a mano aunque no haya sesión — el navbar las oculta pero la ruta no está protegida. Hay que crear un componente wrapper `<RutaProtegida>` que comprueba el contexto y redirige a `/login` si no hay usuario, y un `<RutaAdmin>` que además exija `rol === "ADMIN"`.

Detalle paso a paso en [`integracion.md` § 4 y § 5](integracion.md).

---

## Semana 10 — Fase 4, sesión 3: Rutas protegidas (28/04/2026)

**Fase:** FASE 4 — paso 4 del plan.

### Objetivo

Hasta ahora el navbar oculta los enlaces a páginas privadas, pero las rutas siguen siendo accesibles escribiendo la URL a mano. Hay que añadir protección **a nivel de ruta** con dos componentes wrapper que comprueben el contexto antes de renderizar.

### Cambios

| Fichero | Cambio |
|---|---|
| `src/components/routing/RutaProtegida.jsx` *(nuevo)* | Wrapper que comprueba `usuario !== null`. Sin sesión redirige a `/login` guardando `location.state.from`. Con sesión renderiza `<Outlet />`. |
| `src/components/routing/RutaAdmin.jsx` *(nuevo)* | Igual + comprobación de rol. Sin sesión a `/login`, con sesión sin rol ADMIN a `/` (NO a `/login`, eso crea bucle). |
| `src/App.jsx` | Rutas reagrupadas en 3 bloques: públicas, envueltas en `<RutaProtegida>`, envueltas en `<RutaAdmin>`. |
| `src/pages/Login.jsx` | Lee `location.state.from` y tras login navega a esa URL en vez de a `/`. Con `{ replace: true }` para que `/login` no quede en el historial. |

### Patrón usado

```jsx
<Route element={<RutaProtegida />}>
  <Route path="/favoritos" element={<MisFavoritos />} />
  <Route path="/crear-resena" element={<CrearResena />} />
  ...
</Route>
```

`<Outlet />` (de React Router) es la primitiva que indica "aquí va la ruta hija". Permite usar el wrapper como ruta padre sin tener que duplicar lógica en cada página privada.

### Decisiones técnicas

- **`state={{ from: location }}` al redirigir a `/login`**: para que tras autenticarse el usuario vuelva a la URL que intentaba abrir, no al home. Mejora la UX y es estándar de React Router.
- **`replace` en `<Navigate>` y en `navigate()`**: evita que `/login` quede en el historial. Pulsar "atrás" tras autenticarse no devuelve al formulario completado.
- **`RutaAdmin` redirige a `/` (no a `/login`) cuando hay sesión sin rol**: re-autenticarse no le daría rol ADMIN al usuario, y mandarlo a `/login` con `from = /admin` provoca bucle (tras login volvería a `/admin` y rebotaría otra vez).
- **`location.state?.from?.pathname || "/"`**: optional chaining porque entrada directa a `/login` deja `location.state` a `null`.

### Pruebas realizadas (5 casos)

Frontend en `:5173` + backend Spring Boot en `:8080`, con dos usuarios de prueba:
- `maria@musicreviews.com / maria123` → rol USER
- `admin@musicreviews.com / admin123` → rol ADMIN (creado para esta prueba)

| # | Caso | Esperado | Resultado |
|---|---|---|---|
| 1 | Sin sesión, URL directa a `/favoritos`, `/crear-resena`, `/editar-perfil`, `/admin` | Cada una redirige a `/login` | ✅ |
| 2 | Sin sesión → `/favoritos` → login con maría | Tras login aterrizar en `/favoritos` (no `/`); pulsar atrás no vuelve a `/login` | ✅ |
| 3 | Logueado con maría, URL directa a `/admin` | Redirige a `/` (no a `/login`); navbar sin link "Admin" | ✅ |
| 4 | Logueado con admin, URL directa a `/admin` | Carga `PanelAdmin`; navbar muestra link "Admin"; avatar "A" lleva a `/perfil/admin` | ✅ |
| 5 | Rutas públicas (`/catalogo`, `/rankings`, `/album/:id`, `/perfil/maria_indie`...) con o sin sesión | Cargan normal en ambos casos | ✅ |

### Creación del usuario admin

`POST /api/auth/register` siempre crea con `rol = USER` (lo fija el `@PrePersist` de `Usuario.java`). Para tener un admin de prueba:

1. Registrar via API: `curl -X POST /api/auth/register -d '{"username":"admin","email":"admin@musicreviews.com","password":"admin123"}'` → `id: 10, rol: USER`.
2. Promocionar con SQL en Aiven (vía MySQL Shell): `UPDATE usuario SET rol = 'ADMIN' WHERE email = 'admin@musicreviews.com';`.

No hay endpoint de promoción a propósito: sería un agujero de seguridad. La asignación de roles siempre debe ser operativa (DBA o panel admin con verificación de rol).

### Limitaciones conocidas (a propósito, fuera del paso 4)

- **El backend no comprueba el rol en endpoints administrativos** — todavía. Cuando el panel admin se conecte de verdad habrá que comprobar `SecurityConfig` y añadir `.hasRole("ADMIN")` donde toque. La protección del frontend no es suficiente: un usuario podría llamar al endpoint con su token sin pasar por la UI.
- **Verificación de email**: no implementada. Cualquiera puede registrarse con un email inventado. Es una limitación conocida y aceptable para un TFG académico, mencionable como ampliación futura.
- **El logout no invalida el JWT en el servidor**, solo lo borra del cliente. JWT puro no permite invalidación; haría falta blacklist o tokens de corta vida + refresh. Fuera del ámbito del TFG.

### Estado al cerrar la sesión

✅ Pasos 1, 2, 3 y 4 completos.
✅ Protecciones por rol funcionando en los dos lados (UI: navbar oculta; ruta: redirección).
🔜 Paso 5: Páginas públicas con datos reales. Catálogo, Búsqueda, Rankings, Detalle de álbum y de artista hoy usan datos mock dentro de cada componente. Hay que reemplazarlos por llamadas a `GET /api/albumes`, `/api/artistas`, `/api/estadisticas/*` y manejar estados de carga / error.

Detalle completo en [`integracion.md` § 6](integracion.md).

---

## Semana 10 — Fase 4, sesión 4: páginas públicas con datos reales (28/04/2026)

**Fase:** FASE 4 — paso 5 del plan.

### Objetivo

Reemplazar los datos mock de Inicio, Catálogo, Búsqueda y Rankings por datos reales del backend. Es la primera vez que el frontend consume datos de negocio que no son de auth.

### Capa de servicios — separación por dominio

Tres servicios nuevos, uno por dominio del backend:

| Fichero | Funciones | Endpoints |
|---|---|---|
| `services/albumes.js` | `getAlbumes(params)`, `getAlbum(id)` | `GET /api/albumes`, `GET /api/albumes/{id}` |
| `services/artistas.js` | `getArtistas`, `getArtista(id)` | `GET /api/artistas`, `GET /api/artistas/{id}` |
| `services/estadisticas.js` | 8 funciones | `GET /api/estadisticas/*` |

Construyen query strings con `URLSearchParams` (codificación correcta de tildes y caracteres especiales) y lanzan `Error` con mensaje legible si la respuesta no es 2xx.

### Patrón de fetching

`useState` + `useEffect` + `fetch` nativo. **Sin librería externa** (nada de React Query, SWR, Axios). Tres estados: cargando / error / datos. `Promise.all` para peticiones paralelas en páginas con varias.

### Cambios por página

**Inicio (`/`):**
- Hero rediseñado: el mock estático "DAMN. — Kendrick Lamar — 2017" se ha reemplazado por la **mejor reseña reciente real** (sort por puntuación desc, primera). Card clicable a `/album/:id`.
- "Reseñas recientes": 4 `ResenaCard` reales, ahora todas clicables (`ResenaCard` actualizada para ser `<Link>`).
- "Top Álbumes": 5 `AlbumCard` reales con portadas de Spotify y rating.

**Catálogo (`/catalogo`):**
- Paginación server-side: `Page<Album>` con `{content, page: {totalPages, totalElements}}`.
- Géneros dinámicos: top 8 con más álbumes (de los 36 que hay en BD).
- Búsqueda con **debounce de 300 ms** (`setTimeout` + `clearTimeout` en cleanup) para evitar spam de peticiones.
- Solo orden "A → Z" porque el backend no acepta `sort` en el listado paginado. Las opciones del mock que no funcionaban se han retirado.
- `CatalogoCard.jsx` hizo opcional el `rating` (`{rating != null && ...}`) porque el listado paginado del backend no incluye `puntuacionMedia`.

**Búsqueda (`/busqueda`):**
- Tendencias (4) + Añadidos recientemente (3) cargadas al montar.
- Resultados de búsqueda con `getAlbumes({titulo})` + debounce 300 ms.
- Chips "Álbumes/Artistas/Usuarios" se mantienen como decoración — el backend solo busca por título de álbum.

**Rankings (`/rankings`):**
- 5 peticiones en paralelo con `Promise.all`: resumen, top-albumes, top-artistas, generos, actividad-reciente.
- Stats con números reales (732, 99, 39, 9) en lugar de los formateados a "1.2k" del mock.
- Top Álbumes, Top Artistas y Actividad reciente todas con links a sus respectivas páginas.

### Limitaciones conocidas que afloraron

- **El listado paginado de álbumes no incluye `puntuacionMedia`** — el catálogo no muestra estrellas. Para tenerlo hace falta añadir el campo computado al modelo o un endpoint dedicado.
- **El backend no acepta `sort` en `GET /api/albumes`** — el frontend solo ofrece "A → Z".
- **La búsqueda solo busca por título de álbum** — buscar artistas o usuarios requiere endpoints nuevos.
- **Géneros del backend no normalizados** ("Rock" y "rock", "hip-hop" y "Hip-Hop"). Se usa `capitalize` en CSS.

### Verificación manual

12 casos probados con `npm run dev` (frontend) + backend Spring Boot. Detalle en [`integracion.md` § 7](integracion.md).

### Estado al cerrar la sesión

✅ Pasos 1-5 completos. Las 4 páginas públicas con datos reales del backend.
🔜 Paso 6: Detalle de álbum y artista. `/album/:id` y `/artista/:id` siguen mock; conectar con `getAlbum(id)`, listar reseñas reales del álbum, mostrar discografía del artista.

---

## Semana 10 — Fase 4, sesión 5: detalle de álbum y de artista (28/04/2026)

**Fase:** FASE 4 — paso 6 del plan.

### Objetivo

Conectar las páginas de detalle (álbum y artista) con datos reales del backend, incluyendo el toggle funcional de favoritos.

### Servicios nuevos

| Fichero | Funciones | Auth |
|---|---|---|
| `services/resenas.js` | `getResenasPorAlbum`, `getResenasPorUsuario`, `getResenaUsuarioAlbum` | Pública |
| `services/favoritos.js` | `esFavorito`, `getFavoritosUsuario`, `agregarFavorito`, `quitarFavorito` | Requiere token |

**Patrón de auth:** las funciones reciben el `token` como parámetro y lo ponen en el header `Authorization`. El componente lo lee de `useAuth()`. Servicios siguen siendo funciones puras, sin acoplarse a React ni a localStorage.

### `DetalleAlbum.jsx`

- `useParams` lee `:id` de la URL.
- `Promise.all([getAlbum, getResenasPorAlbum])` paralelo, después `getAlbumes({artistaId, size: 5})` encadenado para "Más del artista" (filtrando el álbum actual).
- `useEffect` separado para comprobar el estado de favorito (depende de `usuario` y `token`, que pueden cambiar tras login/logout).
- Toggle favorito con guard `favoritoOcupado` para evitar doble click → 400 "ya está en favoritos".
- Sin sesión, el botón se convierte en `<Link to="/login">` con texto distinto. Honesto.
- Reseñas con username clicable a `/perfil/:username` y fecha formateada con `toLocaleDateString("es-ES")`. Indicador "editada" si `fechaEdicion != null`.
- Puntuación media calculada en cliente sobre las reseñas reales (el backend no devuelve `puntuacionMedia` en el detalle del álbum).
- "Escribir reseña" pasa el `albumId` por `state` para que `CrearResena` (paso 8) sepa qué álbum.

### `DetalleArtista.jsx`

- `useParams` + `Promise.all([getArtista, getAlbumes({artistaId, size: 100})])`.
- Discografía ordenada por `fechaLanzamiento` descendente (más recientes primero).
- **Sin sección "Reseñas recientes"** — el backend no expone "todas las reseñas de un artista". Implementarlo con N+1 fetches no escala. Se documenta como mejora futura.
- **Stats reducidas a "Álbumes"** — la media y el total de reseñas por artista requieren agregado del backend.
- **Botón "Seguir artista" eliminado** — no hay endpoint en el backend; mantener el toggle local sería engañoso.

### Verificación — 10 casos manuales

Probado con maría logueada y sin sesión:

| Caso | Resultado |
|---|---|
| Click en card del catálogo / Top Álbumes → /album/:id | ✅ Carga datos reales |
| Detalle de álbum sin reseñas | ✅ Estado vacío con CTA |
| Reseñas con username clicable + fecha + "editada" | ✅ |
| "Más del artista" excluye el álbum actual | ✅ |
| Click en nombre del artista → /artista/:id | ✅ |
| Sin sesión, botón "Inicia sesión para guardar" en lugar de favoritos | ✅ |
| Con sesión, click en "♡" → POST favorito y cambia a "♥" sin recargar | ✅ |
| Click rápido doble | ✅ Segundo click ignorado por `disabled` |
| Refrescar con favorito guardado | ✅ Aparece "♥ En favoritos" en la carga |
| Quitar de favoritos | ✅ DELETE y vuelve a "♡" |

### Limitaciones conocidas

- No hay endpoint para "todas las reseñas de un artista" — sin sección de reseñas recientes en DetalleArtista.
- No hay endpoint de "seguir artista" — botón eliminado.
- No hay endpoint para "media + total reseñas por artista" — stats reducidas.
- Muchos álbumes no tienen `descripcion` en BD (Spotify no la importa) — bloque condicional.

### Estado al cerrar la sesión

✅ Pasos 1-6 completos. Las 6 páginas públicas/de detalle con datos reales y el toggle de favoritos funcional.
🔜 Paso 7: Páginas de usuario. `/perfil/:username` (perfil público con reseñas y favoritos del usuario), `/editar-perfil` (PUT con auth), `/favoritos` (lista del usuario logueado).

Detalle completo en [`integracion.md` § 8](integracion.md).

---

## Semana 10 — Fase 4, sesión 6: páginas de usuario (28/04/2026)

**Fase:** FASE 4 — paso 7 del plan.

### Objetivo

Conectar las tres páginas relacionadas con el usuario: perfil público, edición de perfil y mis favoritos.

### Servicio nuevo

| Fichero | Funciones |
|---|---|
| `services/usuarios.js` | `getUsuarioPorUsername` (público), `actualizarUsuario(id, datos, token)` (con auth) |

### Cambios

**`AuthContext.jsx`:** nuevo método `actualizarUsuarioLocal(datosUsuario)` que actualiza el estado y localStorage **sin tocar el token**. Permite que tras editar el perfil el navbar muestre el nuevo username/avatar inmediatamente.

**`PerfilUsuario.jsx`:**
- `useParams` para `:username` (URL más amigable que id).
- Cadena de fetches: `getUsuarioPorUsername` → `getResenasPorUsuario` → si hay sesión, `getFavoritosUsuario`.
- `useEffect` separado para favoritos (depende de `usuario+token`, que pueden cambiar sin recargar).
- Reset de estado al cambiar `:username` para evitar mezclar perfiles.
- `esMiPerfil` controla la visibilidad del botón "Editar perfil".
- Tab Favoritos con tres estados: sin sesión, cargando, con datos.

**`EditarPerfil.jsx`:**
- Inicializado desde `useAuth()` (no fetch adicional).
- PUT con auth y sincronización del contexto tras éxito (spread `{...usuario, ...actualizado}` para mantener campos no devueltos por el backend).
- Email solo lectura (el backend no lo permite cambiar).
- URL para foto en lugar de upload (paso 9 simplificado).
- Botones de "Cambiar contraseña" y "Desactivar cuenta" eliminados (no hay endpoints).
- Banner de éxito tras guardar.

**`MisFavoritos.jsx`:**
- Ruta protegida (paso 4), siempre hay sesión.
- Botón quitar inline en cada card con `e.preventDefault()` + `stopPropagation()` para no disparar el `<Link>` que envuelve.
- Optimistic update (filter del array local sin GET adicional).
- `borrandoId` como guard contra doble click y para indicador visual.

### Decisiones honestas

- **Subida de archivos no implementada** → input de URL. Documentado.
- **Cambio de contraseña no implementado** → botón eliminado. Documentado.
- **Desactivar cuenta no implementado** → botón eliminado (DELETE borra de verdad). Documentado.

### Verificación — 9 casos manuales

| Caso | Resultado |
|---|---|
| `/perfil/maria_indie` sin sesión | ✅ Datos + reseñas; Favoritos pide login |
| `/perfil/maria_indie` con sesión de maría | ✅ Botón Editar visible; Favoritos cargan |
| `/perfil/maria_indie` con sesión de admin | ✅ Sin botón Editar; favoritos visibles |
| `/perfil/inexistente` | ✅ Mensaje de error del backend |
| `/editar-perfil` sin sesión | ✅ Redirige a `/login` (RutaProtegida) |
| Cambiar username y guardar | ✅ Banner verde; navbar actualizado al instante |
| Cambiar URL de foto | ✅ Preview se actualiza al teclear; navbar tras guardar |
| `/favoritos` lista y permite quitar inline | ✅ Sin recargar |
| Click rápido en quitar | ✅ Botón "…" disabled durante DELETE |

### Estado al cerrar la sesión

✅ Pasos 1-7 completos. Falta solo el paso 8 (crear/editar/borrar reseñas) para terminar la integración.
🔜 Paso 8: `CrearResena` recibe `albumId` por `state` desde DetalleAlbum y hace POST con auth. `EditarResena` carga la reseña existente, permite actualizar (PUT) o borrar (DELETE).

Detalle completo en [`integracion.md` § 9](integracion.md).

---

## Semana 10 — Fase 4, sesión 7: CRUD de reseñas (28/04/2026)

**Fase:** FASE 4 — paso 8 del plan. **Cierre de la integración frontend ↔ backend.**

### Objetivo

Conectar las dos pantallas restantes (`/crear-resena` y `/editar-resena`) y mejorar la navegación entre ellas para que la UX sea coherente.

### Cambios

**`services/resenas.js` ampliado:**

| Función | Endpoint |
|---|---|
| `crearResena({usuarioId, albumId, puntuacion, comentario}, token)` | POST /api/resenas |
| `actualizarResena(id, {puntuacion, comentario}, token)` | PUT /api/resenas/{id} |
| `borrarResena(id, token)` | DELETE /api/resenas/{id} |

**`CrearResena.jsx`:**
- Recibe `albumId` por `location.state` desde DetalleAlbum.
- Carga el álbum (`getAlbum`) para previsualizar la card.
- POST con auth, redirige a `/album/:id` con `replace` tras éxito.
- Pantalla de aviso si entra sin albumId (entrada directa a la URL).

**`EditarResena.jsx`:**
- Recibe `albumId` por `location.state`.
- Carga la reseña existente con `getResenaUsuarioAlbum`. Si no existe, redirige automáticamente a `/crear-resena`.
- PUT con auth para guardar cambios.
- DELETE con `window.confirm` para borrar.
- Muestra "Publicada" y "Última edición" con las fechas reales.

**Mejoras transversales:**

- **`DetalleAlbum.jsx`**: detecta si el usuario logueado ya tiene reseña sobre el álbum y cambia el botón "Escribir reseña" → "Editar mi reseña" para que no aparezca el 400 "ya ha reseñado".
- **`PerfilUsuario.jsx`**: si `esMiPerfil`, añade botón "✎ Editar" en cada card de reseña (con `e.preventDefault/stopPropagation` para no disparar el Link envolvente).

### Verificación — 12 casos del CRUD completo

| Caso | Resultado |
|---|---|
| Click en "Escribir reseña" desde DetalleAlbum sin reseña previa | ✅ /crear-resena con álbum cargado |
| Submit con puntuación 0 | ✅ Botón disabled |
| Submit válido | ✅ POST OK + redirige al álbum |
| Volver al álbum tras crear | ✅ Botón ahora dice "Editar mi reseña" |
| Click en "Editar mi reseña" | ✅ /editar-resena pre-rellenada |
| Cambiar puntuación + guardar | ✅ PUT OK, valores nuevos visibles |
| /editar-resena con álbum sin reseña previa | ✅ Redirige a /crear-resena |
| Botón "Eliminar reseña" + confirm | ✅ DELETE + redirige al álbum |
| Botón "Eliminar reseña" + cancelar | ✅ No hace nada |
| Mi perfil, botón "✎ Editar" en una reseña | ✅ /editar-resena con albumId |
| Click en card sin tocar Editar | ✅ Lleva al álbum (no a editar) |
| Sin sesión a /crear-resena | ✅ RutaProtegida redirige a /login |

### Estado final de la fase 4

✅ **Integración completa frontend ↔ backend.** Las 15 pantallas tienen contenido real, navegación coherente y manejo de errores. El paso 9 (subida de archivos) se sustituyó por "URL como input" en el paso 7 — la subida real queda como mejora futura porque requiere endpoint multipart en el backend.

**Limitaciones conocidas documentadas en `integracion.md` § 12:**

- Sin verificación de email
- Sin cambio de contraseña
- Sin desactivar cuenta (DELETE borra)
- Sin subida de archivos (URL como workaround)
- Sin invalidación del JWT en logout
- Catálogo sin estrellas (backend no devuelve puntuación en listado)
- Solo orden A→Z (backend no acepta `sort`)
- Búsqueda solo por título de álbum
- Sin "seguir artista"
- Sin reseñas recientes ni stats compuestas en DetalleArtista

Todas tienen justificación honesta en el doc — son limitaciones del backend o decisiones de alcance, no bugs.
