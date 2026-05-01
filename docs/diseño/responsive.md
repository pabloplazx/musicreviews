# Adaptación responsive — MusicReviews Frontend

Documentación del proceso de adaptación del frontend para que funcione correctamente en móvil y tablet, manteniendo intacto el diseño de escritorio.

**Punto de partida:** Frontend React 19 + Vite + Tailwind CSS, diseñado en Figma a 1440 px de ancho y maquetado pixel-perfect a esa resolución (ver `diseño_figma.md`). Las 15 pantallas funcionaban perfectamente en escritorio pero rompían en cualquier viewport por debajo de ~900 px: padding lateral excesivo, grids con 4–6 columnas que reducían las tarjetas a tamaños ilegibles, headers con elementos desbordados y un navbar horizontal sin alternativa para móvil.

**Objetivo:** producir una experiencia móvil/tablet usable sin alterar nada de lo que un usuario en escritorio (≥ 1024 px) ve hoy.

---

## Estrategia: mobile-first additive

Tailwind aplica las clases sin prefijo a **todos los tamaños de pantalla** (es la base) y los prefijos `sm:`, `md:`, `lg:`, `xl:` activan estilos **a partir de** ese breakpoint. Esto permite dos patrones:

1. **Desktop-first (sustractivo):** se escriben clases para escritorio y se sobreescriben en breakpoints menores. Riesgo: tocar el comportamiento de escritorio.
2. **Mobile-first (aditivo):** se escriben clases para móvil como base y se añaden prefijos para *enriquecer* el diseño en pantallas más grandes. La capa de escritorio queda explícitamente en `lg:`/`xl:`, idéntica a la original.

Se ha aplicado el patrón **2** porque preserva el diseño de escritorio sin tocar las decisiones de Figma. El criterio de equivalencia es:

> A 1024 px o más, todas las pantallas deben renderizar exactamente igual que antes de la adaptación.

Concretamente, donde antes había `px-12` se escribió `px-4 sm:px-6 lg:px-12`:

| Viewport | Padding lateral | Comentario |
|---|---|---|
| < 640 px (móvil) | 16 px (`px-4`) | Nuevo: cómodo en pantallas estrechas |
| 640–1023 px (tablet) | 24 px (`px-6`) | Nuevo: equilibrado para tablet |
| ≥ 1024 px (escritorio) | 48 px (`px-12`) | **Idéntico al diseño Figma original** |

---

## Breakpoints utilizados

Solo se han usado tres breakpoints de Tailwind, suficientes para cubrir el espectro típico:

| Prefijo | Min-width | Dispositivo objetivo |
|---|---|---|
| (sin prefijo) | 0 px | Móviles (320–639 px) |
| `sm:` | 640 px | Tablets verticales pequeñas (~iPad mini portrait) |
| `lg:` | 1024 px | Escritorio y portátiles |

`md:` (768 px) y `xl:` (1280 px) se han usado puntualmente solo cuando la transición visual lo requería — por ejemplo, el grid del catálogo necesita un escalón extra a `md:` para no saltar de 3 a 5 columnas.

---

## Patrones aplicados

### Container universal

Todas las páginas y los layouts (Navbar, Footer) comparten un contenedor:

```jsx
// Antes
<div className="max-w-300 mx-auto px-12">

// Después
<div className="max-w-300 mx-auto px-4 sm:px-6 lg:px-12">
```

`max-w-300` (1200 px) y `mx-auto` ya escalaban correctamente — solo el padding lateral necesitaba degradación móvil.

### Grids de tarjetas

Las páginas con grids fijos a 4–6 columnas se rompen en móvil porque las tarjetas quedan a 50–80 px de ancho. La regla general aplicada:

```jsx
// Antes
<div className="grid grid-cols-4 gap-6">      // Reseñas Inicio
<div className="grid grid-cols-5 gap-6">      // Top álbumes, Catálogo, Discografía
<div className="grid grid-cols-6 gap-4">      // Favoritos en perfil

// Después
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
<div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4 sm:gap-6">
<div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
```

El número de columnas en escritorio (`lg:grid-cols-N`) preserva el diseño original.

### Layouts horizontales que apilan en móvil

Cabeceras tipo "imagen + información" (`DetalleAlbum`, `DetalleArtista`, `PerfilUsuario`, `Inicio` Hero) usaban `flex` con dos columnas. En móvil se apilan verticalmente y centran:

```jsx
// Antes
<div className="flex gap-10 items-start">

// Después
<div className="flex flex-col sm:flex-row gap-6 sm:gap-10 items-center sm:items-start text-center sm:text-left">
```

### Tipografía adaptativa

Los tamaños grandes del Design System (`text-6xl` ≈ 60 px, `text-5xl` ≈ 48 px) son legítimos en una pantalla de 1440 px pero ocupan media pantalla en un móvil de 375 px. Se han escalado:

| Antes | Después | Uso |
|---|---|---|
| `text-6xl` | `text-4xl sm:text-5xl lg:text-6xl` | Hero del Inicio |
| `text-5xl` | `text-3xl sm:text-4xl lg:text-5xl` | CTA del Inicio |
| `text-4xl` | `text-3xl sm:text-4xl` | Títulos de página (Catálogo, Rankings, Mis Favoritos…) |
| `text-3xl` | `text-2xl sm:text-3xl` | Títulos secundarios (Perfil Usuario) |
| `text-[10rem]` | `text-7xl sm:text-9xl lg:text-[10rem]` | "404" del NotFound |

### Padding vertical

Las secciones con `py-20` (80 px) en escritorio se reducen a `py-12 sm:py-16 lg:py-20` para no dejar tanto vacío en móvil.

---

## Cambio estructural: menú hamburguesa

El Navbar tenía 3 bloques en una sola fila (logo, links principales, acciones) que ocupan ~900 px de ancho mínimo. Por debajo, los elementos se solapan o desbordan.

La solución implementa el patrón estándar de menú móvil:

- **`lg:` (≥ 1024 px):** Layout original sin cambios. Los tres bloques se renderizan en horizontal.
- **`<lg:` (móvil/tablet):** Logo a la izquierda, acciones imprescindibles (búsqueda, avatar/login compacto) en el centro-derecha, y un botón hamburguesa (`☰`) que abre/cierra un drawer vertical bajo el header con todos los links de navegación y los botones de auth.

Estado React añadido:

```jsx
const [menuAbierto, setMenuAbierto] = useState(false);
```

El drawer se cierra automáticamente al navegar a cualquier link (`onClick={cerrarMenu}` en cada `<Link>`) para no quedar pegado tras un cambio de ruta.

Atributos de accesibilidad en el botón:

```jsx
<button
  aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
  aria-expanded={menuAbierto}
>
```

---

## Cambios menores transversales

- **`flex-wrap` en filas con muchos elementos** (`PanelAdmin` lista de usuarios): los badges de estado/rol y el botón de acción ahora envuelven a una segunda fila en móvil en lugar de salirse de la card.
- **`break-words` en comentarios largos** (`DetalleAlbum`): evita que reseñas con palabras muy largas (URLs, emojis pegados) rompan el ancho de la card.
- **`whitespace-nowrap` en fechas** (`PerfilUsuario`): impide que "12 may. 2026" se parta en dos líneas en hueco apretado.
- **Botones full-width en móvil** (`CrearResena`, `EditarResena`): `w-full sm:w-auto` para que el CTA principal sea fácil de pulsar.
- **Header de páginas con layout standalone** (`CrearResena`, `EditarResena`, `EditarPerfil`): el título centrado con `absolute left-1/2 -translate-x-1/2` se desactiva en móvil con `lg:absolute lg:left-1/2 lg:-translate-x-1/2`. En su lugar, el título se coloca en el flujo flex normal sin colisionar con los botones laterales.
- **Logo compacto en headers de pantallas modales:** "♪ MusicReviews" → solo "♪" en móvil con `<span className="hidden sm:inline">MusicReviews</span>`.

---

## Páginas no modificadas

`Login` y `Registro` ya partían de un layout responsive correcto: card centrada con `min-h-[calc(100vh-64px)] flex items-center justify-center px-4` y `w-full max-w-md`. Se han verificado pero no han requerido cambios.

Los componentes UI compartidos (`AlbumCard`, `CatalogoCard`, `ResenaCard`, `AlbumRow`, `GenreChip`, `SearchBar`, `Paginacion`, `FormInput`, `Estrellas`) tampoco necesitaron cambios. Usan `aspect-square` (responsive intrínseco), `truncate` para textos largos, padding compacto y `w-full`. El comportamiento responsive de las tarjetas viene determinado por el grid contenedor, que sí se ha adaptado en cada página.

---

## Resumen de archivos tocados

| Archivo | Cambio |
|---|---|
| `src/components/layout/Navbar.jsx` | Reescrito con drawer móvil + estado `menuAbierto` |
| `src/components/layout/Footer.jsx` | Stack vertical en móvil, padding responsive |
| `src/pages/Inicio.jsx` | Hero apilado, grids 4→1/2 y 5→2/3, tipografías y paddings adaptados |
| `src/pages/Catalogo.jsx` | Grid 5→2/3/4, filtros + selector apilados, paddings |
| `src/pages/Busqueda.jsx` | Chips wrap, grids 4→2/3, paddings |
| `src/pages/Rankings.jsx` | Stats 4→2, secciones apiladas en móvil |
| `src/pages/DetalleAlbum.jsx` | Header apilado, botones full-width móvil, grid "Más de" responsive |
| `src/pages/DetalleArtista.jsx` | Header apilado, discografía 5→2/3/4 |
| `src/pages/PerfilUsuario.jsx` | Header apilado, grid favoritos 6→2/3/4, reseñas con padding adaptado |
| `src/pages/MisFavoritos.jsx` | Grid 4→2/3, paddings |
| `src/pages/CrearResena.jsx` | Layout dos columnas → apilado, header standalone responsive, botón full-width móvil |
| `src/pages/EditarResena.jsx` | Mismas adaptaciones que `CrearResena` + botones de acción apilan en móvil |
| `src/pages/EditarPerfil.jsx` | Header standalone responsive |
| `src/pages/PanelAdmin.jsx` | Stats 4→2, columnas apiladas, lista de usuarios con `flex-wrap` |
| `src/pages/NotFound.jsx` | "404" reducido en móvil, padding lateral |

`Login.jsx` y `Registro.jsx` no se modificaron por estar ya adaptados.

---

## Cómo probar el responsive

### En el navegador del PC (DevTools — recomendado para iterar)

1. Arrancar el dev server: `cd musicreviews-frontend && npm run dev`
2. Abrir `http://localhost:5173` en Chrome/Firefox
3. Abrir DevTools (`F12`) y activar el modo dispositivo (`Ctrl+Shift+M` en Chrome, icono de móvil en la barra superior)
4. En la barra superior del modo dispositivo, elegir presets como "iPhone SE" (375 px), "iPhone 12 Pro" (390 px), "iPad Mini" (768 px), o personalizar el ancho con el slider

**Qué comprobar** en al menos un móvil pequeño (375 px) y un tablet (768 px):
- Navbar: el menú hamburguesa aparece bajo `lg` (1024 px), abre y cierra el drawer correctamente
- Catálogo: las tarjetas son legibles, no se desbordan, los chips de género hacen wrap
- Detalle Álbum: portada arriba, info abajo en móvil; botones "Escribir reseña" y "Añadir a favoritos" se apilan
- Mis Favoritos: 2 columnas en móvil, 3 en tablet, 4 en escritorio
- Crear/Editar Reseña: card del álbum encima del formulario en móvil
- Cualquier página con paddings: el contenido respira en móvil sin pegarse a los bordes

### En el móvil real (recomendado para validar UX táctil)

El dev server de Vite escucha por defecto solo en `localhost`. Para que el móvil acceda al PC en la misma red Wi-Fi:

1. Conectar el móvil a la **misma red Wi-Fi** que el PC
2. Saber la IP local del PC. En Windows, `Win+R` → `cmd` → `ipconfig` → buscar "IPv4" en la sección de tu adaptador Wi-Fi (suele ser `192.168.x.x`)
3. Arrancar el dev server con flag `--host`:
   ```bash
   cd musicreviews-frontend
   npm run dev -- --host
   ```
   Vite imprimirá dos URLs, la "Network" será la accesible desde el móvil
4. En el navegador del móvil, abrir esa URL (ej: `http://192.168.1.50:5173`)

Si no carga, comprobar:
- Firewall de Windows: permitir Node.js / Vite por Wi-Fi privada
- El móvil y el PC están en la misma red (no en redes 5GHz/2.4GHz separadas)

### Validación final

A 1024 px o más, todas las pantallas deben verse exactamente igual que antes del trabajo de responsive. Si algo cambió en escritorio, es un bug — el patrón mobile-first additive lo previene por construcción pero conviene comprobarlo visualmente en al menos 2–3 páginas representativas (Inicio, Catálogo, Detalle Álbum) tras la adaptación.
