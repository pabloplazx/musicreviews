# Tailwind CSS — Guía del proyecto MusicReviews

Documento de referencia de todas las clases Tailwind usadas en el proyecto, organizadas por componente.

---

## Componentes UI reutilizables

### `SectionTitle`
Título de sección con línea verde debajo.
| Clase | Qué hace |
|---|---|
| `mb-6` | Margen abajo del bloque completo |
| `text-2xl` | Tamaño del título (24px) |
| `font-heading` | Fuente Space Grotesk |
| `font-bold` | Negrita |
| `text-text` | Color blanco/crema (#ebf0ed) |
| `w-8` | Ancho de la línea verde (32px) |
| `h-1` | Alto de la línea verde (4px) |
| `bg-primary` | Color verde (#48a377) |
| `mt-2` | Separación entre título y línea |

---

### `PortadaPlaceholder`
Placeholder oscuro con ♪ para sustituir portadas de álbum.
| Clase | Qué hace |
|---|---|
| `bg-input` | Fondo oscuro (#0a0f0c) |
| `rounded-xl` | Bordes redondeados |
| `flex items-center justify-center` | Centra el ♪ dentro del div |
| `text-primary` | Color verde del ♪ |
| `font-heading font-bold` | Space Grotesk negrita |
| `text-4xl` | Tamaño del ♪ |

> El tamaño (ancho y alto) se pasa desde fuera con `className`.

---

### `Estrellas`
Valoración del 1 al 5 con soporte de medias estrellas. Cada posición es un componente `Estrella` interno que recibe `fill` (1, 0.5 o 0).
| Clase | Qué hace |
|---|---|
| `text-primary` | Color verde de las estrellas llenas |
| `text-muted` | Color gris de las estrellas vacías |
| `text-sm` | Tamaño base (14px) |
| `flex` | Alinea las 5 estrellas en fila |
| `relative inline-block` | Contenedor de la media estrella |
| `absolute inset-0 overflow-hidden w-1/2` | Recorta la estrella verde al 50% para la media estrella |

---

### `ResenaCard`
Tarjeta vertical de reseña: portada arriba, información debajo.
| Clase | Qué hace |
|---|---|
| `bg-card` | Fondo de la tarjeta (#0e1310) |
| `border border-border` | Borde sutil |
| `rounded-xl` | Esquinas redondeadas |
| `overflow-hidden` | La imagen respeta el rounded de la tarjeta |
| `flex flex-col` | Apila portada e info en columna |
| `aspect-square` | Portada cuadrada |
| `object-cover` | La imagen cubre sin deformarse |
| `p-4` | Padding interior del bloque de info |
| `gap-1` | Separación pequeña entre líneas de info |
| `italic` | Cursiva para el texto de la reseña |

---

### `AlbumCard`
Tarjeta de álbum con badge de posición para rankings.
| Clase | Qué hace |
|---|---|
| `bg-card` | Fondo de la tarjeta (#0e1310) |
| `border border-border` | Borde sutil |
| `rounded-xl overflow-hidden` | Esquinas redondeadas + imagen recortada |
| `flex flex-col` | Apila portada e info en columna |
| `relative` | Permite posicionar el badge encima de la portada |
| `absolute top-2 left-2` | Badge en la esquina superior izquierda |
| `w-6 h-6 rounded-full` | Badge circular de 24px |
| `bg-primary text-background` | Badge verde con número oscuro |
| `aspect-square object-cover` | Portada cuadrada sin deformarse |
| `p-3` | Padding interior del bloque de info |

---

## Páginas

### `Inicio.jsx` — Hero
| Clase | Qué hace |
|---|---|
| `py-20 bg-card` | Sección hero: padding 80px, fondo card |
| `max-w-300 mx-auto px-12` | Contenedor centrado, ancho máx. 1200px |
| `flex items-center justify-between gap-12` | Dos columnas separadas y centradas |
| `text-6xl leading-tight` | Título grande con líneas compactas |
| `mb-4` / `mb-8` | Separación título→subtítulo y subtítulo→botón |
| `inline-flex items-center gap-2` | Botón con texto y flecha alineados |
| `rounded-input hover:bg-secondary transition-colors` | Botón con hover suave |
| `bg-input border border-primary/40` | Tarjeta DAMN.: fondo más oscuro, borde verde 40% |
| `w-55 shrink-0` | Tarjeta fija de 220px que no se encoge |
| `w-full h-47.5 mb-3` | Portada placeholder de 190px con margen |
| `text-xs italic` | Texto pequeño y en cursiva para la cita |

---

### `Inicio.jsx` — Reseñas recientes
| Clase | Qué hace |
|---|---|
| `py-12 bg-background` | Sección: padding 48px, fondo oscuro (#060907) |
| `grid grid-cols-4 gap-6` | Grid de 4 columnas con espacio de 24px |

---

### `Inicio.jsx` — Top Álbumes
| Clase | Qué hace |
|---|---|
| `py-12 bg-card` | Sección: padding 48px, fondo card (#0e1310) |
| `grid grid-cols-5 gap-6` | Grid de 5 columnas con espacio de 24px |

---

### `Inicio.jsx` — CTA
| Clase | Qué hace |
|---|---|
| `py-20 bg-background` | Sección: padding 80px, fondo principal (#060907) |
| `text-5xl leading-tight mb-8` | Título grande compacto con margen bajo |
| `flex items-center gap-6` | Botón y link en fila con espacio de 24px |
| `hover:underline` | Subrayado al hover en el link secundario |

---

## Footer

| Clase | Qué hace |
|---|---|
| `bg-card` | Fondo oscuro (#0e1310), igual que el Navbar |
| `border-t border-border` | Línea superior sutil |
| `py-6` | Padding vertical 24px |
| `flex items-center justify-between` | Copyright a la izquierda, links a la derecha |
| `gap-6` | Separación entre los links de navegación |
| `hover:text-text transition-colors` | Los links del footer se iluminan al hover |

---

## Navbar

| Clase | Qué hace |
|---|---|
| `sticky top-0 z-50` | Fija la navbar arriba al hacer scroll |
| `bg-card` | Fondo oscuro (#0e1310) |
| `border-b border-border` | Línea inferior sutil |
| `h-16` | Altura de 64px |
| `flex items-center justify-between` | Logo, links y botones en fila separados |
| `w-5 h-5` | Tamaño del icono SVG de lupa (20px) |

---

### `FormInput`
Campo de formulario reutilizable con label, input y soporte de error.
| Clase | Qué hace |
|---|---|
| `h-12` | Altura del input (48px) |
| `placeholder:text-muted` | Color gris del texto placeholder |
| `outline-none` | Elimina el outline por defecto del navegador al hacer focus |
| `focus:border-primary` | Borde verde al hacer focus (estado normal) |
| `focus:border-error` | Borde rojo al hacer focus (estado error) |
| `border-error` | Borde rojo (#cc3333) en estado error |

---

### `GenreChip`
Chip de género/filtro con estado activo e inactivo.
| Clase | Qué hace |
|---|---|
| `rounded-full` | Bordes completamente redondeados (píldora) |
| `hover:border-primary` | Borde verde al pasar el ratón (estado inactivo) |

---

### `CatalogoCard`
Tarjeta de álbum con badge de género opcional en la esquina superior derecha.
| Clase | Qué hace |
|---|---|
| `top-2 right-2` | Badge en la esquina superior derecha |
| `px-2 py-0.5` | Padding compacto del badge (horizontal 8px, vertical 2px) |
| `truncate` | Corta el texto con `…` si es demasiado largo |

---

### `SearchBar`
Barra de búsqueda con icono SVG de lupa integrado.
| Clase | Qué hace |
|---|---|
| `w-4 h-4` | Tamaño del icono SVG de lupa (16px) |
| `focus-within:border-primary` | Borde verde cuando cualquier hijo tiene focus (no solo el input) |
| `flex-1` | El input crece para ocupar todo el espacio disponible |
| `bg-transparent` | Fondo transparente para que el input herede el fondo del contenedor |

---

### `SelectOrden`
Selector desplegable estilizado con el design system.
| Clase | Qué hace |
|---|---|
| `h-10` | Altura del select (40px) |
| `cursor-pointer` | Muestra el cursor de mano al pasar por encima |

---

### `Paginacion`
Paginación con botones de página, flechas y puntos suspensivos.
| Clase | Qué hace |
|---|---|
| `w-9 h-9` | Tamaño de cada botón de página (36px × 36px) |
| `rounded-lg` | Bordes redondeados medianos (8px) |
| `disabled:opacity-40` | Reduce la opacidad del botón cuando está desactivado |
| `disabled:cursor-not-allowed` | Muestra cursor de prohibido cuando el botón está desactivado |

---

### `AlbumRow`
Fila compacta horizontal con portada pequeña y texto "Álbum — Artista".
| Clase | Qué hace |
|---|---|
| `gap-4` | Separación de 16px entre portada y texto |
| `w-10 h-10` | Tamaño de la portada pequeña (40px × 40px) |

---

## Páginas

### `Login.jsx` / `Registro.jsx`
| Clase | Qué hace |
|---|---|
| `min-h-[calc(100vh-64px)]` | Altura mínima = viewport menos la navbar (64px), para centrar la card verticalmente |
| `max-w-md` | Ancho máximo de la card (~448px) |
| `rounded-2xl` | Bordes muy redondeados de la card (16px) |
| `p-7` | Padding interior de la card (28px) |
| `gap-5` | Separación entre campos del formulario (20px) |
| `mb-1` | Separación mínima entre logo y subtítulo |
| `bg-error/10` | Fondo del banner de error: rojo con 10% de opacidad |
| `text-error` | Texto rojo (#cc3333) para mensajes de error |
| `self-start` | Alinea el link "¿Olvidaste tu contraseña?" al inicio, sin que ocupe todo el ancho |

---

### `Catalogo.jsx`
| Clase | Qué hace |
|---|---|
| `flex-wrap` | Los chips de género se envuelven en varias líneas si no caben |
| `mb-5` | Separación de 20px debajo de la barra de búsqueda |
| `mb-10` | Separación de 40px debajo del bloque de filtros y entre secciones |

---

### `Busqueda.jsx` — estado sin resultados
| Clase | Qué hace |
|---|---|
| `py-24` | Padding vertical 96px para centrar visualmente el mensaje vacío |

---

### `Rankings.jsx`
| Clase | Qué hace |
|---|---|
| `py-10` | Padding vertical 40px para el contenedor principal |
| `mb-8` | Margen inferior 32px bajo el título de página |
| `grid-cols-4` | Grid de 4 columnas (stat cards) |
| `gap-4` | Separación de 16px entre stat cards |
| `py-5` | Padding vertical 20px en las stat cards |
| `text-3xl` | Tamaño del número stat (30px) |
| `grid-cols-2` | Grid de 2 columnas para secciones paralelas |
| `gap-10` | Separación de 40px entre columnas |
| `gap-5` | Separación de 20px entre géneros |
| `mb-2` | Margen inferior 8px entre nombre de género y barra |
| `h-2` | Alto de la barra de progreso (8px) |
| `bg-border` | Fondo de la barra de progreso (gris oscuro #223228) |
| `h-full` | La barra interior ocupa todo el alto del contenedor |
| `bg-primary/20` | Badge de posición: verde con 20% de opacidad |
| `min-w-0` | Evita que el texto desborde en contenedores flex (necesario con `truncate`) |
| `font-medium` | Peso de fuente medio (500) para nombres en listas |
| `w-8 h-8` | Portada miniatura de 32px en filas de ranking |
| `hover:opacity-80` | Reducción de opacidad al hover en links de artista |
| `transition-opacity` | Transición suave de opacidad |

---

### `DetalleAlbum.jsx`
| Clase | Qué hace |
|---|---|
| `border-b` | Línea inferior que separa el header del contenido |
| `w-48 h-48` | Portada del álbum en el header (192px × 192px) |
| `gap-10` | Separación entre portada e información del álbum |
| `gap-3` | Separación entre elementos del bloque de info |
| `px-3 py-1` | Padding del chip de género (horizontal 12px, vertical 4px) |
| `bg-primary/20` | Fondo del chip de género: verde con 20% de opacidad |
| `px-5 py-2.5` | Padding de los botones de acción del álbum |
| `py-2.5` | Padding vertical 10px (entre py-2 y py-3) |
| `bg-primary/10` | Fondo del botón "En favoritos" activo: verde con 10% de opacidad |
| `max-w-lg` | Ancho máximo de la descripción del álbum (~512px) |
| `mb-12` | Margen inferior 48px entre secciones |
| `px-6 py-5` | Padding de las tarjetas de reseña |
| `mt-1` | Margen superior 4px para el texto de la reseña |
| `text-xl` | Tamaño de texto 20px (título estado vacío) |
| `text-center` | Texto centrado horizontalmente |
| `group` | Marca el contenedor para activar estilos `group-*` en hijos |
| `group-hover:opacity-80` | Reduce opacidad de la portada al hacer hover en el `Link` padre |
| `w-9 h-9` | Avatar circular del usuario en reseñas (36px × 36px) |
| `uppercase` | Inicial del usuario en el avatar en mayúscula |

---

### `DetalleArtista.jsx`
| Clase | Qué hace |
|---|---|
| `items-start` | Alinea los hijos al inicio (arriba) en un flex row |
| `w-36 h-36` | Tamaño del avatar circular del artista (144px × 144px) |
| `border-2` | Borde de 2px (más grueso que `border`) para el avatar |
| `pt-2` | Padding superior 8px en el bloque de info |
| `py-1.5` | Padding vertical 6px en el botón "Seguir artista" |
| `px-8 py-4` | Padding interno de cada celda de stats del artista |
| `border-r` | Línea divisoria derecha entre celdas de stats |
| `leading-relaxed` | Interlineado relajado (1.625) para la biografía |
| `max-w-4xl` | Ancho máximo de la biografía (~896px) |
| `mb-3` | Margen inferior 12px bajo el título "Biografía" |
| `mb-6` | Margen inferior 24px bajo el título "Discografía" |

---

### `CrearResena.jsx`
| Clase | Qué hace |
|---|---|
| `text-lg` | Tamaño de texto 18px (logo y título del header) |
| `absolute left-1/2 -translate-x-1/2` | Centra el título horizontalmente de forma absoluta respecto al header |
| `gap-16` | Separación de 64px entre la card del álbum y el formulario |
| `w-72` | Ancho de la card del álbum (288px) |
| `text-base` | Tamaño de texto 16px (títulos de campo) |
| `gap-6` | Separación de 24px entre bloques del formulario |
| `resize-none` | Desactiva el redimensionado manual del textarea |
| `bottom-3 right-4` | Posición del contador de caracteres dentro del textarea |
| `px-10 py-3` | Padding del botón "Publicar reseña" |
| `rows={10}` | Altura inicial del textarea (10 líneas) — prop JSX, no clase Tailwind |
| `cursor-pointer` | Cursor de mano en las estrellas interactivas |
| `select-none` | Evita que el texto/símbolo ★ se seleccione al hacer click rápido |
| `text-6xl` | Tamaño del ♪ en el `PortadaPlaceholder` de la card de álbum (60px) |

---

### `EditarResena.jsx`
| Clase | Qué hace |
|---|---|
| `h-px` | Altura de 1px — divider horizontal entre el título y las filas de detalles |
| `px-5 py-4` | Padding del textarea (horizontal 20px, vertical 16px) |

---

### `PerfilUsuario.jsx`
| Clase | Qué hace |
|---|---|
| `w-24 h-24` | Tamaño del avatar circular del usuario (96px × 96px) |
| `text-4xl` | Tamaño de la inicial del avatar |
| `pt-1` | Padding superior 4px en el bloque de info del perfil |
| `text-2xl` | Tamaño de los números de stats (Reseñas, Favoritos) |
| `bottom-0` | Posiciona el indicador de tab en el borde inferior del botón |
| `h-0.5` | Alto del indicador de tab activo (2px) |
| `grid-cols-6` | Grid de 6 columnas para la cuadrícula de favoritos |
| `w-7 h-7` | Tamaño del badge de corazón sobre la portada (28px × 28px) |

---

### `EditarPerfil.jsx`
| Clase | Qué hace |
|---|---|
| `max-w-xl` | Ancho máximo del formulario (~576px), más estrecho que el contenedor general |

---

### `MisFavoritos.jsx`
| Clase | Qué hace |
|---|---|
| `py-32` | Padding vertical 128px en el estado vacío — centra el contenido visualmente |
| `text-border` | Usa el color del borde (`#223228`) para el icono ♡ en el estado vacío (efecto sutil) |

---

### `PanelAdmin.jsx`
| Clase | Qué hace |
|---|---|
| `mb-10` | Margen inferior 40px bajo la cuadrícula de stat cards |
| `py-6` | Padding vertical 24px en las stat cards |
| `gap-8` | Separación de 32px entre las dos subsecciones de la columna derecha |
| `text-left` | Alinea el texto a la izquierda en botones (por defecto los botones centran) |
| `px-3 py-1` | Padding del badge Activo/Inactivo (horizontal 12px, vertical 4px) |
| `bg-error/20` | Fondo del badge "Inactivo": rojo con 20% de opacidad |
| `text-xs` | Tamaño de texto 12px — usado en badges, subtítulos y etiquetas pequeñas |
