# Refactorización y optimización del backend

## Por qué se refactorizó

Tras completar la implementación inicial del backend y el sistema de autenticación, se realizó una revisión completa del código en busca de mejoras de rendimiento, seguridad y calidad. El objetivo no era reescribir lo que funcionaba, sino aplicar las buenas prácticas estándar de Spring Boot que hacen el código más eficiente, más fácil de leer y más fácil de mantener.

---

## Cambios realizados

### 1. Inyección por constructor con `@RequiredArgsConstructor`

**Afecta a:** todos los servicios y controladores

**Antes:**
```java
@Service
public class ArtistaService {
    @Autowired
    private ArtistaRepository artistaRepository;
}
```

**Después:**
```java
@Service
@RequiredArgsConstructor
public class ArtistaService {
    private final ArtistaRepository artistaRepository;
}
```

**Por qué:** El uso de `@Autowired` directamente sobre un campo es el patrón antiguo de Spring. Sus problemas:
- El campo no puede ser `final`, lo que significa que podría reasignarse accidentalmente.
- Oculta las dependencias de la clase — no es evidente qué necesita para funcionar sin leer todo el código.
- Dificulta los tests unitarios porque no hay constructor al que pasarle mocks.

Con `@RequiredArgsConstructor` de Lombok, Spring inyecta las dependencias por constructor automáticamente. Los campos son `final` (inmutables), las dependencias son visibles y los tests son más sencillos.

---

### 2. `@Transactional(readOnly = true)` en métodos de lectura

**Afecta a:** todos los métodos `obtener*` y `buscar*` de todos los servicios

**Antes:**
```java
public List<Artista> obtenerTodos() {
    return artistaRepository.findAll();
}
```

**Después:**
```java
@Transactional(readOnly = true)
public List<Artista> obtenerTodos() {
    return artistaRepository.findAll();
}
```

**Por qué:** Al marcar una transacción como `readOnly = true`, Hibernate sabe que no va a haber escrituras y puede optimizar:
- No rastrea el estado de los objetos cargados (dirty checking), ahorrando memoria y CPU.
- Permite que el driver de BD use conexiones de solo lectura si las hay disponibles.
- Hace explícito en el código que ese método nunca escribe en la BD, lo cual es documentación en sí misma.

---

### 3. `FetchType.LAZY` en relaciones `@ManyToOne`

**Afecta a:** `Album.artista`, `Resena.usuario`, `Resena.album`, `Favorito.usuario`, `Favorito.album`

**Antes:**
```java
@ManyToOne
@JoinColumn(name = "artista_id")
private Artista artista;
```

**Después:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "artista_id", nullable = false)
private Artista artista;
```

**Por qué:** Por defecto, JPA carga las relaciones `@ManyToOne` en modo EAGER: cada vez que cargas un `Album`, automáticamente lanza una query adicional para cargar el `Artista` completo aunque no lo necesites. Con 469 álbumes y 99 artistas, esto suponía queries innecesarias en muchas operaciones.

Con `LAZY`, el objeto relacionado solo se carga desde la BD cuando realmente se accede a él. Esto reduce significativamente el número de queries en operaciones que solo necesitan los datos del objeto principal.

---

### 4. Eliminación de queries redundantes en `ResenaService`

**Afecta a:** `ResenaService.crear` y `ResenaService.actualizar`

**Antes:**
```java
public Resena crear(Resena resena) {
    // ...
    Resena guardada = resenaRepository.save(resena);
    return resenaRepository.findById(guardada.getId()).orElse(guardada); // query innecesaria
}
```

**Después:**
```java
public Resena crear(Resena resena) {
    // ...
    return resenaRepository.save(resena); // save() ya devuelve la entidad guardada
}
```

**Por qué:** El método `save()` de Spring Data JPA devuelve la propia entidad guardada con el `id` asignado por la BD. El `findById()` posterior era una segunda query a la BD completamente redundante. Se eliminó en `crear` y en `actualizar`.

---

### 5. Eliminación del endpoint `POST /api/usuarios`

**Afecta a:** `UsuarioController`

**Por qué:** Este endpoint creaba usuarios guardando la contraseña en texto plano, sin pasar por BCrypt. Ahora que existe `POST /api/auth/register`, que sí hashea la contraseña correctamente, mantener este endpoint era un agujero de seguridad. Se eliminó completamente.

El registro de usuarios debe hacerse siempre a través de `/api/auth/register`.

---

### 6. `AuthController` ahora usa el servicio en lugar del repositorio

**Afecta a:** `AuthController`

**Antes:**
```java
public class AuthController {
    @Autowired
    private UsuarioRepository usuarioRepository; // acceso directo al repositorio
}
```

**Después:**
```java
public class AuthController {
    private final UsuarioService usuarioService; // acceso a través del servicio
}
```

**Por qué:** Un controlador nunca debe acceder directamente a un repositorio. La arquitectura en capas del proyecto es `Controller → Service → Repository`. Saltarse la capa de servicio rompe esa arquitectura, duplica lógica de negocio y dificulta el mantenimiento. Se corrigió para que `AuthController` use `UsuarioService`.

---

### 7. `spring.jpa.open-in-view=false`

**Afecta a:** `application.properties`

**Por qué:** Por defecto, Spring Boot mantiene la sesión de Hibernate abierta durante todo el ciclo de vida de la petición HTTP, incluyendo el renderizado de la respuesta. Esto permite cargar relaciones LAZY en cualquier momento, pero tiene un coste: la conexión a la BD se mantiene ocupada más tiempo del necesario. Con este proyecto usando carga LAZY explícita y controlada, no necesitamos este comportamiento. Desactivarlo libera conexiones antes y mejora el rendimiento general.

---

### 8. `spring.jpa.show-sql=false`

**Afecta a:** `application.properties`

**Por qué:** Imprimir cada query SQL en la consola tiene un coste de rendimiento real (serialización de strings, I/O) y llena los logs de información que solo es útil durante el desarrollo. Se desactiva para el entorno de producción.

---

## Resumen de mejoras (primera ronda)

| Cambio | Tipo | Impacto |
|---|---|---|
| Constructor injection | Calidad | Código más limpio, testeable e inmutable |
| `@Transactional(readOnly=true)` | Rendimiento | Menos memoria y CPU en operaciones de lectura |
| `FetchType.LAZY` | Rendimiento | Menos queries innecesarias al cargar entidades |
| Eliminar `findById` redundantes | Rendimiento | Elimina 2 queries innecesarias |
| Eliminar `POST /api/usuarios` | Seguridad | Cierra agujero de contraseñas en texto plano |
| `AuthController` usa servicio | Arquitectura | Respeta la separación de capas |
| `open-in-view=false` | Rendimiento | Libera conexiones a la BD antes |
| `show-sql=false` | Rendimiento | Elimina I/O innecesario en logs |

---

## Segunda ronda de optimizaciones (20/04/2026)

### 9. Manejo de errores centralizado con `GlobalExceptionHandler`

**Archivos creados:**
- `exception/RecursoNoEncontradoException.java` — lanza HTTP 404
- `exception/ReglaNegocioException.java` — lanza HTTP 400
- `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice` que intercepta ambas y devuelve JSON uniforme

**Antes:** cada controlador devolvía errores de formas distintas — algunos `ResponseEntity.notFound().build()` (sin body), otros `ResponseEntity.badRequest().body("string plano")`, y los `try-catch` mezclaban lógica de error con lógica de negocio.

**Después:** todos los servicios lanzan excepciones tipadas. El `GlobalExceptionHandler` las convierte a un JSON uniforme:

```json
{
    "timestamp": "2026-04-20T12:34:56.789",
    "status": 404,
    "mensaje": "Álbum no encontrado"
}
```

Esto elimina todo el código de manejo de errores disperso por los controladores y garantiza un formato consistente en todos los endpoints.

---

### 10. Protección de contraseña con `@JsonProperty(WRITE_ONLY)`

**Afecta a:** `model/Usuario.java`

**Problema:** el hash BCrypt de la contraseña se incluía en todas las respuestas JSON de la API. Cualquier `GET /api/usuarios/{id}` devolvía el hash.

**Solución:**
```java
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE)
@Entity @Data
public class Usuario {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
```

**Por qué son necesarias las dos anotaciones:** `@JsonProperty(WRITE_ONLY)` a nivel de campo funciona correctamente. Pero Lombok `@Data` genera un método `getPassword()`, y Jackson detecta ese getter y serializa el campo ignorando la anotación del campo. La solución es decirle a Jackson que ignore los getters (`getterVisibility = NONE`) y use directamente los campos (`fieldVisibility = ANY`).

---

### 11. Corrección de `fechaUltimoLogin`

**Afecta a:** `service/UsuarioService.java`, `controller/AuthController.java`

**Problema:** `AuthController.login` llamaba a `usuarioService.actualizar(id, datos)`, que solo copia `username`, `fotoPerfil` y `bio`. `fechaUltimoLogin` nunca se guardaba en la BD.

**Solución:** método dedicado en el servicio:

```java
@Transactional
public void actualizarUltimoLogin(Long id) {
    Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    usuario.setFechaUltimoLogin(LocalDateTime.now());
    usuarioRepository.save(usuario);
}
```

`AuthController.login` llama ahora a `actualizarUltimoLogin(usuario.getId())` justo antes de devolver el token.

---

### 12. CORS configurado para el frontend

**Afecta a:** `SecurityConfig.java`

Se añadió configuración CORS explícita en Spring Security para permitir peticiones desde `http://localhost:5173` (puerto de Vite en desarrollo):

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Sin esto, el navegador bloquearía todas las peticiones del frontend al backend.

---

### 13. Constructor injection en la capa de seguridad

**Afecta a:** `security/JwtFilter.java`, `security/UserDetailsServiceImpl.java`

La primera ronda de refactorización aplicó constructor injection en servicios y controladores, pero se olvidó la capa de seguridad. Ahora también usan `@RequiredArgsConstructor` con campos `final`, igual que el resto de clases del proyecto.

---

## Resumen de mejoras (segunda ronda)

| Cambio | Tipo | Impacto |
|---|---|---|
| `GlobalExceptionHandler` | Calidad + Seguridad | Errores JSON uniformes, elimina try-catch dispersos |
| `@JsonProperty(WRITE_ONLY)` en password | Seguridad | Hash BCrypt nunca expuesto en respuestas |
| `actualizarUltimoLogin` dedicado | Corrección | `fechaUltimoLogin` ahora se persiste correctamente |
| CORS para `localhost:5173` | Funcionalidad | Frontend puede comunicarse con el backend |
| Constructor injection en security/ | Calidad | Consistencia con el resto del proyecto |
