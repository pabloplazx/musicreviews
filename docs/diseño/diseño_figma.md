# Diseño Figma — MusicReviews

Archivo Figma: **Prototipo** (`qxAT7DYKddqiLyuzrznqd4`)
Enlace: https://www.figma.com/design/qxAT7DYKddqiLyuzrznqd4/Prototipo

---

## Design System

### Paleta de colores

| Token        | Hex       | Uso                                      |
|--------------|-----------|------------------------------------------|
| background   | `#060907` | Fondo de todas las pantallas             |
| card         | `#0e1310` | Fondo de tarjetas, navbar y modales      |
| text         | `#ebf0ed` | Texto principal                          |
| text dim     | `#ebf0ed` al 50% | Texto secundario, placeholders, subtítulos |
| primary      | `#48a377` | Botones principales, links de acción, logo |
| secondary    | `#226846` | Elementos secundarios                    |
| accent       | `#239a60` | Acentos puntuales                        |
| border       | `#223228` | Bordes de cards e inputs                 |
| input bg     | `#0a0f0c` | Fondo de campos de formulario            |
| error        | `#cc3333` | Bordes de error, badges de error, toasts |

### Tipografía

| Fuente             | Variante | Uso                                         |
|--------------------|----------|---------------------------------------------|
| Space Grotesk      | Bold     | Logo, títulos de sección, títulos de card   |
| Space Grotesk      | Medium   | Texto de botones, labels destacados         |
| Outfit             | Medium   | Labels de formularios                       |
| Outfit             | Regular  | Cuerpo de texto, subtítulos, placeholders   |

### Medidas base

> **Nota:** MusicReviews es una **aplicación web**. Todos los frames Figma son de 1440px.

#### Implementación web (React)

- **Ancho de ventana objetivo:** 1440 px
- **Contenedor de contenido:** máx. 1200 px, centrado con `mx-auto`
- **Padding lateral de contenedor:** 48 px
- **Navbar:** horizontal, sticky, ancho completo — logo a la izquierda, nav y acciones a la derecha
- **Layouts:** columnas múltiples (grid/flex), no diseño de columna única
- **Padding interno de card:** 28 px
- **Border radius card:** 16 px
- **Border radius inputs/botones:** 10 px
- **Altura de inputs:** 48 px
- **Altura de botón primario:** 50 px

---

## Identidad de marca en el navegador

Aspecto de la aplicación cuando aparece como pestaña en el navegador. Antes del trabajo de identidad, la app heredaba el placeholder de Vite (`musicreviews-frontend` como título y un favicon morado de Bolt.new que no formaba parte del sistema de diseño).

### Título de la pestaña

**Valor:** `MusicReviews — Reseñas musicales`
**Fichero:** `musicreviews-frontend/index.html` (`<title>`)

Decisiones:

- Se escribe con guion em (`—`) y no con guion corto, alineado con la convención tipográfica que ya usa el resto de la UI (subtítulos del Hero, separadores en cards de reseña).
- El descriptor "Reseñas musicales" actúa como subtítulo SEO: lo lee Google en la pestaña del SERP y los lectores de pantalla al cambiar de página, mejorando accesibilidad.
- Se mantiene en español por coherencia con el resto del producto (la UI no tiene i18n).

### Favicon

**Fichero:** `musicreviews-frontend/public/favicon.svg`
**Formato:** SVG vectorial (32×32 viewBox, escala perfectamente a 16×16 en pestañas y a 192×192 en marcadores móviles).

Composición:

| Elemento | Valor | Justificación |
|---|---|---|
| Fondo | Cuadrado redondeado, `#0e1310` (token `card`) | Mismo tono que el navbar y las tarjetas — el favicon "es" una mini-card |
| Border-radius | 6 px sobre 32 px (≈ 19%) | Idéntica proporción al `--radius-card` (16/120 ≈ 13%) ajustada a la escala pequeña |
| Símbolo | Corchea (♪) en `#48a377` (token `primary`) | Coincide con el icono que aparece junto al wordmark en el `Navbar` y con los estados vacíos de Detalle Álbum y Búsqueda |

Razones para descartar alternativas:

- **Vinilo concéntrico**: queda ilegible a 16×16 en pestañas — los círculos colapsan.
- **Auriculares**: no aparece en ningún otro punto de la UI; introduciría un símbolo nuevo solo para el favicon.
- **Solo nota sin fondo (transparente)**: el verde primary tiene poco contraste sobre el fondo blanco/gris claro de los temas claros del navegador. El fondo card oscuro garantiza visibilidad en cualquier tema.

El SVG no usa filtros, gradientes ni texto: solo tres formas geométricas (`<rect>` para el cuerpo de la nota, `<path>` para la bandera y `<ellipse>` rotada para la cabeza). Esto permite que renderice correctamente sin fuentes externas y mantiene el tamaño del archivo en ~340 bytes.

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="32" height="32">
  <rect width="32" height="32" rx="6" fill="#0e1310"/>
  <g fill="#48a377">
    <rect x="15.5" y="7" width="2.5" height="16"/>
    <path d="M18 7 Q26 9 26 16 Q24 11 18 12 Z"/>
    <ellipse cx="12.5" cy="22.5" rx="4.5" ry="3.2" transform="rotate(-18 12.5 22.5)"/>
  </g>
</svg>
```

---

## Flujos de prototipo

### Conexiones activas (configuradas en Figma)

| Origen | Trigger | Destino |
|--------|---------|---------|
| Login Screen — btn "Iniciar sesión" | Click | Login - Error (estado de error) |

### Conexiones manuales pendientes (cross-page)

Conectar en Figma → Prototype panel seleccionando el nodo y arrastrando al destino:

| Origen (página / nodo) | Destino (página / frame) |
|------------------------|--------------------------|
| Login — btn "Iniciar sesión" (éxito) | Catálogo → Catalogo Screen |
| Login — link "Regístrate" | Registro → Registro Screen |
| Login — nav btn "Registrarse" | Registro → Registro Screen |
| Registro — btn "Crear cuenta" | Catálogo → Catalogo Screen |
| Registro — link "Inicia sesión" | Login → Login Screen |
| Inicio — btn "Explorar catálogo" | Catálogo → Catalogo Screen |
| Inicio — btn "Crear cuenta" | Registro → Registro Screen |
| Catálogo — tarjetas de álbum | Detalle de Álbum → AlbumDetail |
| Detalle de Álbum — btn "Escribir reseña" | Crear Reseña → Crear Reseña Screen |
| Mis Favoritos Vacío — btn "Explorar catálogo" | Catálogo → Catalogo Screen |
| 404 — btn "Ir al inicio" | Inicio → Inicio Screen |

---

## Pantallas diseñadas

### 1. Inicio (`Inicio`)
Captura: `screens/inicio.png`

Página de entrada diferenciada del catálogo. Elementos:
- Navbar: logo, links de navegación, botones Entrar / Registrarse
- Hero con tagline "Tu música. Tus reseñas." y dos CTAs: Explorar catálogo / Crear cuenta
- Sección "Álbumes más valorados": grid de 5 tarjetas con portada, título, artista y puntuación ★
- Sección "Últimas reseñas": 3 tarjetas con avatar, usuario, álbum, estrellas y extracto

---

### 2. Login (`Login`)
Capturas: `screens/login.png` · `screens/login_error.png`

Dos estados en la misma página:

**Login Screen** — estado por defecto:
- Card centrada: logo MusicReviews + tagline, campo email, campo contraseña, link "¿Olvidaste tu contraseña?", botón "Iniciar sesión", link "¿No tienes cuenta? Regístrate"

**Login - Error** — credenciales incorrectas:
- Mismo layout con banner de error rojo ("Credenciales incorrectas. Inténtalo de nuevo.")
- Bordes de ambos inputs en rojo (`#cc3333`)

---

### 3. Registro (`Registro`)
Captura: `screens/registro.png`

- Card centrada: logo + tagline, título "Crear cuenta"
- Campos: Nombre de usuario, Correo electrónico, Contraseña, Confirmar contraseña
- Botón "Crear cuenta", link "¿Ya tienes cuenta? Inicia sesión"

---

### 4. Catálogo (`Catalogo`)
Captura: `screens/catalogo.png`

- Título "Catálogo" con contador de álbumes
- Barra de búsqueda integrada
- Chips de filtro por género: Todos (activo en primary), Hip-Hop, Rock, Electronic, R&B, Pop, Jazz, Clásica
- Selector "Ordenar por" (Mejor valorados, Más reseñados, Recientes)
- Grid de tarjetas de álbum con badge de género, portada, título, artista y puntuación ★
- Paginación

---

### 5. Detalle de Álbum (`Detalle de Álbum`)
Capturas: `screens/detalle_album.png` · `screens/detalle_album_sin_resenas.png`

Dos estados:

**Estado normal:**
- Header: portada grande, badge de género, título, artista · año, puntuación media con estrellas y número de reseñas
- Botones: Escribir reseña (borde primary) / ♡ Añadir a favoritos (borde sutil)
- Descripción del álbum
- Lista de reseñas de usuarios (avatar, nombre, fecha, estrellas, texto)
- Sección "Más de [Artista]": carrusel de álbumes relacionados

**Sin reseñas (álbum nuevo):**
- Mismo header con estrellas en gris al 35% y texto "Sin valoraciones aún"
- Estado vacío centrado: icono ♪ (Space Grotesk, opacidad 25%), título "Sé el primero en reseñar", subtítulo, CTA "Escribir reseña"
- Nota: icono basado en tipografía del sistema de diseño — sin emoji del SO

---

### 6. Perfil de Usuario (`Perfil de Usuario`)
Captura: `screens/perfil_usuario.png`

- Avatar y datos del usuario
- Estadísticas: número de reseñas, álbumes escuchados
- Grid de últimas reseñas escritas
- Sección de álbumes favoritos

---

### 7. Búsqueda (`Busqueda`)
Capturas: `screens/busqueda.png` · `screens/busqueda_sin_resultados.png`

Dos estados:

**Con resultados:**
- Barra de búsqueda grande
- Chips: Todo, Álbumes, Artistas, Usuarios
- Sección "Tendencias": grid de tarjetas de álbum
- Sección "Añadidos recientemente": lista compacta

**Sin resultados:**
- Misma barra con query visible
- Estado vacío con ♪ tenue, mensaje descriptivo

---

### 8. Crear Reseña (`Crear Reseña`)
Captura: `screens/crear_resena.png`

- Card de álbum: portada, título, artista, año, género
- Puntuación: 5 estrellas interactivas con medias estrellas (estilo Letterboxd, 0.5 en 0.5)
- Campo título de reseña
- Área de texto (contador 0 / 2000)
- Botón "Publicar reseña" + botón "Guardar borrador"

---

### 9. Detalle de Artista (`Detalle de Artista`)
Captura: `screens/detalle_artista.png`

- Hero: foto circular del artista, nombre, país · género
- Estadísticas en card: Media ★, Álbumes, Reseñas
- Biografía
- Discografía: grid de tarjetas de álbum

---

### 10. Estadísticas / Rankings (`Estadisticas`)
Captura: `screens/estadisticas.png`

- Resumen global: Álbumes, Artistas, Reseñas, Usuarios
- Top Álbumes: lista de 5 con posición (nº1 en primary), portada, título, artista y ★
- Por género: barras de progreso
- Top Artistas: lista de 3 con avatar, nombre, género y ★
- Actividad reciente: últimas reseñas

---

### 11. Editar Perfil (`Editar Perfil`)
Captura: `screens/editar_perfil.png`

- Avatar circular editable con link "Cambiar foto"
- Campos: Nombre de usuario, Correo electrónico, Biografía (textarea)
- Sección Seguridad: botón "Cambiar contraseña"
- Botón "Guardar cambios"
- Zona de peligro: botón "Desactivar cuenta" en rojo

---

### 12. Mis Favoritos (`Mis Favoritos`)
Capturas: `screens/mis_favoritos.png` · `screens/mis_favoritos_vacio.png`

Dos estados:

**Con favoritos:**
- Grid 4 columnas × 2 filas (8 álbumes), tarjetas 310×280px con portada, título, artista y puntuación ★
- Badge verde redondeado en esquina superior derecha de cada tarjeta como indicador de favorito (sin emoji — color primary `#48a377`)
- Navbar con avatar "P" (usuario autenticado)

**Vacío (usuario nuevo):**
- Estado vacío con ♡ tenue, texto "Aún no tienes favoritos", CTA "Explorar catálogo"

---

### 13. Editar Reseña (`Editar Reseña`)
Captura: `screens/editar_resena.png`

Igual que Crear Reseña con:
- Badge "Editando" en verde
- Campos pre-rellados con valores actuales
- Bordes de inputs en primary (estado activo)
- Contador con texto actual ("142 / 2000")
- Botón "Eliminar reseña" en rojo
- Pie: "Última edición: [fecha]"

---

### 14. Panel Admin (`Panel Admin`)
Captura: `screens/panel_admin.png`

Solo visible para `Rol.ADMIN`:
- Navbar con badge "ADMIN" en amarillo
- Resumen: 4 tarjetas — Álbumes, Artistas, Usuarios, Inactivos (en rojo)
- Gestión de contenido: Añadir álbum (import Spotify), Añadir artista, Editar álbum, Editar artista
- Gestión de usuarios: lista con nombre, email y chip Activo/Inactivo
- Moderación: acceso a reseñas reportadas

---

### 15. 404 (`404`)
Captura: `screens/404.png`

- "404" en grande con efecto de opacidad
- Mensaje "Página no encontrada"
- Subtítulo explicativo
- Botón "Ir al inicio"

---

## Componentes UI (`Componentes UI`)
Captura: `screens/componentes_ui.png`

Página de documentación de componentes reutilizables:

### Toasts / Notificaciones

| Tipo | Color borde | Uso |
|------|-------------|-----|
| Éxito — "Añadido a favoritos" | primary `#48a377` | Al añadir álbum a favoritos |
| Éxito — "Reseña publicada" | primary `#48a377` | Al publicar o editar reseña |
| Error — "Error al guardar" | error `#cc3333` | Fallo de red o validación |

Todos los toasts: barra vertical de 4px en el color del tipo, icono (✓/✕), título en Space Grotesk Medium, subtítulo en Outfit Regular.

### Modal de confirmación

Usado al eliminar una reseña:
- Overlay semitransparente oscuro
- Card centrada: título "Eliminar reseña", mensaje de advertencia
- Dos botones: "Cancelar" (borde sutil) y "Sí, eliminar" (fondo rojo `#cc3333`)
