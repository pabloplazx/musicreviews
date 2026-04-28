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

---

## Fase 4 — Integración con el backend (28/04/2026 →)

> Documentación completa y autocontenida de esta fase: [`integracion.md`](integracion.md). Aquí solo el resumen para mantener `frontend.md` como índice general.

### Plan (9 pasos)

1. **AuthContext** — estado React compartido del usuario y token, persistido en localStorage.
2. **Login + Registro** — formularios reales contra `POST /api/auth/login` y `/register`.
3. **Navbar dinámico** — botones según haya o no sesión; opción de logout.
4. **Rutas protegidas** — redirección a `/login` si se entra sin sesión a páginas que la requieren.
5. **Páginas públicas** — Catálogo, Búsqueda, Rankings con datos reales del backend.
6. **Páginas de álbum** — Detalle de álbum y artista con datos y reseñas reales.
7. **Páginas de usuario** — Perfil, editar perfil, favoritos.
8. **Reseñas** — crear, editar, borrar desde la UI.
9. **Portadas** — gestión de imágenes de álbum y artista.

### Sesión 1 (28/04/2026) — pasos 1 y 2 + arreglo del backend

Lo abordado:

- `AuthContext.jsx`, `services/auth.js`, `Login.jsx`, `Registro.jsx` validados contra el backend real. Login con maría funcional end-to-end.
- **5 bugs del backend** detectados al integrar y arreglados (ver [`integracion.md` § 2](integracion.md) y [`pruebas_postman.md`](pruebas_postman.md) sección "Bugs encontrados durante la integración"):
  - B1: `LazyInitializationException` con `open-in-view=false`
  - B2: `@JsonAutoDetect` rompía proxies de Hibernate
  - B3: `refresh()` antes del flush en `ResenaService.actualizar`
  - B4: Login/registro devolvían texto plano en errores
  - B5: Test unitario sin mock de `EntityManager`
- **38 tests unitarios** verdes tras los arreglos.
- **6 lotes de pruebas Postman** cubriendo login + CRUD reseñas + CRUD favoritos + casos de error.

### Sesión 2 (28/04/2026) — paso 3: Navbar dinámico

Solo se tocó `src/components/layout/Navbar.jsx`. Es el primer sitio fuera de los formularios de auth donde se consume el contexto.

- `useAuth()` da `usuario` y `logout`.
- Renderizado condicional con un ternario sobre `usuario`: sin sesión muestra botones "Entrar"/"Registrarse"; con sesión muestra ♥ favoritos, avatar con la inicial real del username, botón "Salir" y, si `rol === "ADMIN"`, un link al panel admin.
- Logout = `logout()` + `navigate("/")` para no dejar al usuario en una página privada con datos cacheados.
- Logo del navbar ahora enlaza a `/` (antes no era clicable).
- `aria-label` en iconos sin texto (accesibilidad).

Detalle completo y motivación de cada decisión: [`integracion.md` § 4](integracion.md).

### Sesión 3 (28/04/2026) — paso 4: Rutas protegidas

Hasta ahora el navbar oculta los enlaces a páginas privadas, pero las rutas siguen siendo accesibles escribiendo la URL a mano. El paso 4 añade protección **a nivel de ruta**.

**Ficheros nuevos** (en `src/components/routing/`):

- `RutaProtegida.jsx`: comprueba `usuario` del contexto. Sin sesión → redirige a `/login` guardando la URL actual en `location.state.from`. Con sesión → renderiza `<Outlet />`.
- `RutaAdmin.jsx`: igual + comprobación de `rol === "ADMIN"`. Sin rol → redirige a `/` (NO a `/login` para evitar bucle).

**Ficheros modificados:**

- `App.jsx`: rutas reagrupadas en 3 bloques (públicas / protegidas envueltas en `<RutaProtegida>` / admin envueltas en `<RutaAdmin>`).
- `Login.jsx`: tras autenticar, navega a `location.state.from?.pathname` (con `replace: true`) en lugar de siempre a `/`. Así el usuario que fue rebotado desde `/favoritos` vuelve allí tras login.

**Pruebas realizadas:** 5 casos manuales cubriendo redirección sin sesión, vuelta a la URL original tras login, USER no entra a `/admin`, ADMIN entra y rutas públicas siguen funcionando con/sin sesión. Para probar el rol ADMIN se creó un usuario `admin@musicreviews.com / admin123` (registro via API + UPDATE rol vía MySQL Shell en Aiven, ya que `POST /register` siempre crea con rol USER).

Detalle completo, código y verificaciones: [`integracion.md` § 6](integracion.md).

### Sesión 4 (28/04/2026) — paso 5: páginas públicas con datos reales

Reemplazo de los datos mock de Inicio, Catálogo, Búsqueda y Rankings por datos reales del backend.

**Servicios nuevos** (uno por dominio del backend, igual que `auth.js`):

- `services/albumes.js` — listado paginado y detalle de álbumes
- `services/artistas.js` — listado y detalle de artistas
- `services/estadisticas.js` — resumen, rankings, géneros, actividad reciente

**Páginas conectadas:**

- `Inicio.jsx` — Reseñas recientes + Top Álbumes reales. Hero rediseñado: el mock estático "DAMN." sustituido por la mejor reseña reciente con su portada real, link al detalle.
- `Catalogo.jsx` — paginación server-side, géneros dinámicos (top 8 de la BD), búsqueda con **debounce 300 ms**, solo orden A→Z (limitación del backend).
- `Busqueda.jsx` — tendencias + recientes + búsqueda con debounce.
- `Rankings.jsx` — 5 fetches en paralelo con `Promise.all`.

**Componentes UI tocados:**

- `CatalogoCard.jsx` — `rating` opcional (el listado paginado del backend no incluye puntuación).
- `ResenaCard.jsx` — convertido a `<Link>` para que las reseñas sean clicables.

**Limitaciones conocidas:** el catálogo no muestra estrellas (el backend no devuelve `puntuacionMedia` en el listado), solo orden A→Z (no acepta `sort`), búsqueda solo por título de álbum.

Detalle, decisiones técnicas, código y 12 casos verificados: [`integracion.md` § 7](integracion.md).

### Sesión 5 (28/04/2026) — paso 6: detalle de álbum y de artista

Páginas de detalle conectadas, incluyendo el toggle de favoritos funcional con auth.

**Servicios nuevos:**

- `services/resenas.js` — funciones públicas: `getResenasPorAlbum`, `getResenasPorUsuario`, `getResenaUsuarioAlbum`.
- `services/favoritos.js` — todas con token: `esFavorito`, `getFavoritosUsuario`, `agregarFavorito`, `quitarFavorito`.

**Páginas conectadas:**

- `DetalleAlbum.jsx` — `useParams` + `Promise.all([getAlbum, getResenasPorAlbum])` + segunda fetch encadenada para "Más del artista". Toggle favorito real con auth (POST/DELETE), guard contra doble click. Sin sesión, el botón se convierte en `<Link>` a login. Reseñas con username clicable y fecha en español. Puntuación media calculada en cliente.
- `DetalleArtista.jsx` — `Promise.all([getArtista, getAlbumes({artistaId})])`. Discografía ordenada por fecha desc. Stats reducidas a "Álbumes" porque el backend no expone media/total de reseñas por artista. Botón "Seguir artista" eliminado (no hay endpoint).

**Limitaciones conocidas que se documentan:**

- No hay endpoint para todas las reseñas de un artista → sin sección reseñas recientes en DetalleArtista.
- No hay endpoint de "seguir artista" → botón quitado.
- No hay endpoint para stats agregadas por artista (media, total reseñas).

**10 casos verificados manualmente.** Detalle completo: [`integracion.md` § 8](integracion.md).

### Sesión 6 (28/04/2026) — paso 7: páginas de usuario

Las tres páginas relacionadas con el usuario conectadas con el backend.

**Servicio nuevo:** `services/usuarios.js` con `getUsuarioPorUsername` (público) y `actualizarUsuario(id, datos, token)` (con auth).

**`AuthContext.jsx`:** nuevo método `actualizarUsuarioLocal(datosUsuario)` para sincronizar el contexto + localStorage tras editar perfil sin tocar el token.

**Páginas conectadas:**

- `PerfilUsuario.jsx` — `useParams` para `:username`; cadena de 2-3 fetches (datos, reseñas, favoritos si hay sesión); tab Favoritos con tres estados según haya o no sesión; botón "Editar perfil" solo si `esMiPerfil`.
- `EditarPerfil.jsx` — inicializado desde `useAuth`; PUT con auth y sincronización del contexto tras éxito; email read-only (el backend no lo permite cambiar); URL en lugar de upload (paso 9 simplificado). Botones de "Cambiar contraseña" y "Desactivar cuenta" eliminados (no hay endpoints).
- `MisFavoritos.jsx` — ruta protegida; quitar inline con `e.preventDefault/stopPropagation` para no disparar el Link envolvente; optimistic update.

**Limitaciones honestas:** subida de archivos no implementada (URL como workaround), cambio de contraseña no implementado, desactivar cuenta no implementado (DELETE borra de verdad).

**9 casos verificados manualmente.** Detalle: [`integracion.md` § 9](integracion.md).

### Sesión 7 (28/04/2026) — paso 8: CRUD de reseñas (cierre de la integración)

Última sesión de la fase 4. Conexión de las dos pantallas restantes y mejoras transversales.

**`services/resenas.js` ampliado** con `crearResena`, `actualizarResena`, `borrarResena` (todas con token).

**Páginas conectadas:**

- `CrearResena.jsx` — recibe `albumId` por `location.state` desde DetalleAlbum, carga el álbum para previsualizar, POST con auth, redirige al álbum tras éxito. Pantalla de aviso si se entra sin albumId.
- `EditarResena.jsx` — `getResenaUsuarioAlbum` para cargar la reseña; si no existe, redirige a CrearResena. PUT y DELETE con auth, `window.confirm` para confirmar borrado, muestra fechas de publicación y última edición reales.

**Mejoras transversales:**

- `DetalleAlbum.jsx` detecta si el usuario logueado ya tiene reseña y cambia "Escribir reseña" por "Editar mi reseña". Evita el 400 "ya has reseñado".
- `PerfilUsuario.jsx` añade botón "✎ Editar" en cada reseña si `esMiPerfil`.

**12 casos del CRUD completo verificados manualmente.**

### Estado final de la fase 4 (tras paso 8)

✅ **Integración completa frontend ↔ backend.** Las 15 pantallas tienen contenido real, navegación coherente y manejo de errores.

ℹ️ **Paso 9 (subida de archivos)** simplificado a "URL como input" en el paso 7. La subida real queda como mejora futura.

Detalle completo: [`integracion.md` § 10](integracion.md).

### Sesión 8 (28/04/2026) — paso 9 reformulado: panel admin funcional + fix búsqueda

El "paso 9" original del plan (subida de archivos) ya se simplificó a URL en paso 7. Lo que se hace ahora con ese número es **el panel de administración funcional** y un **fix puntual de búsqueda** que mejoraban la integración.

**Backend (6 cambios):**

- `AlbumRepository`: método `findByTituloContainingIgnoreCaseOrArtistaNombreContainingIgnoreCase`.
- `AlbumService.buscar(texto, pageable)` y `AlbumController` con parámetro `?q=`.
- `UsuarioController`: `PATCH /api/usuarios/{id}/activo` con body `{activo: boolean}`.
- `UsuarioService.cambiarActivo(id, activo)`.
- `SecurityConfig`: `GET /api/usuarios` y `PATCH /api/usuarios/**` solo ADMIN (antes `GET` exponía todos los emails a cualquier autenticado).

**Frontend:**

- `services/usuarios.js` ampliado con `getUsuarios` y `cambiarEstadoActivo`.
- `services/artistas.js` ampliado con `crearArtista`.
- `services/albumes.js` ampliado con parámetro `q`.
- `Catalogo.jsx` y `Busqueda.jsx` pasan `q` en lugar de `titulo`.
- `PanelAdmin.jsx` reescrito por completo: stats reales, gestión de usuarios con toggle activar/desactivar, formulario de nuevo artista, moderación de reseñas con borrar.

**Limitación honesta:** el endpoint `DELETE /api/resenas/{id}` no verifica owner/admin en backend. Protección solo a nivel de UI. Mejora futura.

**13 casos verificados manualmente. 38/38 tests verdes.**

Detalle completo: [`integracion.md` § 11](integracion.md).

### Pulido final (mismo día, commits sueltos posteriores)

- **Orden en /catalogo** funcional con 4 opciones: A→Z, Z→A, Más recientes, Más antiguos (`?sort=` en backend).
- **Hero del Inicio**: card aumentada de `w-55` a `w-80`, tipografía ampliada, lógica de elección cambiada para que la reseña destacada tenga **comentario** (no solo estrellas).
- **Color del Hero**: `bg-card` → `bg-surface` para diferenciarlo del navbar y de las cards interiores. Se reutiliza el verde oscuro que ya estaba en la paleta para la CTA del final, mantiene coherencia.

### Decisiones técnicas

| Decisión | Razón |
|---|---|
| Context API en vez de Redux/Zustand | El estado compartido se reduce a `usuario` y `token`. Context resuelve eso en 50 líneas. Redux sería over-engineering para un TFG. |
| `localStorage` en vez de cookies HttpOnly | El backend devuelve el JWT en el body. Para el header `Authorization: Bearer ...` lo más simple es localStorage. Cookies HttpOnly serían más seguras contra XSS pero requieren cambiar el backend. |
| `fetch` nativo en vez de Axios | App con pocos endpoints. No se justifica una dependencia de 30 KB extra solo por azúcar sintáctico. Si en el futuro hace falta interceptores se reevalúa. |
| Capa `services/` separada del contexto | El contexto solo gestiona estado React. La red está en `services/auth.js`. Si mañana se cambia a Axios o se añade renovación automática de token, solo se toca un fichero. |
| Errores como `throw new Error(mensaje)` | Idiomático en JS async — quien llama hace `try/catch`. El `mensaje` viene del campo `mensaje` del JSON del backend (`GlobalExceptionHandler`). |

### Pendiente al cerrar la sesión 1

Pasos 3 al 9 del plan. El paso 3 (Navbar dinámico) es el siguiente: primera vez que el contexto se usa fuera de los formularios de auth.
