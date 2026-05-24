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

---

## Verificación de email y refresh tokens (23/05/2026)

### Motivación

El sistema original registraba al usuario y le devolvía un token JWT de acceso de inmediato, sin comprobar que el email fuese válido. Cualquiera podía registrarse con el email de otra persona. Además, el token JWT tenía una ventana de expiración fija de 1 hora sin mecanismo de renovación — el usuario perdía la sesión sin poder recuperarla sin volver a hacer login.

Se implementaron dos mecanismos complementarios:

1. **Verificación de email**: el usuario no puede iniciar sesión hasta confirmar el correo.
2. **Refresh tokens**: token UUID de larga duración (7 días) para renovar el JWT de acceso sin pedir credenciales.

---

### Nuevos archivos

```
com.musicreviews.backend/
├── model/
│   └── RefreshToken.java            ← Entidad JPA (token UUID, usuario, expiración)
├── repository/
│   └── RefreshTokenRepository.java  ← findByToken, deleteByUsuario
└── service/
    ├── RefreshTokenService.java     ← crear, verificar, limpiar expirados
    └── EmailService.java            ← envío de email HTML via Brevo SMTP
```

**Dependencias añadidas en `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

---

### Cambios en archivos existentes

| Archivo | Cambio |
|---|---|
| `Usuario.java` | +`emailVerificado` (boolean), +`tokenVerificacion` (String) |
| `UsuarioService.java` | +`registrar()` con JdbcTemplate para token, +`JdbcTemplate` injection |
| `AuthController.java` | `register` → llama a `registrar()` + `emailService.enviarConfirmacion()`, +`GET /verificar`, +`POST /refresh`, +`POST /logout` |
| `AuthResponse.java` | + campo `refreshToken` |
| `SecurityConfig.java` | + rutas `/api/auth/verificar` y `/api/auth/refresh` como públicas |
| `GlobalExceptionHandler.java` | + `MailException` traducida a 503 con mensaje legible |
| `application.properties` | + configuración SMTP Brevo, + `refresh-token.expiration-days` |

---

### Flujo de registro con verificación

```
Usuario → POST /api/auth/register
  ↓
UsuarioService.registrar()
  ├── Valida duplicados (email, username)
  ├── Crea Usuario con emailVerificado=false
  ├── saveAndFlush() → Hibernate INSERT (token_verificacion = NULL)
  └── JdbcTemplate.update("UPDATE usuario SET token_verificacion = ? WHERE id = ?", token, id)
  ↓
EmailService.enviarConfirmacion()
  └── envía enlace: http://localhost:5173/verificar-email?token=<uuid>
  ↓
Responde: 200 { "mensaje": "Hemos enviado un correo a ..." }
```

---

### Flujo de login con verificación de email

```
Usuario → POST /api/auth/login
  ↓
AuthController.login()
  ├── busca usuario por email
  ├── comprueba BCrypt
  ├── if (!activo) → 400 "Cuenta desactivada"
  ├── if (!emailVerificado) → 400 "Debes verificar tu email..."
  ├── actualizarUltimoLogin()
  ├── genera JWT (1 hora)
  └── RefreshTokenService.crear() → genera UUID, guarda en BD con expiración 7 días
  ↓
Responde: 200 { accessToken, refreshToken, id, username, email, rol }
```

---

### Flujo de verificación de email

```
Usuario hace click en el enlace del email
  ↓
VerificarEmail.jsx → GET /api/auth/verificar?token=<uuid>
  ↓
AuthController.verificar()
  ├── usuarioRepository.findByTokenVerificacion(token)
  ├── if not found → 400 "Enlace de verificación inválido o ya utilizado"
  ├── usuario.setEmailVerificado(true)
  ├── usuario.setTokenVerificacion(null)
  └── usuarioRepository.save(usuario)
  ↓
Responde: 200 { "mensaje": "Cuenta verificada correctamente." }
```

---

### Flujo de renovación de token (refresh)

```
Frontend detecta JWT expirado (401)
  ↓
interceptor Axios → POST /api/auth/refresh { "refreshToken": "<uuid>" }
  ↓
RefreshTokenService.verificar()
  ├── findByToken
  ├── if not found → 400 "Refresh token inválido"
  └── if expirado → elimina + 400 "Refresh token expirado"
  ↓
JwtUtil.generarToken(email, rol) → nuevo JWT (1 hora)
  ↓
Responde: 200 { "token": "<nuevo-jwt>" }
```

---

### Endpoints añadidos

#### `GET /api/auth/verificar?token=<uuid>`

Activa la cuenta. Público, sin autenticación.

**Respuesta 200:**
```json
{ "mensaje": "Cuenta verificada correctamente. Ya puedes iniciar sesión." }
```
**Error 400:** token no encontrado o ya usado.

---

#### `POST /api/auth/refresh`

Renueva el JWT usando un refresh token.

**Body:**
```json
{ "refreshToken": "uuid-del-refresh-token" }
```
**Respuesta 200:**
```json
{ "token": "nuevo-jwt" }
```

---

#### `POST /api/auth/logout`

Invalida el refresh token en el servidor.

**Body:**
```json
{ "refreshToken": "uuid-del-refresh-token" }
```
**Respuesta:** 204 No Content.

---

### Cambios en el frontend

**`src/services/auth.js`**
- `verificarEmail(token)` — GET `/api/auth/verificar?token=`
- `refreshAccessToken(refreshToken)` — POST `/api/auth/refresh`
- `logout(refreshToken)` — POST `/api/auth/logout`

**`src/context/AuthContext.jsx`**
- Guarda `refreshToken` en localStorage junto al JWT
- Interceptor Axios: si recibe 401, intenta refresh automático antes de forzar logout
- `logout()` llama al endpoint `/api/auth/logout` para invalidar el refresh token en servidor

**`src/pages/VerificarEmail.jsx`**
- Página nueva. Lee `?token=` de la URL y llama a `verificarEmail(token)`
- Muestra spinner mientras verifica, éxito con enlace a login, o error con mensaje

**`src/pages/Registro.jsx`**
- Tras registro exitoso, muestra panel informando al usuario que revise su email en vez de redirigir a login directamente

**`src/App.jsx`**
- Añadida ruta `/verificar-email` → `<VerificarEmail />`

---

### Configuración de email (Brevo SMTP)

```properties
spring.mail.host=smtp-relay.brevo.com
spring.mail.port=587
spring.mail.username=ac5298001@smtp-brevo.com
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=musicreviews@outlook.es
app.url=http://localhost:5173
```

El email se envía como HTML con un botón que enlaza a `{app.url}/verificar-email?token={token}`.

---

## Proceso de debugging: token_verificacion siempre NULL

Esta sección documenta el problema más complejo encontrado durante el desarrollo del TFG: por qué `token_verificacion` llegaba siempre como `NULL` a la base de datos pese al código aparentemente correcto.

### Síntoma inicial

Tras implementar el registro con verificación, todos los usuarios en la BD tenían `token_verificacion = NULL`. El email llegaba con un enlace, pero al hacer click el backend devolvía "Enlace de verificación inválido o ya utilizado" porque no encontraba ningún usuario con ese token.

### Intento 1 — `@JsonIgnore`

**Hipótesis:** Jackson ignoraba el campo al serializar y quizá también al persistir.
**Resultado:** No era el problema — `@JsonIgnore` no afecta a JPA, solo a la serialización JSON.

### Intento 2 — `@Modifying @Query` JPQL

Se añadió un método al repositorio:
```java
@Modifying
@Query("UPDATE Usuario u SET u.tokenVerificacion = :token WHERE u.id = :id")
void setTokenVerificacion(@Param("id") Long id, @Param("token") String token);
```
**Resultado:** El código compilaba y no lanzaba error, pero el token seguía siendo NULL en BD.

### Intento 3 — `@Modifying @Query` nativa SQL + `clearAutomatically = true`

```java
@Transactional
@Modifying(clearAutomatically = true)
@Query(value = "UPDATE usuario SET token_verificacion = :token WHERE id = :id", nativeQuery = true)
void setTokenVerificacion(@Param("id") Long id, @Param("token") String token);
```
**Resultado:** Aparentemente el mismo resultado. La BD seguía mostrando NULL.

### Intento 4 — `saveAndFlush()`

**Hipótesis:** El token se establece en la entidad Java pero Hibernate no hace el flush antes de que acabe la transacción.

```java
usuario.setTokenVerificacion(token);
usuarioRepository.saveAndFlush(usuario);
```
**Resultado:** NULL persiste. El flush fuerza el SQL inmediatamente, pero el problema estaba antes.

### Diagnóstico: activar logging SQL

Se activó el logging de parámetros de Hibernate:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

El log reveló la causa raíz:
```
Hibernate: insert into usuario (activo,bio,email,email_verificado,fecha_ultimo_login,fecha_registro,foto_perfil,password,rol,token_verificacion,username) values (?,?,?,?,?,?,?,?,?,?,?)
TRACE binding parameter (10:VARCHAR) <- [null]
```

**Hibernate enviaba `token_verificacion = null` en el INSERT aunque el campo estuviera seteado en Java.** El campo se marcaba en el estado JPA como "no dirty" en el momento de la inserción porque la sesión tenía un estado inconsistente al establecer el campo después de resolver el ID con `GenerationType.IDENTITY`.

### Solución final — JdbcTemplate bypass

En lugar de luchar con el estado interno de Hibernate, se usó `JdbcTemplate` para hacer el UPDATE directamente vía JDBC puro, completamente fuera de Hibernate:

```java
private final JdbcTemplate jdbcTemplate;

@Transactional
public String registrar(String username, String email, String encodedPassword) {
    // Paso 1: INSERT limpio vía JPA (Hibernate envía token = null)
    Usuario usuario = new Usuario();
    usuario.setUsername(username);
    usuario.setEmail(email);
    usuario.setPassword(encodedPassword);
    usuario.setEmailVerificado(false);
    Usuario guardado = usuarioRepository.saveAndFlush(usuario);
    Long userId = guardado.getId();

    // Paso 2: UPDATE directo vía JDBC — bypassa Hibernate completamente
    String token = UUID.randomUUID().toString();
    jdbcTemplate.update(
            "UPDATE usuario SET token_verificacion = ? WHERE id = ?",
            token, userId
    );

    return token;
}
```

El log confirmó que el UPDATE llegaba correctamente a la BD:
```
1 fila(s) afectada(s) — token = 971c9d2c-...
```

---

## Proceso de debugging: doble verificación por React StrictMode

### Síntoma

Tras confirmar que el token se guardaba correctamente en BD, la verificación de email seguía fallando. La página mostraba "Verificación fallida — Enlace de verificación inválido o ya utilizado".

### Diagnóstico

Con el logging SQL activo, el log del backend mostró la secuencia exacta de SQLs durante una verificación:

```
[T+0ms]  SELECT * FROM usuario WHERE token_verificacion = '971c9d2c-...'  → encontrado ✓
[T+2ms]  UPDATE usuario SET email_verificado=true, token_verificacion=null WHERE id=36
[T+10ms] SELECT * FROM usuario WHERE token_verificacion = '971c9d2c-...'  → NOT FOUND ✗
```

**Dos peticiones GET `/api/auth/verificar?token=...` llegaban al backend casi simultáneamente.**

- La primera: encontraba el token, lo ponía a NULL, devolvía 200 OK.
- La segunda (10ms después): no encontraba el token (ya era NULL), devolvía 400 Error.

El frontend mostraba el resultado de la segunda petición.

### Causa raíz

**React 18+ StrictMode invoca todos los efectos dos veces en modo desarrollo** para detectar side effects no puros. El `useEffect` de `VerificarEmail.jsx` se ejecutaba dos veces, enviando dos peticiones simultáneas al backend.

### Solución — `useRef` guard

```jsx
const llamadaHecha = useRef(false);

useEffect(() => {
  // Evita la segunda llamada del StrictMode de React en desarrollo
  if (llamadaHecha.current) return;
  llamadaHecha.current = true;

  const token = params.get("token");
  verificarEmail(token)
    .then(() => setEstado("ok"))
    .catch((err) => { setEstado("error"); setMensaje(err.message); });
}, []);
```

Un `useRef` persiste entre renders sin provocar re-renders. El flag `llamadaHecha.current` se establece a `true` en la primera ejecución, y la segunda llamada del StrictMode sale inmediatamente sin hacer la petición.

---

## Bug: email rechazado por Brevo (sender no verificado)

### Síntoma

Los primeros intentos de envío devolvieron: `"Sending has been rejected because sender plaza953pablo@gmail.com is not valid"`.

### Causa

Brevo exige verificar el dominio o dirección del remitente antes de permitir el envío. El email `plaza953pablo@gmail.com` no estaba verificado en la cuenta Brevo.

### Solución

1. Crear `musicreviews@outlook.es` como dirección de remitente en el panel de Brevo.
2. Verificarla siguiendo el proceso de confirmación por email de Brevo.
3. Actualizar `app.mail.from=musicreviews@outlook.es` en `application.properties`.

Adicionalmente, Gmail aplicaba filtros SPF que silenciaban los emails enviados con un remitente `@gmail.com` a través de un servidor SMTP de terceros. Usar `musicreviews@outlook.es` como remitente evitó ese problema.

---

## Restablecimiento de contraseña por email (24/05/2026)

### Motivación

El sistema de autenticación permitía cambiar la contraseña solo estando logueado (editar perfil), pero no existía ningún mecanismo de recuperación cuando el usuario la olvidaba. El link "¿Olvidaste tu contraseña?" en el formulario de login apuntaba a `#` como placeholder. Se implementó el flujo completo de recuperación de contraseña por email, aprovechando la infraestructura de Brevo SMTP ya configurada para la verificación de email.

---

### Flujo completo

```
Usuario → POST /api/auth/forgot-password { email }
  ↓
UsuarioService.generarTokenReset(email)
  ├── Busca el usuario por email
  ├── Si no existe → NO lanza error (no revelar si el email está registrado)
  ├── Genera token UUID y fecha de expiración (24 horas)
  └── JdbcTemplate.update("UPDATE usuario SET token_restablecimiento = ?, fecha_expiracion_reset = ? WHERE id = ?")
  ↓
EmailService.enviarRestablecimiento(email, username, token)
  └── envía HTML con botón → {app.url}/reset-password?token=<uuid>
  ↓
Responde siempre: 200 { "mensaje": "Si ese email está registrado, recibirás un correo en breve." }

────────────────────────────────────────────────────

Usuario → POST /api/auth/reset-password { token, nuevaPassword }
  ↓
UsuarioService.restablecerPassword(token, rawPassword)
  ├── findByTokenRestablecimiento(token) → 400 si no existe
  ├── if (fechaExpiracionReset.isBefore(now())) → 400 "Token expirado"
  ├── if (passwordEncoder.matches(raw, stored)) → 400 "La nueva contraseña no puede ser igual a la actual"
  ├── Codifica: passwordEncoder.encode(raw)
  └── JdbcTemplate.update("UPDATE usuario SET password = ?, token_restablecimiento = NULL, fecha_expiracion_reset = NULL WHERE id = ?")
  ↓
Responde: 200 { "mensaje": "Contraseña actualizada correctamente." }
```

---

### Archivos nuevos

| Archivo | Contenido |
|---|---|
| `dto/ForgotPasswordRequest.java` | `@Email @NotBlank String email` |
| `dto/ResetPasswordRequest.java` | `@NotBlank String token`, `@Pattern(...) String nuevaPassword` |
| `src/pages/ForgotPassword.jsx` | Formulario de email con estado "enviado" |
| `src/pages/ResetPassword.jsx` | Formulario de nueva contraseña con indicador de fortaleza |

---

### Cambios en archivos existentes

| Archivo | Cambio |
|---|---|
| `model/Usuario.java` | +`tokenRestablecimiento` (String, @JsonIgnore), +`fechaExpiracionReset` (LocalDateTime, @JsonIgnore) |
| `repository/UsuarioRepository.java` | +`findByTokenRestablecimiento(String)` |
| `service/UsuarioService.java` | +`generarTokenReset(email)`, +`restablecerPassword(token, rawPassword)`, +`PasswordEncoder` inyectado |
| `service/EmailService.java` | +`enviarRestablecimiento(email, username, token)` con template HTML dark glassmorphism |
| `controller/AuthController.java` | +`POST /api/auth/forgot-password`, +`POST /api/auth/reset-password` |
| `dto/RegisterRequest.java` | password actualizado de `@Size(min=6)` a `@Pattern(regexp="^(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$")` |
| `services/auth.js` | +`forgotPassword(email)`, +`resetPassword(token, nuevaPassword)` |
| `App.jsx` | +rutas `/forgot-password` y `/reset-password` |
| `pages/Login.jsx` | link "¿Olvidaste tu contraseña?" actualizado de `#` a `/forgot-password` |

---

### Política de contraseñas seguras

Con la implementación del flujo de reset se aprovechó para endurecer la política de contraseñas en toda la aplicación:

**Backend:** `@Pattern(regexp="^(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$")` en `RegisterRequest` y `ResetPasswordRequest`. Requisitos:
- Mínimo 8 caracteres
- Al menos un dígito (0-9)
- Al menos un carácter especial (cualquier no alfanumérico)

**Frontend** (`ResetPassword.jsx` — indicador visual de fortaleza):

```js
const REGLAS = [
  { label: "Mínimo 8 caracteres", test: (p) => p.length >= 8 },
  { label: "Al menos un número", test: (p) => /\d/.test(p) },
  { label: "Al menos un carácter especial", test: (p) => /[^a-zA-Z0-9]/.test(p) },
];

function nivelFortaleza(pwd) {
  const n = REGLAS.filter((r) => r.test(pwd)).length;
  if (n === 0) return null;
  if (n === 1) return "débil";
  if (n === 2) return "media";
  return "fuerte";
}
```

Barra de progreso (1/3 → 2/3 → 3/3) con colores `error` / `yellow-500` / `primary` según el nivel. Checklist de reglas con ✓/✗ que se actualiza al teclear.

**Comprobación "misma contraseña":** antes de llamar al backend, `restablecerPassword` en el service comprueba `passwordEncoder.matches(rawPassword, usuario.getPassword())` y lanza `ReglaNegocioException` si coincide. Esta validación solo puede hacerse en el servidor porque el hash BCrypt no es determinista.

---

### Decisión de seguridad: respuesta invariante en forgot-password

`POST /api/auth/forgot-password` siempre responde con el mismo mensaje independientemente de si el email existe en la BD. Si respondiera diferente ("email no encontrado" vs "email enviado"), un atacante podría enumerar los emails registrados enviando miles de peticiones. El único riesgo de la respuesta invariante es que el usuario no sepa inmediatamente si su email está registrado — se asume aceptable porque el email tiene que haberlo introducido él mismo al registrarse.

A nivel de implementación: `generarTokenReset` captura el `Optional.empty()` y retorna sin hacer nada; `AuthController` envuelve todo en `try/catch` genérico para que cualquier excepción inesperada tampoco revele información.

---

### JdbcTemplate para el token de reset

Se usó el mismo patrón JdbcTemplate que para `token_verificacion` (documentado en la sección anterior). La razón es idéntica: con `GenerationType.IDENTITY`, Hibernate no puede incluir el token en el `INSERT` porque el ID aún no existe cuando se construye la entidad. El UPDATE directo vía JDBC sortea el estado interno de Hibernate:

```java
jdbcTemplate.update(
    "UPDATE usuario SET token_restablecimiento = ?, fecha_expiracion_reset = ? WHERE id = ?",
    token, LocalDateTime.now().plusHours(24), userId
);
```

---

### Endpoints añadidos

#### `POST /api/auth/forgot-password`

**Body:**
```json
{ "email": "pablo@gmail.com" }
```

**Respuesta 200** (siempre, independientemente de si el email existe):
```json
{ "mensaje": "Si ese email está registrado, recibirás un correo en breve." }
```

---

#### `POST /api/auth/reset-password`

**Body:**
```json
{
  "token": "uuid-del-token",
  "nuevaPassword": "NuevaPass1!"
}
```

**Respuesta 200:**
```json
{ "mensaje": "Contraseña actualizada correctamente." }
```

**Errores:**
- `400` — token inválido o no encontrado
- `400` — token expirado (> 24 horas)
- `400` — nueva contraseña igual a la actual
- `400` — contraseña no cumple el patrón de seguridad

---

### Bug: pantalla en blanco al teclear en ResetPassword

**Síntoma:** la página `ResetPassword.jsx` quedaba en blanco en cuanto el usuario empezaba a escribir la nueva contraseña.

**Causa:** el componente calculaba `const nivel = nivelFortaleza(nuevaPassword)` y luego renderizaba `{nuevaPassword && <div style={{ color: COLOR_NIVEL[nivel] }}>...}`. Cuando el usuario teclea el primer carácter, `nivelFortaleza` devuelve `null` si no se cumple ninguna regla. `COLOR_NIVEL[null]` devuelve `undefined` — no lanza excepción, pero el `style` recibe un valor inválido que en ciertos browsers causa que React desmonte el árbol completo.

**Solución:** cambiar la condición de render a `{nivel && ...}` en lugar de `{nuevaPassword && ...}`. Si `nivel` es `null` (cero reglas cumplidas), el bloque simplemente no se renderiza.

```jsx
// ❌ Antes
{nuevaPassword && <div style={{ color: COLOR_NIVEL[nivel] }}>...</div>}

// ✅ Después
{nivel && <div style={{ color: COLOR_NIVEL[nivel] }}>...</div>}
```
