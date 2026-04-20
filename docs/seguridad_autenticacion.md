# Seguridad y Autenticación

## Por qué se implementó

La aplicación necesita proteger los datos de los usuarios y controlar quién puede hacer qué. Sin autenticación, cualquiera podría crear reseñas como otro usuario, borrar datos o acceder a endpoints de administración. Se implementó un sistema de autenticación basado en **JWT (JSON Web Tokens)** con **BCrypt** para el cifrado de contraseñas.

---

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Spring Security | 7.0.4 | Framework de seguridad base |
| jjwt | 0.12.6 | Generación y validación de tokens JWT |
| BCrypt | — | Cifrado de contraseñas (incluido en Spring Security) |

---

## Cómo funciona

### Flujo de registro

1. El usuario envía `POST /api/auth/register` con `username`, `email` y `password`
2. Se comprueba que el email y el username no estén ya en uso
3. La contraseña se cifra con **BCrypt** antes de guardarla — nunca se almacena en texto plano
4. Se crea el usuario con rol `USER` por defecto y `activo = true`
5. Se devuelve un token JWT junto con los datos básicos del usuario

### Flujo de login

1. El usuario envía `POST /api/auth/login` con `email` y `password`
2. Se busca el usuario por email en la BD
3. Se verifica la contraseña con BCrypt (compara el texto plano con el hash guardado)
4. Si la cuenta está desactivada (`activo = false`) se devuelve 403
5. Se actualiza `fechaUltimoLogin` con la fecha y hora actual
6. Se devuelve un token JWT

### Flujo de petición autenticada

1. El frontend incluye el token en el header: `Authorization: Bearer <token>`
2. El `JwtFilter` intercepta la petición antes de que llegue al controlador
3. Extrae y valida el token: comprueba firma, expiración y formato
4. Si es válido, carga el usuario y establece la autenticación en el contexto de Spring Security
5. Spring Security aplica las reglas de autorización según el rol

### Token JWT

El token contiene:
- `sub` — email del usuario
- `rol` — rol del usuario (`USER` o `ADMIN`)
- `iat` — fecha de emisión
- `exp` — fecha de expiración (24 horas por defecto)

Está firmado con HMAC-SHA512 usando la clave secreta del `application.properties`.

---

## Estructura de archivos creados

```
com.musicreviews.backend/
├── SecurityConfig.java              ← Configuración de rutas, filtros y CORS
├── security/
│   ├── JwtUtil.java                 ← Genera y valida tokens JWT
│   ├── JwtFilter.java               ← Intercepta requests y verifica el token
│   └── UserDetailsServiceImpl.java  ← Carga el usuario por email para Spring Security
├── controller/
│   └── AuthController.java          ← Endpoints /api/auth/register y /api/auth/login
└── dto/
    ├── RegisterRequest.java         ← Body del registro
    ├── LoginRequest.java            ← Body del login
    └── AuthResponse.java            ← Respuesta con token y datos del usuario
```

---

## Endpoints de autenticación

### `POST /api/auth/register`

**Body:**
```json
{
    "username": "pablo",
    "email": "pablo@gmail.com",
    "password": "1234"
}
```

**Respuesta 200:**
```json
{
    "token": "eyJhbGci...",
    "id": 1,
    "username": "pablo",
    "email": "pablo@gmail.com",
    "rol": "USER"
}
```

**Errores:**
- `400` — email o username ya en uso

---

### `POST /api/auth/login`

**Body:**
```json
{
    "email": "pablo@gmail.com",
    "password": "1234"
}
```

**Respuesta 200:** igual que register (nuevo token)

**Errores:**
- `401` — email o contraseña incorrectos
- `403` — cuenta desactivada

---

## Autorización por roles

| Ruta | Método | Acceso |
|---|---|---|
| `/api/auth/**` | POST | Público |
| `/api/artistas/**` | GET | Público |
| `/api/albumes/**` | GET | Público |
| `/api/resenas/**` | GET | Público |
| `/api/artistas/**` | POST / PUT / DELETE | Solo ADMIN |
| `/api/albumes/**` | POST / PUT / DELETE | Solo ADMIN |
| `/api/spotify/**` | GET | Solo ADMIN |
| `/api/favoritos/**` | * | Autenticado (USER o ADMIN) |
| `/api/resenas/**` | POST / PUT / DELETE | Autenticado (USER o ADMIN) |
| `/api/usuarios/**` | PUT / DELETE | Autenticado (USER o ADMIN) |

---

## Cambios en la entidad Usuario

Se añadieron dos nuevos campos durante esta fase:

| Campo | Tipo | Descripción |
|---|---|---|
| `activo` | boolean | Indica si la cuenta está activa. `true` por defecto. Permite desactivar usuarios sin borrarlos |
| `fechaUltimoLogin` | LocalDateTime | Se actualiza automáticamente en cada login |

Hibernate los añadió automáticamente en Aiven al arrancar con `ddl-auto=update`:
```sql
alter table usuario add column activo bit not null
alter table usuario add column fecha_ultimo_login datetime(6)
```

---

## Mejoras de seguridad adicionales (20/04/2026)

### Protección del hash de contraseña

El campo `password` de `Usuario` nunca debe aparecer en respuestas JSON. Se protegió con:

```java
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE)
@Entity @Data
public class Usuario {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
```

El hash BCrypt de la contraseña ahora se acepta en escritura (registro) pero nunca se incluye en ninguna respuesta de la API.

**Por qué son necesarias las dos anotaciones:** la anotación de campo sola no basta porque Lombok `@Data` genera `getPassword()`, y Jackson usa ese getter ignorando la anotación del campo. Se necesita `@JsonAutoDetect` para decirle a Jackson que use los campos directamente y descarte los getters.

---

### CORS para el frontend

Se configuró CORS en `SecurityConfig` para permitir peticiones desde `http://localhost:5173` (Vite):

```java
config.setAllowedOrigins(List.of("http://localhost:5173"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

La configuración se aplica a todas las rutas (`/**`) y está integrada en el `filterChain` de Spring Security.

---

### Constructor injection en la capa de seguridad

`JwtFilter` y `UserDetailsServiceImpl` ahora usan `@RequiredArgsConstructor` con campos `final`, igual que el resto de clases del proyecto. Antes usaban `@Autowired` sobre campos, que es el patrón antiguo de Spring.

---

### `fechaUltimoLogin` ahora se persiste correctamente

El flujo de login llama a `usuarioService.actualizarUltimoLogin(id)`, un método dedicado que carga el usuario y guarda la fecha actual. Antes se usaba `actualizar()`, que solo copia los campos de perfil (username, foto, bio) y nunca actualizaba la fecha.

---

## Pruebas realizadas con Postman

### Prueba 1 — Registro de usuario
- **Request:** `POST /api/auth/register` con body válido
- **Primer intento:** devolvió `403 Forbidden`
- **Causa:** Postman estaba enviando `GET` en lugar de `POST`
- **Solución:** cambiar el método a POST en Postman
- **Resultado final:** `200 OK` con token JWT ✅

### Prueba 2 — Registro con datos duplicados
- **Request:** `POST /api/auth/register` con username ya existente
- **Resultado:** `400 Bad Request` — "Ya existe un usuario con ese username" ✅

### Prueba 3 — Login
- **Request:** `POST /api/auth/login` con credenciales correctas
- **Resultado:** `200 OK` con token JWT ✅

### Prueba 4 — Ruta protegida sin token
- **Request:** `GET /api/favoritos?usuarioId=1` sin header Authorization
- **Primer resultado:** `403 Forbidden` (incorrecto)
- **Causa:** Spring Security devuelve 403 por defecto para todo lo denegado, incluyendo requests no autenticados
- **Solución:** añadir `authenticationEntryPoint` en `SecurityConfig` para devolver 401 específicamente cuando no hay autenticación
- **Resultado final:** `401 Unauthorized` ✅

### Prueba 5 — Ruta de ADMIN con token de USER
- **Request:** `POST /api/artistas` con token de rol USER
- **Resultado:** `403 Forbidden` — autenticado pero sin permisos ✅

### Resumen de pruebas

| Prueba | Resultado |
|---|---|
| Registro nuevo usuario | ✅ 200 + token |
| Registro con datos duplicados | ✅ 400 + mensaje |
| Login correcto | ✅ 200 + token |
| Ruta protegida sin token | ✅ 401 |
| Ruta ADMIN con token USER | ✅ 403 |
