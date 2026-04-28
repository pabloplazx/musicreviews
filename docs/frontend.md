# MusicReviews — Frontend

Documentación completa del desarrollo del frontend: React + Vite + Tailwind CSS v4.

---

## Stack y dependencias

| Paquete | Versión | Uso |
|---|---|---|
| React | 19 | Librería de UI |
| Vite | 8 | Bundler y servidor de desarrollo |
| Tailwind CSS | 4 | Estilos mediante clases de utilidad |
| React Router DOM | 7 | Navegación entre pantallas (SPA) |

- Servidor de desarrollo: `http://localhost:5173`
- Comando: `npm run dev` desde `musicreviews-frontend/`

---

## Estructura de carpetas

```
src/
├── components/
│   ├── ui/        ← componentes reutilizables (tarjetas, formularios, badges...)
│   └── layout/    ← Navbar, Footer
├── pages/         ← una archivo por pantalla
├── services/      ← llamadas al backend (fetch/axios) — pendiente de implementar
├── hooks/         ← lógica reutilizable entre componentes — pendiente de implementar
├── context/       ← estado global (usuario logueado, token JWT) — pendiente de implementar
├── assets/        ← imágenes estáticas
├── App.jsx        ← raíz de la app, configura el router
└── index.css      ← variables de diseño (@theme) + import Tailwind
```

---

## Design System

### Paleta de colores

| Token | Clase Tailwind | Hex | Uso |
|---|---|---|---|
| background | `bg-background` | `#060907` | Fondo de todas las pantallas |
| card | `bg-card` | `#0e1310` | Tarjetas, navbar, modales |
| surface | `bg-surface` | `#16261E` | Secciones CTA y bloques destacados |
| text | `text-text` | `#ebf0ed` | Texto principal |
| muted | `text-muted` | `#78908a` | Texto secundario, placeholders, fechas |
| primary | `text-primary` / `bg-primary` | `#48a377` | Botones, links, logo |
| secondary | `bg-secondary` | `#226846` | Hover de elementos primarios |
| accent | `bg-accent` | `#239a60` | Acentos puntuales |
| border | `border-border` | `#223228` | Bordes de cards e inputs |
| input | `bg-input` | `#0a0f0c` | Fondo de campos de formulario |
| error | `text-error` / `border-error` | `#cc3333` | Errores, toasts de error |

### Tipografía

| Fuente | Clase Tailwind | Uso |
|---|---|---|
| Space Grotesk | `font-heading` | Logo, títulos, botones |
| Outfit | `font-body` | Cuerpo, labels, placeholders |

### Medidas base

| Elemento | Valor | Clase Tailwind |
|---|---|---|
| Contenedor máx. | 1200px | `max-w-300 mx-auto px-12` |
| Border radius card | 16px | `rounded-card` |
| Border radius inputs/botones | 10px | `rounded-input` |
| Altura inputs | 48px | `h-12` |

> En Tailwind v4 la escala de espaciado es `1 = 4px`, por lo que `max-w-300 = 1200px`. Los tokens `rounded-card` y `rounded-input` están definidos en `index.css` con `@theme`.

---

## Routing

### Rutas de la aplicación

| Pantalla | Ruta | Con Navbar/Footer |
|---|---|---|
| Inicio | `/` | ✅ |
| Login | `/login` | ✅ |
| Registro | `/registro` | ✅ |
| Catálogo | `/catalogo` | ✅ |
| Búsqueda | `/busqueda` | ✅ |
| Rankings | `/rankings` | ✅ |
| Detalle de Álbum | `/album/:id` | ✅ |
| Detalle de Artista | `/artista/:id` | ✅ |
| Crear Reseña | `/crear-resena` | ❌ (header propio) |
| Editar Reseña | `/editar-resena` | ❌ (header propio) |
| Perfil de Usuario | `/perfil/:username` | ✅ |
| Editar Perfil | `/editar-perfil` | ❌ (header propio) |
| Mis Favoritos | `/favoritos` | ✅ |
| Panel Admin | `/admin` | ✅ |
| 404 Not Found | `*` | ✅ |

### Patrón SIN_NAVBAR

Las rutas que tienen su propio header (formularios a pantalla completa) se excluyen del Navbar/Footer global mediante el array `SIN_NAVBAR` en `App.jsx`:

```js
const SIN_NAVBAR = ["/crear-resena", "/editar-resena", "/editar-perfil"];
```

El componente `Layout` interno usa `useLocation` para consultar el pathname en cada cambio de ruta.

---

## Componentes

### Layout

#### `Navbar` — `src/components/layout/Navbar.jsx`

Barra de navegación superior. Sticky, presente en todas las pantallas excepto las del array `SIN_NAVBAR`.

**Estructura:**
```
<header>  sticky, bg-card, border-b
  <div>   contenedor centrado máx. 1200px, h-16
    LOGO          → ♪ MusicReviews (text-primary, font-heading bold)
    <nav>         → links: Inicio · Explorar · Top Álbumes
    <div>         → ♥ favoritos · icono búsqueda · Entrar · Registrarse · Avatar mock
```

> El avatar mock (inicial "P") es un placeholder temporal hasta que se implemente el contexto de autenticación. Cuando haya auth, se mostrará condicionalmente: si hay usuario → avatar; si no → Entrar + Registrarse.

#### `Footer` — `src/components/layout/Footer.jsx`

Pie de página con copyright y links de navegación. `bg-card`, `border-t border-border`.

---

### Componentes UI reutilizables

#### `SectionTitle` — `src/components/ui/SectionTitle.jsx`

Título de sección con línea verde decorativa de 32×4px debajo. Recibe el texto como `children`.

#### `PortadaPlaceholder` — `src/components/ui/PortadaPlaceholder.jsx`

Placeholder oscuro con ♪ centrado para portadas de álbum cuando no hay imagen disponible.

| Prop | Tipo | Default | Descripción |
|---|---|---|---|
| `className` | string | `""` | Dimensiones y forma (`w-full`, `aspect-square`, `rounded-full`...) |
| `iconSize` | string | `"text-4xl"` | Tamaño del símbolo ♪ |

#### `Estrellas` — `src/components/ui/Estrellas.jsx`

Valoración visual de 1 a 5 estrellas. Soporta medias estrellas (valores como 4.5, 3.5...).

**Prop:** `cantidad` (número decimal entre 0 y 5)

La media estrella se implementa superponiendo dos `★`: uno gris de fondo y otro verde recortado al 50% con `overflow-hidden w-1/2` y posicionamiento absoluto.

#### `EstrellasInteractivas` — `src/components/ui/EstrellasInteractivas.jsx`

Selector de puntuación para formularios. Detecta la mitad de la estrella mediante `getBoundingClientRect` para soportar medias estrellas al hacer click.

| Prop | Tipo | Descripción |
|---|---|---|
| `valor` | number | Puntuación actual (0–5) |
| `onChange` | function | Callback al seleccionar |

#### `FormInput` — `src/components/ui/FormInput.jsx`

Campo de formulario con label e indicador de error.

| Prop | Tipo | Default | Descripción |
|---|---|---|---|
| `id` | string | — | Vincula label con input |
| `label` | string | — | Texto del label |
| `type` | string | `"text"` | Tipo de input |
| `placeholder` | string | — | Placeholder |
| `error` | boolean | `false` | Activa borde rojo |

#### `GenreChip` — `src/components/ui/GenreChip.jsx`

Chip de filtro con estado activo/inactivo. Usado en Catálogo y Búsqueda.

#### `SearchBar` — `src/components/ui/SearchBar.jsx`

Barra de búsqueda con icono SVG. `focus-within:border-primary` en el contenedor.

#### `SelectOrden` — `src/components/ui/SelectOrden.jsx`

Selector `<select>` estilizado. Recibe `opciones`, `value` y `onChange`.

#### `Paginacion` — `src/components/ui/Paginacion.jsx`

Paginación con flechas, páginas cercanas y `...` para rangos lejanos.

| Prop | Tipo | Descripción |
|---|---|---|
| `paginaActual` | number | Página activa |
| `totalPaginas` | number | Total de páginas |
| `onPageChange` | function | Callback al cambiar de página |

#### `AlbumCard` — `src/components/ui/AlbumCard.jsx`

Tarjeta de álbum con badge de posición numerado. Para la sección Top Álbumes.

| Prop | Descripción |
|---|---|
| `id` | ID del álbum (Link a `/album/:id`) |
| `posicion` | Posición en el ranking (badge circular) |
| `album` | Título |
| `artista` | Nombre del artista |
| `rating` | Valoración media (0–5) |
| `portada` | URL de imagen (opcional) |

#### `CatalogoCard` — `src/components/ui/CatalogoCard.jsx`

Tarjeta de álbum con badge de género opcional en esquina superior derecha.

| Prop | Descripción |
|---|---|
| `id` | ID del álbum (Link a `/album/:id`) |
| `album` | Título |
| `artista` | Nombre del artista |
| `rating` | Valoración media |
| `genero` | Género (opcional — muestra badge si se pasa) |
| `portada` | URL de imagen (opcional) |

#### `ResenaCard` — `src/components/ui/ResenaCard.jsx`

Tarjeta vertical de reseña. Diseño: portada arriba, información debajo.

#### `AlbumRow` — `src/components/ui/AlbumRow.jsx`

Fila compacta horizontal de álbum. Usada en Búsqueda (sección "Añadidos recientemente").

---

## Páginas

### `Inicio` — `/`

Cuatro secciones:
1. **Hero** (`bg-card`): título, subtítulo, botón "Explorar álbumes", tarjeta destacada con portada placeholder
2. **Reseñas recientes** (`bg-background`): grid 4 columnas con `ResenaCard`
3. **Top Álbumes** (`bg-card`): grid 5 columnas con `AlbumCard` y badges numerados
4. **CTA** (`bg-surface`): llamada a la acción con botones "Crear cuenta" y "Entra aquí"

### `Login` — `/login`

Formulario centrado en pantalla. Incluye banner de error condicional (controlado por la constante `error` — se activará con el estado real de la llamada al backend).

### `Registro` — `/registro`

Formulario centrado: username, email, contraseña, confirmar contraseña.

### `Catálogo` — `/catalogo`

- Búsqueda por texto + filtro por género (chips) + selector de orden
- Grid 5 columnas con `CatalogoCard`
- Paginación
- Estado vacío cuando no hay resultados
- Filtrado y ordenación en cliente (con mock data); con backend se delegará al servidor

### `Búsqueda` — `/busqueda`

- Barra de búsqueda + chips de filtro (Todo / Álbumes / Artistas / Usuarios)
- Estado inicial: muestra "Tendencias" (grid 4 col) y "Añadidos recientemente" (lista)
- Al escribir: muestra estado vacío con mensaje (placeholder hasta conectar backend)

### `Rankings` — `/rankings`

- 4 stat cards (Álbumes, Artistas, Reseñas, Usuarios)
- Grid 2 columnas: Top Álbumes + distribución por Género (barras proporcionales)
- Grid 2 columnas: Top Artistas (con Link a `/artista/:id`) + Actividad reciente

### `DetalleAlbum` — `/album/:id`

- Header con portada, género, título, artista, año, rating, botones "Escribir reseña" y "Añadir a favoritos"
- El botón de favoritos tiene estado local (toggle visual mientras no hay backend)
- Lista de reseñas con avatar, usuario, fecha, estrellas y texto
- Estado vacío si no hay reseñas
- Sección "Más de [Artista]" con grid 4 columnas

### `DetalleArtista` — `/artista/:id`

- Header: avatar circular con ♪, nombre, botón "Seguir artista" (toggle local), país, género, stats (media, álbumes, reseñas)
- Biografía
- Discografía: grid 5 columnas con portadas
- Reseñas recientes: grid 2 columnas

### `CrearResena` — `/crear-resena`

Sin Navbar/Footer. Header propio con logo, título centrado y botón "Cancelar".
- Card del álbum a la izquierda (portada + título + artista + badge género)
- Formulario a la derecha: `EstrellasInteractivas` + textarea con contador de caracteres (máx. 2000)
- Botón "Publicar reseña" deshabilitado si puntuación = 0

### `EditarResena` — `/editar-resena`

Sin Navbar/Footer. Mismo layout que CrearResena.
- Card del álbum con badge "Editando" + card de detalles (fechas publicación/edición)
- Formulario pre-rellenado con la reseña existente
- Botones: "Guardar cambios" / "Eliminar reseña" (borde error) / "Cancelar"

### `PerfilUsuario` — `/perfil/:username`

- Header: avatar circular con inicial, username, "Miembro desde...", stats (reseñas y favoritos)
- Botón "Editar perfil" posicionado absolute top-right
- Tabs con indicador de línea verde deslizante: **Reseñas** y **Favoritos**
  - Tab Reseñas: lista de cards horizontales enlazadas al álbum
  - Tab Favoritos: grid 6 columnas con badge ♥

### `EditarPerfil` — `/editar-perfil`

Sin Navbar/Footer. Header propio con logo, título centrado y botón "Guardar".
- Avatar con borde verde y botón "Cambiar foto de perfil"
- Campos: username, email, biografía (textarea)
- Sección Seguridad: botón "Cambiar contraseña"
- Botón principal "Guardar cambios"
- Zona de peligro: "Desactivar cuenta" (borde error)

### `MisFavoritos` — `/favoritos`

- Grid 4 columnas con badge ♥ `bg-primary`
- Estado vacío con ♡ y enlace al catálogo

### `PanelAdmin` — `/admin`

- 4 stat cards (Inactivos en `text-error`)
- Grid 2 columnas: Gestión de contenido (4 botones-acción) + Gestión de usuarios (badges Activo/Inactivo) y Moderación

### `NotFound` — `*`

Número "404" a `text-[10rem]`, subtítulo, botón "Ir al inicio".

---

## Historial de desarrollo

### Fase 3 — Frontend

#### Sesión 1 — 23/04/2026: Configuración inicial y primeros componentes

- Proyecto creado con Vite + React
- Tailwind CSS v4 instalado y configurado
- Variables de diseño definidas en `index.css` con `@theme`: colores, tipografías, border-radius
- Google Fonts importado (Space Grotesk y Outfit, pesos 400/500/700)
- `App.css` vaciado (contenía CSS del template de Vite)
- `.vscode/settings.json` creado para suprimir el aviso `unknownAtRules` de `@theme`
- Componentes creados: `Navbar`, `SectionTitle`, `PortadaPlaceholder`, `Estrellas`, `ResenaCard`, `AlbumCard`
- Página `Inicio` completada con Hero, Reseñas recientes, Top Álbumes y CTA

#### Sesión 2 — 27/04/2026: Páginas restantes

Componentes adicionales: `EstrellasInteractivas`, `FormInput`, `GenreChip`, `CatalogoCard`, `SearchBar`, `SelectOrden`, `Paginacion`, `AlbumRow`

Páginas completadas:
- `Login` y `Registro`
- `Catálogo` con filtros, orden y paginación
- `Búsqueda` con estado inicial y chips de filtro
- `Rankings` con stats, tops y barras de género
- `DetalleAlbum` con reseñas y toggle de favoritos
- `DetalleArtista` con discografía y toggle de seguir
- `CrearResena` y `EditarResena` (pantallas sin Navbar)
- `PerfilUsuario` con tabs deslizantes
- `EditarPerfil` (pantalla sin Navbar)
- `MisFavoritos` con estado vacío
- `PanelAdmin` con gestión de contenido y usuarios
- `NotFound` con 404 grande

#### Sesión 3 — 28/04/2026: Optimización y corrección de bugs

**Bugs corregidos:**

1. **Token `text-muted` no definido** — Se añadió `--color-muted: #78908a` al bloque `@theme` de `index.css`. El token era usado en prácticamente todos los componentes (texto secundario, fechas, placeholders) pero no estaba definido en el design system, causando que Tailwind lo ignorase silenciosamente.

2. **`CatalogoCard` no recibía `id`** en `Catalogo.jsx` y `Busqueda.jsx` — Todos los links de las tarjetas del catálogo apuntaban a `/album/1` (el valor por defecto del prop). Corregido pasando `id={a.id}` explícitamente en ambas páginas.

3. **Typos en el hero de `Inicio.jsx`** — "Desubre" → "Descubre", "Esucha" → "Escucha".

**Limpieza de código:**

4. **Bloques de comentarios eliminados** en `Inicio.jsx` y `Navbar.jsx` — Se eliminaron ~35 y ~36 líneas respectivamente de comentarios que describían qué hacía cada clase CSS (e.g., `/* text-6xl → tamaño grande (60px) */`). Este tipo de comentarios no aportan valor a un lector que conozca Tailwind y enturbian la lectura del JSX.

5. **`NotFound.jsx`** — Reemplazado `style={{ fontSize: "10rem", lineHeight: 1 }}` inline por clases Tailwind `text-[10rem] leading-none`.

---

## Estado actual (28/04/2026)

**15 pantallas implementadas — frontend 100% completo con datos mock.**

Toda la lógica usa arrays hardcodeados en cada página. La siguiente fase conectará el frontend al backend sustituyendo los mocks por llamadas `fetch`/`axios` a la API REST, e implementará el contexto de autenticación (JWT) para que el Navbar muestre el avatar real del usuario logueado.
