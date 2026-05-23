# Sistema de Interacción Social y Comunidad en MusicReviews

Este documento describe la arquitectura, el diseño de base de datos, la configuración de seguridad y la implementación del frontend para el módulo de **Interacción Social y Comunidad** de MusicReviews. Este módulo permite la interconexión de melómanos a través de un directorio público, un algoritmo básico de recomendaciones ("Sugeridos para ti") y un sistema bidireccional de seguimiento (seguir/dejar de seguir).

---

## 1. Modelo de Datos del Backend

El núcleo de las interacciones sociales reside en la relación "Seguir" (follow) entre usuarios. Esta se representa mediante la entidad `Seguimiento` mapeada con JPA/Hibernate en la base de datos MySQL.

### Entidad `Seguimiento.java`
La relación se modela mediante una tabla intermedia autogestionada que conecta dos registros de la tabla `usuario`:
* **seguidor_id**: El usuario que inicia la acción de seguir (Foreign Key).
* **seguido_id**: El usuario que recibe la acción (Foreign Key).

```java
@Entity
@Table(name = "seguimiento", uniqueConstraints = @UniqueConstraint(columnNames = {"seguidor_id", "seguido_id"}))
@Data
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguidor_id", nullable = false)
    private Usuario seguidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguido_id", nullable = false)
    private Usuario seguido;

    @Column(name = "fecha_seguimiento")
    private LocalDateTime fechaSeguimiento;

    @PrePersist
    public void prePersist() {
        fechaSeguimiento = LocalDateTime.now();
    }
}
```

* **Restricción de unicidad**: Se aplica `@UniqueConstraint(columnNames = {"seguidor_id", "seguido_id"})` para evitar a nivel de base de datos que un usuario siga por duplicado al mismo perfil.
* **Carga Perezosa (LAZY)**: Se configuran las relaciones como `FetchType.LAZY` para evitar consultas innecesarias de usuarios completos (N+1 queries) al listar relaciones.

---

## 2. API REST del Backend (Endpoints)

El backend expone la funcionalidad a través de dos controladores principales: `SeguimientoController` y `UsuarioController`.

### A. Endpoints de Seguimiento (`SeguimientoController.java`)
Gestionan la lógica de relaciones sociales. Requieren autenticación JWT salvo para consultas de listas:

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/usuarios/{id}/seguir` | Autenticado | Sigue al usuario con el ID `{id}`. |
| **DELETE** | `/api/usuarios/{id}/seguir` | Autenticado | Deja de seguir al usuario con el ID `{id}`. |
| **GET** | `/api/usuarios/{id}/sigue` | Autenticado | Comprueba si el usuario logueado sigue al usuario `{id}`. |
| **GET** | `/api/usuarios/{id}/seguidores` | Público | Lista los seguidores del usuario `{id}` en formato `UsuarioResumenDTO`. |
| **GET** | `/api/usuarios/{id}/siguiendo` | Público | Lista a quién sigue el usuario `{id}` en formato `UsuarioResumenDTO`. |
| **GET** | `/api/usuarios/{id}/contadores` | Público | Devuelve el total de seguidores y seguidos del usuario en un mapa JSON. |

* **Reglas de Negocio en `SeguimientoService`**:
  * Un usuario no puede seguirse a sí mismo (`seguidor.getId().equals(seguidoId)`).
  * No se permite seguir a un usuario previamente seguido.

### B. Endpoints de Consulta de Usuarios (`UsuarioController.java`)
Para permitir un directorio público sin exponer datos confidenciales (como correos o contraseñas), se crearon endpoints seguros utilizando el record `UsuarioResumenDTO`:

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/usuarios/publico` | Público | Devuelve todos los usuarios registrados como lista resumida. |
| **GET** | `/api/usuarios/buscar` | Público | Realiza búsquedas de usuarios por coincidencia en el username (`?q=...`). |

---

## 3. Configuración de Seguridad (`SecurityConfig.java`)

Para que el directorio y las interacciones del frontend funcionen sin obligar a loguearse inmediatamente (y permitir la navegación anónima en los perfiles), se configuraron accesos permitidos en Spring Security:

```java
.authorizeHttpRequests(auth -> auth
    // Rutas públicas de comunidad y perfiles
    .requestMatchers(HttpMethod.GET, "/api/usuarios/username/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/usuarios/buscar").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/usuarios/publico").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/usuarios/*/seguidores").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/usuarios/*/siguiendo").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/usuarios/*/contadores").permitAll()
    
    // Solo ADMIN: listar todos los usuarios en bruto (con emails)
    .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
    ...
)
```

---

## 4. Implementación del Frontend (React)

El frontend de la Comunidad se divide en dos componentes principales y sus servicios de API asociados.

### A. Vista de la Comunidad (`Comunidad.jsx`)
Ubicada en `/usuarios`, ofrece un punto de encuentro interactivo.
* **Buscador Reactivo**: Permite filtrar localmente mediante el componente custom `SearchBar` a los miembros registrados sin sobrecargar el servidor de base de datos.
* **Sección "Sugeridos para ti"**: Muestra hasta 4 melómanos recomendados que el usuario activo aún no sigue.
* **Prevención de Saltos de Diseño (Layout Shifts)**:
  * Las sugerencias se cargan y mantienen **estáticas en el estado** al iniciar la página.
  * Al pulsar el botón "Seguir", el elemento no desaparece repentinamente del grid (lo cual causaría un reajuste tosco). En su lugar, el estado del botón transiciona reactivamente a **✓ Siguiendo**, permaneciendo estable hasta que la vista se recargue.

### B. Vista de Perfil (`PerfilUsuario.jsx`)
Ubicada en `/perfil/:username`, es el perfil del melómano.
* **Resumen de Contadores**: Muestra contadores dinámicos para "Reseñas", "Favoritos", "Seguidores" y "Siguiendo".
* **Pestañas Interactivas (Tabs)**:
  * Las pestañas de **Seguidores** y **Siguiendo** renderizan cuadrículas de usuarios que comparten la misma lógica de "Seguimiento Rápido" que el directorio general.
  * Si un usuario deja de seguir a alguien desde su propia pestaña de "Siguiendo", la tarjeta se desvanece de inmediato y los contadores del perfil se recalculan en tiempo real de forma automática.

---

## 5. Diseño Visual Premium y Diseño Adaptable (UI/UX & Responsive)

Se ha mantenido la coherencia estética con la interfaz del reproductor y catálogo de MusicReviews mediante tokens oscuros y cristalizados.

### Estilo Glassmorphism
* **`glass-panel`**: Paneles oscuros translúcidos con desenfoque de fondo (`backdrop-blur-lg`) y bordes de color verde bosque con un `8%` de opacidad.
* **`border-light-glow`**: Efecto personalizado que dibuja un borde luminoso muy fino en la parte superior de las tarjetas para simular el reflejo físico de la luz en el cristal.

### Micro-interacciones
* **Efectos en Avatares**:
  * Las imágenes de perfil se escalan (`hover:scale-105`) y añaden un anillo verde resplandeciente (`ring-primary/20`) cuando el usuario pasa el ratón por encima de la tarjeta.
  * Los avatares de texto por defecto (iniciales del usuario) aplican una rotación animada de 6 grados (`hover:rotate-6`) para aportar dinamismo táctil.
* **Transiciones Premium**: Todos los cambios de tamaño, color y sombras se animan con curvas bezier de alta gama para evitar saltos bruscos.

### Adaptabilidad (Responsividad Móvil)
El diseño cambia de forma fluida a través de Tailwind:
* **Grids Flexibles**: Las sugerencias y el directorio fluyen de 1 columna en smartphones, a 2 o 3 columnas en tablets, hasta 4 columnas en monitores de alta resolución.
* **Alineación Adaptable**: En móviles, la cabecera del perfil se organiza en formato de pila vertical (`flex-col items-center`) centrando avatares y bios; mientras que en pantallas más grandes se estira horizontalmente (`flex-row text-left`) aprovechando el espacio.
