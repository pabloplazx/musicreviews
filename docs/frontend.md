# MusicReviews — Frontend

Documentación del desarrollo del frontend: proceso, decisiones de diseño, problemas encontrados y soluciones adoptadas.

**Stack:** React 19 + Vite 8 + Tailwind CSS v4 + React Router DOM 7  
**Repositorio:** `pabloplazx/musicreviews-frontend`  
**Servidor de desarrollo:** `http://localhost:5173` (`npm run dev`)

---

## Fase 3, Sesión 1 — Configuración del entorno y design system (23/04/2026)

### Punto de partida

El backend estaba 100% completo (Java 21 + Spring Boot, MySQL en Aiven, JWT, 38 tests). Toca arrancar el frontend desde cero.

### Creación del proyecto

```bash
npm create vite@latest musicreviews-frontend -- --template react
npm install -D tailwindcss @tailwindcss/vite
npm install react-router-dom
```

`vite.config.js` configurado con el plugin de Tailwind. `src/index.css` reducido a `@import "tailwindcss"` (sintaxis de Tailwind v4, que ya no usa `tailwind.config.js`).

### Decisiones de diseño iniciales

**Estilo oscuro (dark mode):** Referencia principal Letterboxd. Una app de reseñas musicales pide un look serio y cinematográfico, no corporativo. Fondo casi negro.

**Tipografía inicial:** Bebas Neue (títulos condensados, impacto fuerte) + Inter (cuerpo, legible a cualquier tamaño). Máximo dos fuentes.

**→ Cambio de tipografía:** Al ver los primeros componentes en pantalla, Bebas Neue quedaba demasiado agresivo para una interfaz de reseñas. Se descartó en favor de **Space Grotesk** (titulares, botones) y **Outfit** (cuerpo). Space Grotesk tiene personalidad sin sacrificar legibilidad; Outfit es más suave que Inter para textos largos.

**Herramientas de diseño:**
- **Figma** — diseño de pantallas y prototipo antes de codificar
- **Realtime Colors** — previsualización de la paleta sobre una UI real antes de comprometerse con los valores hex
- **Google Fonts** — selección y carga de las fuentes

### Design system en `index.css`

Las variables de diseño se definen en el bloque `@theme` de Tailwind v4. Esto hace que cada token sea directamente una clase de utilidad (`bg-primary`, `text-muted`, `border-border`...) sin configuración adicional.

```css
@theme {
  --color-background: #060907;   /* fondo general */
  --color-card:       #0e1310;   /* tarjetas, navbar */
  --color-surface:    #16261E;   /* secciones CTA */
  --color-text:       #ebf0ed;   /* texto principal */
  --color-muted:      #78908a;   /* texto secundario, fechas */
  --color-primary:    #48a377;   /* verde — botones, links, logo */
  --color-secondary:  #226846;   /* hover de primario */
  --color-border:     #223228;   /* bordes de cards e inputs */
  --color-input:      #0a0f0c;   /* fondo de inputs */
  --color-error:      #cc3333;   /* errores */

  --font-heading: 'Space Grotesk', sans-serif;
  --font-body:    'Outfit', sans-serif;

  --radius-card:  1rem;       /* 16px — tarjetas */
  --radius-input: 0.625rem;   /* 10px — inputs y botones */
}
```

**Decisión sobre `--color-surface`:** añadido a posteriori cuando se implementó la sección CTA de la página Inicio. No estaba en el design system inicial, se detectó la necesidad al diseñar el bloque verde oscuro de fondo distinto al `card`.

**Problema de VSCode:** el bloque `@theme` generaba el aviso `unknownAtRules` en el editor porque no es CSS estándar. Solucionado con `.vscode/settings.json`:

```json
{ "css.customData": [], "css.lint.unknownAtRules": "ignore" }
```

### Estructura de carpetas

```
src/
├── components/
│   ├── ui/        ← componentes reutilizables
│   └── layout/    ← Navbar, Footer
├── pages/         ← una archivo por pantalla
├── services/      ← pendiente (llamadas al backend)
├── hooks/         ← pendiente (lógica reutilizable)
├── context/       ← pendiente (auth, token JWT)
├── assets/
├── App.jsx
└── index.css
```

### Componentes iniciales

**`Navbar.jsx`:** header sticky con logo `♪ MusicReviews` (Space Grotesk, `text-primary`), links de navegación (hover verde), botones Entrar (outline) y Registrarse (relleno verde). `z-50` para que quede por encima de cualquier contenido al hacer scroll.

**`SectionTitle.jsx`:** título de sección con línea decorativa verde de 32×4px debajo. Extraído como componente porque se repite exactamente igual en Inicio, Catálogo, Rankings y DetalleAlbum.

**`PortadaPlaceholder.jsx`:** caja oscura con `♪` centrado, para sustituir portadas reales mientras los datos son mock. Acepta `className` (dimensiones) e `iconSize` (tamaño del símbolo) como props para ser flexible en cualquier contexto.

**`Estrellas.jsx`:** valoración visual con soporte de medias estrellas. La media estrella se implementa superponiendo dos `★`: uno gris de fondo y otro verde recortado al 50% con `overflow-hidden w-1/2` en posición absoluta. Más simple que importar una librería de estrellas externa.

**`ResenaCard.jsx`** y **`AlbumCard.jsx`:** tarjetas de álbum/reseña para la página Inicio.

### Página Inicio — `/`

Cuatro secciones:
1. **Hero** (`bg-card`): título "Descubre. Escucha. Opina.", subtítulo, botón "Explorar álbumes", tarjeta destacada con `PortadaPlaceholder` y cita mock.
2. **Reseñas recientes** (`bg-background`): grid 4 columnas con `ResenaCard`.
3. **Top Álbumes** (`bg-card`): grid 5 columnas con `AlbumCard` y badges numerados posicionados con `absolute top-2 left-2`.
4. **CTA** (`bg-surface`): "¿Listo para compartir tu opinión musical?" + botones "Crear cuenta gratis" y "¿Ya tienes cuenta?".

**`Footer.jsx`:** copyright a la izquierda, links a la derecha. `bg-card border-t border-border`.

---

## Fase 3, Sesión 2 — Login, Registro, Catálogo y Búsqueda (26/04/2026)

### Repositorio independiente

El frontend vive en su propio repositorio GitHub (`pabloplazx/musicreviews-frontend`) separado del repo principal del TFG (`MusicReviews_TFG`). Esto permite deploys independientes en el futuro y mantiene limpio el historial de cada parte.

### Componente `FormInput`

Antes de implementar Login y Registro se extrajo `FormInput.jsx` — campo de formulario con label e input — porque ambas páginas comparten exactamente la misma estructura de campo. La prop `error={true}` cambia el borde a `border-error` para el estado de validación.

### Página Login — `/login`

Card centrada verticalmente con `min-h-[calc(100vh-64px)]` (resta la altura del Navbar para centrar en el espacio real disponible). Banner de error condicional preparado pero controlado por una constante `error = false` mientras no hay backend. El link "¿Olvidaste tu contraseña?" apunta a `#` de momento.

### Página Registro — `/registro`

Misma estructura visual que Login. Cuatro campos: username, email, contraseña, confirmar contraseña. Sin banner de error por ahora — los errores de validación se añadirán al conectar el backend.

### Componentes para Catálogo y Búsqueda

Antes de montar las páginas se identificaron y extrajeron todos los elementos reutilizables:

| Componente | Decisión de diseño |
|---|---|
| `GenreChip` | Chip con dos estados: activo (`bg-primary/20 text-primary border-primary`) e inactivo (`border-border text-muted`). Prop `active` booleano. |
| `CatalogoCard` | Badge de género **opcional** (`{genero && ...}`). Permite reutilizar la misma card en Búsqueda (sin badge) y en Catálogo (con badge). |
| `SearchBar` | `focus-within:border-primary` en el contenedor, no en el input. El borde se activa al hacer click en cualquier parte del componente, no solo en el campo. |
| `SelectOrden` | `<select>` nativo estilizado con clases Tailwind. Se descartó un select custom (más trabajo, más código) porque el nativo funciona igual de bien para este caso. |
| `Paginacion` | Muestra siempre primera y última página, las 2 adyacentes a la actual, y `...` para los saltos. |
| `AlbumRow` | Fila horizontal compacta para "Añadidos recientemente". |

### Página Catálogo — `/catalogo`

Filtrado, búsqueda y ordenación completamente funcionales en cliente con datos mock. La lógica:

```js
// Filtrar por género y texto de búsqueda
const albumsFiltrados = ALBUMS.filter((a) =>
  (generoActivo === "Todos" || a.genero === generoActivo) &&
  (a.album.toLowerCase().includes(busqueda.toLowerCase()) ||
   a.artista.toLowerCase().includes(busqueda.toLowerCase()))
);

// Ordenar copia del array (no mutar el estado)
const albumsOrdenados = [...albumsFiltrados].sort((a, b) => {
  if (orden === "mejor-valorados") return b.rating - a.rating;
  if (orden === "az") return a.album.localeCompare(b.album);
  return 0; // "recientes" lo ordena el backend por fecha
});
```

**Decisión:** `[...albumsFiltrados].sort()` en lugar de `.sort()` directo, porque `sort()` muta el array original. En este caso con mock no importa, pero cuando venga del backend se evita corromper el estado.

Tanto el filtro de género como la búsqueda de texto hacen `setPagina(1)` al cambiar para evitar que el usuario quede en una página vacía.

### Página Búsqueda — `/busqueda`

Tres estados distintos:
- **Filtro "Todo" + texto vacío** → contenido por defecto: Tendencias + Añadidos recientemente
- **Texto escrito** → estado "Sin resultados" (placeholder hasta conectar backend)
- **Filtro distinto de "Todo" + texto vacío** → pantalla vacía

**Decisión:** el estado "Sin resultados" se muestra en cuanto hay texto escrito, antes de consultar el backend. Al conectar, se condicionará a que la API devuelva array vacío en lugar de condicionarlo al texto.

### Navbar: icono de búsqueda

Se añadió el icono SVG de lupa al Navbar (entre los links y los botones), enlazado a `/busqueda`. SVG inline en lugar de importar una librería de iconos para no añadir dependencias por un solo icono.

---

## Fase 3, Sesión 3 — Rankings, Detalles, Crear Reseña (27/04/2026, sesión 1)

### Metodología con Figma MCP

A partir de esta sesión se trabajó consultando el diseño directamente en Figma Desktop a través del MCP antes de codificar cada pantalla. Esto garantizó fidelidad con el prototipo y evitó iterar sobre el diseño a ciegas.

### Refactorización de `App.jsx` — patrón SIN_NAVBAR

Al implementar Crear Reseña se detectó la necesidad de pantallas sin Navbar ni Footer. Se refactorizó `App.jsx` para extraer un componente interno `Layout` con `useLocation`:

```js
const SIN_NAVBAR = ["/crear-resena", "/editar-resena", "/editar-perfil"];

function Layout() {
  const { pathname } = useLocation();
  const sinNavbar = SIN_NAVBAR.includes(pathname);
  return (
    <div className="bg-background min-h-screen text-text font-body">
      {!sinNavbar && <Navbar />}
      <Routes>...</Routes>
      {!sinNavbar && <Footer />}
    </div>
  );
}
```

**Por qué un componente `Layout` en lugar de condiciones en cada página:** centraliza la lógica en un solo sitio. Añadir una nueva ruta sin Navbar solo requiere añadir la ruta al array, no modificar la página en cuestión.

### Página Rankings — `/rankings`

- 4 stat cards en fila.
- Barras de progreso de géneros calculadas proporcionalmente: `width: ${(g.cantidad / maxGenero) * 100}%`. El primero del array siempre ocupa el 100%, los demás son relativos a él.
- Las filas de Top Artistas son `Link` a `/artista/:posicion`. Las de Actividad reciente son `Link` a `/perfil/:usuario`.

### Página Detalle de Álbum — `/album/:id`

- Botón "Añadir a favoritos" con **toggle local** (`useState(false)`): alterna entre `♡ Añadir a favoritos` (outline) y `♥ En favoritos` (fondo `primary/10`). Cuando haya backend, este estado se inicializará consultando `GET /api/favoritos/usuario` y las acciones llamarán a `POST /DELETE /api/favoritos`.
- Estado vacío de reseñas condicionado a `RESENAS.length > 0`. La condición no cambia al conectar backend, solo cambia el origen del array.
- El nombre del artista es un `Link` a `/artista/1` (ID hardcodeado hasta que `useParams` lea el `:id` real).

### Página Detalle de Artista — `/artista/:id`

- Botón "Seguir artista" con toggle local. Funcionalidad de seguimiento no existe en el backend actual — es visual puro, se implementará si da tiempo antes del 4 de junio.
- Stats en card horizontal con `border-r border-border` como divisores. Se descartó usar `gap` porque los divisores visuales son parte del diseño.

### Página Crear Reseña — `/crear-resena`

**Header centrado con posicionamiento absoluto:**

```jsx
<h1 className="... absolute left-1/2 -translate-x-1/2">Nueva reseña</h1>
```

El logo queda a la izquierda y el botón a la derecha con `justify-between`. El título está físicamente fuera del flujo normal pero centrado respecto a la pantalla. Funciona porque el header tiene `position: relative` implícito.

**`EstrellasInteractivas`:** componente para seleccionar puntuación con soporte de medias estrellas. Detecta en qué mitad de la estrella está el cursor con `getBoundingClientRect`:

```js
function handleMouseMove(e, i) {
  const rect = e.currentTarget.getBoundingClientRect();
  const mitad = e.clientX - rect.left < rect.width / 2;
  setHover(mitad ? i + 0.5 : i + 1);
}
```

**Decisión:** se implementó como componente local dentro de `CrearResena.jsx` inicialmente, porque no estaba claro si se reutilizaría. Al implementar `EditarResena` se detectó la duplicación y se extrajo a `src/components/ui/EstrellasInteractivas.jsx`.

---

## Fase 3, Sesión 4 — Pantallas restantes y cierre (27/04/2026, sesión 2)

### Extracción de `EstrellasInteractivas`

`EditarResena.jsx` necesitaba el mismo selector de estrellas que `CrearResena.jsx`. El componente estaba duplicado. Se extrajo a `src/components/ui/` y ambas páginas actualizadas para importarlo. Esta es la única refactorización inducida por duplicación real en todo el frontend.

### Página Editar Reseña — `/editar-resena`

- Card "Detalles de tu reseña" con divisor `<div class="w-full h-px bg-border" />`. Se usó un `div` en lugar de `<hr>` para poder controlarlo exactamente con Tailwind.
- El badge "Editando" usa `bg-primary/20` (verde muy diluido) en lugar del chip de género para diferenciar visualmente que es una vista de edición, no de detalle.
- Tres botones con jerarquía visual clara: primario (guardar, `bg-primary`), destructivo (eliminar, `border-error hover:bg-error/10`) y secundario (cancelar, solo texto).

### Página Perfil de Usuario — `/perfil/:username`

- El botón "Editar perfil" usa `absolute top-0 right-0` dentro del header del perfil (`position: relative`). Esto lo ancla a la esquina superior derecha del header sin depender del flujo del documento.
- Tabs con indicador de línea verde: `<span class="absolute bottom-0 left-0 w-full h-0.5 bg-primary" />` dentro del botón del tab activo. Efecto visual limpio sin librería de tabs.
- El tab Favoritos usa grid 6 columnas (más denso que el catálogo en 5) porque son álbumes personales, se espera que el usuario quiera ver más de un vistazo.

**Bug corregido en esta sesión:** el badge del corazón en los favoritos tenía `bg-white text-primary`. Incorrecto — sobre fondo blanco el ♥ verde no se ve bien y rompe la consistencia del design system. Corregido a `bg-primary text-white`.

### Página Editar Perfil — `/editar-perfil`

- El avatar muestra `♪` en lugar de la inicial del usuario porque en este punto no hay foto real. Cuando haya upload de foto, se mostrará la imagen; si no hay, la inicial.
- "Zona de peligro" con heading `text-error` y botón "Desactivar cuenta" con `border-error hover:bg-error/10`. El hover rellena sutilmente de rojo para dar feedback visual sin ser agresivo.

### Página Mis Favoritos — `/favoritos`

- Estado vacío con ♡ en `text-border` (el verde muy oscuro del design system), no en `text-muted`. El corazón vacío en color de borde queda más sutil que en gris.

### Página Panel Admin — `/admin`

- La stat card de "Inactivos" usa `text-error` en lugar de `text-primary` para que destaque visualmente como un número que requiere atención.
- Los badges de usuario Activo/Inactivo usan colores con opacidad (`bg-primary/20`, `bg-error/20`) en lugar de fondos sólidos para no competir visualmente con el resto de la fila.

### Página 404 Not Found — `*`

- El número "404" a `font-size: 10rem` inicialmente implementado con `style={{ fontSize: "10rem" }}` inline (no hay clase Tailwind para ese tamaño). **Corregido en sesión de optimización** a `text-[10rem]` (valor arbitrario de Tailwind v4).

---

## Fase 3, Sesión 5 — Optimización y corrección de bugs (28/04/2026)

### Bug 1 — `text-muted` no definido en el design system

**Problema:** el token `text-muted` se usaba en prácticamente todos los componentes (texto secundario, fechas, placeholders, artistas) pero `--color-muted` no estaba definido en el bloque `@theme` de `index.css`. Tailwind v4 ignora silenciosamente los tokens desconocidos, de modo que todos esos textos heredaban el color del padre (casi siempre `text-text`, es decir blanco) y la jerarquía visual de la interfaz era incorrecta.

**Causa:** la paleta se definió con los colores principales pero se omitió el gris de texto secundario. El valor `#78908a` estaba referenciado en comentarios del código pero nunca llegó al `@theme`.

**Solución:**
```css
--color-muted: #78908a;  /* añadido a @theme en index.css */
```

**Impacto:** afectaba a todos los archivos del proyecto. Con este token corregido, las fechas, artistas secundarios, placeholders y labels pasan de blanco a gris, restaurando la jerarquía tipográfica de la interfaz.

### Bug 2 — `CatalogoCard` sin `id` en Catálogo y Búsqueda

**Problema:** `CatalogoCard` acepta un prop `id` con default `1`. En `Catalogo.jsx` y `Busqueda.jsx` se renderizaba la card con los datos del álbum pero sin pasar `id={a.id}`. Resultado: todos los links del catálogo apuntaban a `/album/1` independientemente del álbum.

**Causa:** error de omisión al montar las páginas. El prop `id` se añadió al componente, pero al usarlo en las páginas se pasaron el resto de props y se olvidó este.

**Solución:** añadir `id={a.id}` en los dos sitios donde se usa `CatalogoCard`:
```jsx
// Catalogo.jsx y Busqueda.jsx
<CatalogoCard id={a.id} album={a.album} artista={a.artista} ... />
```

### Bug 3 — Typos en el hero de Inicio

**Problema:** el título del hero decía "Desubre. Esucha. Opina." — faltan dos letras ("Descubre" y "Escucha").

**Causa:** error de tipeo directo al escribir el JSX.

**Solución:** corrección directa en `Inicio.jsx`.

### Bug 4 — `style` inline en `NotFound.jsx`

**Problema:** el número "404" usaba `style={{ fontSize: "10rem", lineHeight: 1 }}` inline porque no existe una clase Tailwind estándar para ese tamaño. Rompe la consistencia del código (todo el resto del proyecto usa solo clases Tailwind).

**Solución:** Tailwind v4 soporta valores arbitrarios directamente como clase:
```jsx
<span className="text-[10rem] leading-none">404</span>
```

### Limpieza — bloques de comentarios eliminados

`Inicio.jsx` tenía 35 líneas de comentarios al inicio del archivo explicando qué hacía cada clase Tailwind (`/* text-6xl → tamaño grande (60px) */`, etc.). `Navbar.jsx` tenía 36 líneas similares. Estos comentarios describen **qué** hace el código, no **por qué** — información que cualquier desarrollador familiarizado con Tailwind ya conoce. Eliminados en su totalidad.

---

## Estado del frontend (28/04/2026)

**15 pantallas — 100% implementadas visualmente con datos mock.**

La siguiente fase es la integración con el backend:
- Reemplazar los arrays mock por llamadas `fetch`/`axios` en los archivos de `services/`
- Implementar el contexto de autenticación (JWT) en `context/`
- Navbar: mostrar avatar real si hay usuario logueado; Entrar + Registrarse si no
- Conectar los toggles de favorito y seguir artista a los endpoints reales de la API

### Tabla de pantallas

| Pantalla | Ruta | Notas |
|---|---|---|
| Inicio | `/` | Hero + Reseñas recientes + Top Álbumes + CTA |
| Login | `/login` | Banner de error preparado para backend |
| Registro | `/registro` | — |
| Catálogo | `/catalogo` | Filtro + búsqueda + orden + paginación en cliente |
| Búsqueda | `/busqueda` | 3 estados: inicio / buscando / sin resultados |
| Rankings | `/rankings` | Stats + tops + barras de género |
| Detalle de Álbum | `/album/:id` | Toggle favorito local |
| Detalle de Artista | `/artista/:id` | Toggle seguir artista local |
| Crear Reseña | `/crear-resena` | Sin Navbar, EstrellasInteractivas, textarea con contador |
| Editar Reseña | `/editar-resena` | Sin Navbar, pre-rellenado, botón eliminar |
| Perfil de Usuario | `/perfil/:username` | Tabs Reseñas/Favoritos con indicador deslizante |
| Editar Perfil | `/editar-perfil` | Sin Navbar, zona de peligro |
| Mis Favoritos | `/favoritos` | Estado vacío con ♡ |
| Panel Admin | `/admin` | Stats + gestión de contenido y usuarios |
| 404 Not Found | `*` | — |
