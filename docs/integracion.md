# Fase 4 — Integración Frontend ↔ Backend

Documentación del proceso de conectar el frontend React (puerto 5173) con el backend Spring Boot (puerto 8080). Este documento explica **cada decisión técnica, los bugs encontrados al integrar, y cómo se han resuelto** para poder defenderlo en la presentación.

**Punto de partida:** backend al 100% (CRUD, JWT, 38 tests unitarios pasando, BD en Aiven con ~732 álbumes, datos de prueba) y frontend al 100% en cuanto a maquetación (15 pantallas, navegación interna, datos mock). La fase 4 conecta lo uno con lo otro.

**Plan de la fase (9 pasos):**

1. AuthContext — estado compartido del usuario logueado
2. Login + Registro — formularios reales contra el backend
3. Navbar dinámico — cambia según haya sesión o no
4. Rutas protegidas — algunas pantallas requieren login
5. Páginas públicas — Catálogo, Búsqueda, Rankings con datos reales
6. Páginas de álbum — Detalle de álbum y artista
7. Páginas de usuario — Perfil, editar perfil, favoritos
8. Reseñas — crear, editar, borrar desde la UI
9. Portadas — gestión de imágenes de álbum y artista

Este documento se irá ampliando paso a paso. La sesión 1 cubrió los **pasos 1 y 2** (AuthContext y formularios reales) y los **bugs del backend** que destapó la integración. La sesión 2 añade el **paso 3 (Navbar dinámico)** — primera vez que el contexto se usa fuera de los formularios de auth.

---

## 1. AuthContext — el estado compartido de la sesión

### El problema que resuelve

Sin un contexto, cada componente que necesite saber si hay un usuario logueado tendría que mirar `localStorage` por su cuenta y volver a leerlo cada vez. El `Navbar` lo necesita para decidir qué botones mostrar, el `Login.jsx` para guardar el token al entrar, las páginas protegidas para redirigir, las llamadas a la API para mandar el token en el header `Authorization`. Si cada uno gestiona su copia, la app se desincroniza al instante.

La solución de React para esto es **Context**: una "caja" global que se rellena en un sitio y se lee desde cualquier componente con un hook.

### Estructura

Tres ficheros se coordinan:

| Fichero | Rol |
|---|---|
| `src/services/auth.js` | Capa de red. Hace `fetch` al backend, parsea respuestas, lanza `Error` si falla. No sabe nada de React. |
| `src/context/AuthContext.jsx` | Estado React. Guarda `token` y `usuario`, persiste en localStorage, expone `login()`, `register()`, `logout()`. |
| `src/main.jsx` | Conexión. Envuelve toda la app con `<AuthProvider>` para que cualquier componente pueda leer el contexto. |

### `services/auth.js` — la capa de comunicación

```js
const API = "http://localhost:8080/api";

export async function login(email, password) {
  const res = await fetch(`${API}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.mensaje || "Error al iniciar sesión");
  return data; // { token, id, username, email, rol }
}
```

Decisiones:

- **Función pura sin dependencias de React**. Si mañana se quiere reusar desde un script de Node o cambiar a Axios, solo se toca este fichero.
- **El `Error` lleva el `mensaje` que devuelve el backend** (`{"mensaje": "Email o contraseña incorrectos", ...}`). Quien lo llame solo tiene que hacer `try/catch` y mostrar `err.message`.
- **`if (!res.ok)`** atrapa cualquier status que no sea 2xx. Esto requiere que el backend **siempre** devuelva JSON con un campo `mensaje` también en errores. Esto provocó el **Bug B4** (ver más abajo) y obligó a corregir el backend.

### `context/AuthContext.jsx` — el estado React

Las cuatro responsabilidades del fichero, una por una:

**(a) Inicialización desde localStorage** — para que la sesión persista al refrescar la página:

```js
const [token, setToken] = useState(() => localStorage.getItem("token"));
const [usuario, setUsuario] = useState(() => {
  const saved = localStorage.getItem("usuario");
  return saved ? JSON.parse(saved) : null;
});
```

El `() => ...` es la "lazy initial state" de React: la función solo se ejecuta una vez, al montar el componente. Sin esa flecha, se leería localStorage en cada render.

**(b) Acciones que invocan al backend** — `login()` y `register()` son las funciones que el componente Login.jsx/Registro.jsx van a llamar:

```js
async function login(email, password) {
  const data = await loginService(email, password);
  guardarSesion(data);
}
```

Se renombra `loginService` con alias para distinguirla de la función del propio contexto. Si el backend falla, `loginService` lanza `Error` y `login()` lo deja propagar para que el formulario lo atrape y lo muestre.

**(c) Persistencia centralizada** — toda escritura pasa por una sola función para no olvidar campos:

```js
function guardarSesion(data) {
  const { token, ...datosUsuario } = data;
  setToken(token);
  setUsuario(datosUsuario);
  localStorage.setItem("token", token);
  localStorage.setItem("usuario", JSON.stringify(datosUsuario));
}
```

Recibe la respuesta completa del backend (`{token, id, username, email, rol}`), separa el `token` por un lado y el resto en `usuario`. Esto deja el contrato claro: "token va aparte, datos del usuario juntos".

**(d) Logout** — el inverso. Limpia ambos sitios (estado React y localStorage). Sin esto, los datos sobrevivirían al refresh aunque el usuario quisiera salir.

```js
function logout() {
  setToken(null); setUsuario(null);
  localStorage.removeItem("token");
  localStorage.removeItem("usuario");
}
```

### Cómo se conecta a la app

`src/main.jsx` envuelve `<App />` con `<AuthProvider>`:

```jsx
<AuthProvider>
  <App />
</AuthProvider>
```

A partir de aquí, **cualquier** componente puede hacer:

```js
import { useAuth } from "../context/AuthContext";
const { usuario, login, logout } = useAuth();
```

Y recibe el estado actualizado automáticamente cuando cambia. React se encarga de re-renderizar.

### Flujo completo de un login

```
[Login.jsx]
  handleSubmit(e)
   └→ await login(email, password)        ← función del contexto
       ↓
[AuthContext.login()]
   └→ await loginService(email, password) ← capa de red
       ↓
[services/auth.js]
   └→ fetch("/api/auth/login", { ...body })
       ↓
[Backend Spring]
   └→ POST /api/auth/login
   └→ valida credenciales (bcrypt)
   └→ genera JWT (HS512, expira en 24h)
   └→ devuelve { token, id, username, email, rol }
       ↓
[services/auth.js]
   └→ res.json() ✅
       ↓
[AuthContext.guardarSesion(data)]
   ├→ setToken(...) → React re-renderiza Navbar y demás
   ├→ setUsuario(...)
   └→ localStorage ← persiste tras refresh
       ↓
[Login.jsx]
   └→ navigate("/")
```

---

## 2. Bugs del backend descubiertos al integrar

Al hacer las primeras pruebas reales con Postman después de tener el `AuthContext` y el `Login.jsx` listos, aparecieron problemas en el backend que **ya estaban ahí pero no se habían detectado** porque las pruebas anteriores con Postman no validaban el cuerpo completo de la respuesta JSON. La integración con un cliente real (frontend) los hizo evidentes.

> Nota: estos bugs se documentan aquí en lugar de en `pruebas_postman.md` porque pertenecen al ciclo de integración del frontend, no a las pruebas iniciales de cada controlador.

### Bug B1 — `LazyInitializationException` por `open-in-view=false`

**Síntoma:** desde Postman, `POST /api/resenas` con un token válido devolvía **401 Unauthorized**, no 200. Lo mismo con `GET /api/resenas/usuario/5/album/373`. Endpoints muy parecidos como `GET /api/albumes/3` sí funcionaban.

**Diagnóstico:** redirigir los logs del backend a fichero (`./mvnw spring-boot:run > backend.log`) y reproducir el fallo. El log mostraba:

```
HttpMessageNotWritableException: Could not write JSON:
Could not initialize proxy [com.musicreviews.backend.model.Album#373] - no session
```

Es un `LazyInitializationException` clásico: Jackson intenta serializar la respuesta, accede a `resena.album` que es un `@ManyToOne(fetch = FetchType.LAZY)` (un proxy de Hibernate), pero la sesión de Hibernate ya está cerrada → fallo.

**Causa raíz:** en `application.properties` había:

```
spring.jpa.open-in-view=false
```

Se había puesto a `false` en una sesión anterior "para mejorar el rendimiento". Lo que hace ese parámetro es cerrar la sesión Hibernate al salir del `@Transactional`, antes de que Jackson serialice. Para entidades sin relaciones LAZY (como `Album`) no afecta. Para `Resena` y `Favorito`, que tienen `@ManyToOne(fetch = LAZY)` a `usuario` y `album`, rompe la serialización.

El **misterio del 401 (en lugar del 500 esperado)** es que Spring Security 7 (Spring Boot 4.0.5) traduce los fallos durante la escritura del response como `Authentication` errors y dispara el `authenticationEntryPoint` configurado, que devuelve 401. Por eso el síntoma despistaba: parecía un problema de auth cuando en realidad era de serialización.

**Solución:** revertir a `open-in-view=true` (el default de Spring Boot por algo). El supuesto coste de rendimiento es despreciable en este TFG y permite que Jackson cargue las relaciones LAZY al serializar:

```
spring.jpa.open-in-view=true
```

**Verificación:** los mismos endpoints volvieron a 200 inmediatamente.

### Bug B2 — `@JsonAutoDetect` rompía los proxies de Hibernate

**Síntoma:** después de arreglar el B1, la respuesta del POST llegaba con código 200 pero el campo `usuario` aparecía con todos los valores a `null`:

```json
"usuario": {
  "$$_hibernate_interceptor": {},
  "activo": true,
  "bio": null,
  "email": null,
  "id": null,
  "username": null,
  ...
}
```

A la vez, aparecía basura interna de Hibernate (`$$_hibernate_interceptor`, `hibernateLazyInitializer`).

**Causa raíz:** en `Usuario.java` se había añadido en una sesión anterior:

```java
@JsonAutoDetect(
  fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  ...
)
```

Esta anotación fuerza a Jackson a leer **los campos directamente** en lugar de llamar a los getters. El problema es que los proxies de Hibernate **no inicializan los campos** — solo se inicializan al llamar al getter. Resultado: Jackson lee los campos antes de que el proxy los rellene → todo `null`.

La anotación se había añadido para que `@JsonProperty(WRITE_ONLY)` sobre `password` ocultara la contraseña en las respuestas. Pero esa anotación funciona perfectamente sin `@JsonAutoDetect` porque Jackson, por defecto, mezcla las anotaciones de campo con el getter al introspeccionar la propiedad.

**Solución:**

1. Quitar `@JsonAutoDetect` de `Usuario.java`. La ocultación de `password` sigue funcionando con la `@JsonProperty(access = WRITE_ONLY)` que ya estaba en el campo.
2. Añadir `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` en todas las entidades que se serialicen (`Usuario`, `Album`, `Artista`, `Resena`, `Favorito`) para evitar que esos campos internos del proxy se cuelen en el JSON.

**Verificación:** la siguiente respuesta del POST tenía `usuario.username: "maria_indie"`, `usuario.email`, etc., todos rellenos, y sin basura de Hibernate.

### Bug B3 — `refresh()` antes del flush descartaba los cambios en `actualizar()`

**Síntoma:** `PUT /api/resenas/{id}` devolvía siempre los valores **antiguos** de la reseña, no los nuevos. Si la reseña tenía `puntuacion=4.0` y se hacía PUT con `puntuacion=5.0`, la respuesta seguía mostrando 4.0 (aunque la BD luego acababa con 5.0 al final del request).

**Causa raíz:** el método `ResenaService.actualizar` tenía:

```java
resena.setPuntuacion(datosActualizados.getPuntuacion());
resena.setComentario(datosActualizados.getComentario());

resenaRepository.save(resena);
entityManager.refresh(resena);   // ← problema aquí
return resena;
```

`refresh()` recarga la entidad desde la BD descartando los cambios en memoria. Pero `save()` **no fuerza el flush** — los cambios solo se persisten al hacer commit de la transacción, al final del método. Así que el orden temporal real es:

1. `setPuntuacion(5.0)` — en memoria, no en BD.
2. `save()` — Hibernate lo agenda pero no lo persiste todavía.
3. `refresh()` — lee la BD (que sigue con 4.0) y **sobrescribe** los cambios en memoria.
4. `return resena` — con 4.0.
5. Commit — el dirty checking ve que `resena` tiene 4.0 (igual que en BD) y no genera UPDATE.

El refresh tenía sentido en `crear()` porque ahí la entidad se persiste con `INSERT` inmediato (por la estrategia `IDENTITY` que necesita el id auto-generado para devolverlo) y `refresh` convierte los stubs `{id:5}` del request body en proxies gestionados. En `actualizar()` la entidad ya viene gestionada de `findById()` y `refresh` solo estorba.

**Solución:** quitar el `refresh()` en `actualizar()`. El dirty checking de Hibernate al commit ya genera el UPDATE; el objeto en memoria se devuelve con los valores nuevos:

```java
return resenaRepository.save(resena);
```

**Verificación:** `PUT` ahora devuelve los valores nuevos y `fechaEdicion` se rellena correctamente (la lleva el `@PreUpdate`).

### Bug B4 — Login/register devolvían `text/plain` en errores → frontend no podía parsear

**Síntoma:** al probar el `Login.jsx` con un password incorrecto, en lugar de mostrar "Email o contraseña incorrectos" salía un error técnico de JSON parsing.

**Diagnóstico:** `curl -i -X POST /api/auth/login` con credenciales malas devolvía:

```
HTTP/1.1 401
Content-Type: text/plain;charset=UTF-8

Email o contraseña incorrectos
```

Pero el frontend hace:

```js
const data = await res.json();   // ← falla porque el body no es JSON
```

El `res.json()` peta con `SyntaxError: Unexpected token E in JSON at position 0`, el `catch` lo atrapa y muestra ese mensaje técnico al usuario.

**Causa raíz:** `AuthController.java` devolvía manualmente texto plano:

```java
return ResponseEntity.status(401).body("Email o contraseña incorrectos");
```

mientras que el resto de la API usa el `GlobalExceptionHandler` para devolver siempre JSON uniforme con `{status, mensaje, timestamp}`.

**Solución:** que `AuthController` lance excepciones (`ReglaNegocioException`) en vez de devolver bodies manuales. El `GlobalExceptionHandler` las convierte a JSON automáticamente:

```java
if (usuario == null || !passwordEncoder.matches(...)) {
    throw new ReglaNegocioException("Email o contraseña incorrectos");
}
```

Cambia el código HTTP de 401 a 400 (porque `ReglaNegocioException` se mapea a 400), pero eso no es un problema funcional: el frontend no diferenciaba entre 400 y 401 para errores de login, solo lee el campo `mensaje`. Y semánticamente "credenciales mal" está más cerca de "petición incorrecta" que de "no autenticado".

**Verificación:**

```
$ curl -i -X POST /api/auth/login -d '{"email":"x@x.com","password":"WRONG"}'
HTTP/1.1 400
Content-Type: application/json

{"mensaje":"Email o contraseña incorrectos","status":400,"timestamp":"..."}
```

### Bug B5 — Test unitario `ResenaServiceTest.crear_*` roto desde la sesión anterior

**Síntoma:** al ejecutar `./mvnw test` después de los arreglos anteriores, fallaba un único test:

```
ResenaServiceTest.crear_conDatosValidos_guardaYDevuelveResena
NullPointerException: Cannot invoke "EntityManager.refresh(Object)"
because "this.entityManager" is null
```

**Causa raíz:** en una sesión anterior se había añadido el campo `private final EntityManager entityManager` a `ResenaService` (para el `refresh()` documentado como Bug 2/3 en `pruebas_postman.md`), pero **no se actualizó el test** que usa `@InjectMocks`. Mockito necesita un `@Mock EntityManager` para inyectar; sin él, el campo queda a `null`.

Este bug **ya estaba ahí** antes de empezar la fase 4, simplemente nadie había vuelto a correr los tests.

**Solución:** añadir el mock al test:

```java
@Mock
private EntityManager entityManager;
```

Mockito lo inyecta automáticamente en `ResenaService` por tipo. El test no necesita stubbear `entityManager.refresh(...)` porque es un método `void` y Mockito hace nothing por defecto.

**Verificación:** `./mvnw test` pasa los 38 tests sin errores.

---

## 3. Pruebas del backend durante la integración

Las pruebas se hicieron en dos planos complementarios:

- **Diagnóstico con `curl`** — para localizar el origen del bug B1 (el 401 fantasma) y confirmar formato de respuestas durante los arreglos.
- **Postman manual** — 6 lotes con el flujo de auth y CRUD completo, pensados para reproducir lo que hará el frontend.
- **Tests unitarios JUnit + Mockito** — `./mvnw test`, los 38 ya existentes en el proyecto, para asegurar que los cambios no rompen la lógica de servicio.

### 3.1 Diagnóstico con `curl` — proceso de "¿por qué da 401?"

Antes de tocar nada se reprodujo el problema en consola para entenderlo:

```bash
# Login funciona
$ curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@musicreviews.com","password":"maria123"}'
{"token":"eyJ...","id":5,"username":"maria_indie","email":"...","rol":"USER"}

# Pero POST con ese mismo token devuelve 401
$ TOKEN="eyJ..."
$ curl -s -i -X POST http://localhost:8080/api/resenas \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"usuario":{"id":5},"album":{"id":373},"puntuacion":4.0}'
HTTP/1.1 401
```

Como el 401 con un token válido era extraño, se reinició el backend redirigiendo logs a fichero:

```bash
$ ./mvnw spring-boot:run > backend.log 2>&1 &
$ # (...reproducir la petición...)
$ tail -10 backend.log
HttpMessageNotWritableException: Could not write JSON:
Could not initialize proxy [com.musicreviews.backend.model.Album#373] - no session
```

El log mostraba que **el problema no era de auth sino de serialización**. El 401 era una traducción engañosa de Spring Security 7. Con esa pista se identificó el bug B1.

Después del arreglo, otra ronda de `curl` confirmó las respuestas:

```bash
$ curl -s -X POST .../api/resenas ... | python -m json.tool
{
  "id": 53,
  "puntuacion": 4.0,
  "usuario": {
    "id": 5, "username": "maria_indie", "email": "maria@musicreviews.com",
    "rol": "USER", "activo": true,
    ...
  },
  "album": {
    "id": 16, "titulo": "Starina", ...,
    "artista": { "id": 10, "nombre": "Rojuu", "biografia": "...", ... }
  },
  ...
}
```

Las relaciones `usuario` y `album` (e incluso `album.artista` anidado) se serializaban completas. Sin basura `hibernateLazyInitializer`. Sin `password`. El bug B2 también estaba arreglado.

### 3.2 Suite de pruebas Postman (6 lotes)

Pensadas para hacerse en orden, porque cada lote depende de las variables que rellena el anterior.

#### Lote 1 — Login y captura del token

`POST http://localhost:8080/api/auth/login` con body:

```json
{ "email": "maria@musicreviews.com", "password": "maria123" }
```

En la pestaña **Scripts → Post-res** del request:

```js
pm.environment.set("token", pm.response.json().token);
```

Esto guarda el token en una variable de entorno de Postman llamada `{{token}}`. El resto de peticiones la usan para autenticarse.

**Resultado:** `200` con `{token: "eyJ...", id: 5, username: "maria_indie", email: "maria@musicreviews.com", rol: "USER"}`. La variable `token` queda rellena (icono del ojo arriba a la derecha de Postman) — confirma que el script post-response funcionó.

> **Aprendizaje:** en una primera prueba, el `token` se quedaba a `null` porque el script post-response estaba en la petición *de reseñas* en vez de en la *de login*. Al hacer POST a `/api/resenas`, el header `Authorization: Bearer null` provocaba que el backend rechazara el JWT y devolviera 401. Ese 401 fue el primer síntoma reportado y desencadenó toda la investigación.

#### Lote 2 — POST de reseña con relaciones pobladas (verifica B1 y B2)

`POST /api/resenas` con `Authorization: Bearer {{token}}` y body:

```json
{
  "usuario": { "id": 5 },
  "album":   { "id": 16 },
  "puntuacion": 4.0,
  "comentario": "Gran álbum"
}
```

**Resultado real obtenido tras los arreglos:**

```json
{
  "id": 54,
  "puntuacion": 4.0,
  "comentario": "Gran álbum",
  "fechaCreacion": "2026-04-28T19:18:44",
  "fechaEdicion": null,
  "usuario": {
    "id": 5,
    "username": "maria_indie",
    "email": "maria@musicreviews.com",
    "rol": "USER",
    "activo": true,
    "bio": null,
    "fotoPerfil": null,
    "fechaRegistro": "2026-04-20T11:42:49",
    "fechaUltimoLogin": "2026-04-28T19:17:39.591415"
  },
  "album": {
    "id": 16,
    "titulo": "Starina",
    "fechaLanzamiento": "2022-05-20",
    "genero": "trap",
    "portada": "https://i.scdn.co/image/...",
    "descripcion": null,
    "artista": {
      "id": 10,
      "nombre": "Rojuu",
      "biografia": "Roc Jou Morales better known as Rojuu...",
      "genero": "trap",
      "pais": "España",
      "foto": "https://i.scdn.co/image/..."
    }
  }
}
```

Lo que se valida en esta respuesta:

| Verificación | Cumplido |
|---|---|
| `usuario` con todos los campos no nulos (id, username, email, rol) | ✅ |
| `album` con campos básicos (id, titulo, género, fecha) | ✅ |
| `album.artista` anidado con biografía y nombre | ✅ |
| Sin `hibernateLazyInitializer`, `$$_hibernate_interceptor` en el JSON | ✅ |
| Sin `password` filtrado | ✅ |
| `fechaCreacion` rellena por `@PrePersist`, `fechaEdicion` aún null | ✅ |

Los `null` que aparecen en `usuario.bio`, `usuario.fotoPerfil` y `album.descripcion` son **legítimos**: son campos opcionales sin valor en la BD para esos registros, no fallos de carga.

#### Lote 3 — Casos de error de POST (verifica seguridad y validaciones)

Mantengo el mismo POST de arriba y voy cambiando una cosa cada vez.

| # | Caso | Cambio | Status | Body de respuesta |
|---|---|---|---|---|
| 3.1 | Sin token | Authorization → No Auth | **401** | (vacío, header `WWW-Authenticate`) |
| 3.2 | Reseña duplicada | `album.id` ya reseñado por maría (373) | **400** | `{"status":400,"mensaje":"El usuario ya ha reseñado este álbum","timestamp":"..."}` |
| 3.3 | Puntuación 7 | `puntuacion: 7` | **400** | `{"status":400,"mensaje":"La puntuación debe estar entre 0.5 y 5",...}` |
| 3.4 | Puntuación 0 | `puntuacion: 0` | **400** | mismo mensaje que 3.3 |
| 3.5 | Token corrupto | Bearer `xxx.yyy.zzz` | **401** | (vacío) |

Lo que se valida:

- El `JwtFilter` rechaza tokens malformados (3.5).
- `SecurityConfig` exige autenticación para POST (3.1).
- `ResenaService.crear` valida puntuación antes de tocar BD (3.3, 3.4).
- `ResenaService.crear` detecta duplicados con `existsByUsuarioIdAndAlbumId` (3.2).
- El `GlobalExceptionHandler` traduce las `ReglaNegocioException` a JSON uniforme con `mensaje` legible.

#### Lote 4 — PUT y DELETE de reseña (verifica B3)

Con la reseña creada en el lote 2 (id 54).

| # | Petición | Status | Verificación |
|---|---|---|---|
| 4.1 | `PUT /api/resenas/54` con `{"puntuacion":5.0,"comentario":"Disco perfecto"}` | **200** | Body devuelve `puntuacion: 5.0` (no 4.0), `comentario: "Disco perfecto"`, `fechaEdicion` con timestamp actual (ya no null). |
| 4.2 | `PUT /api/resenas/54` con `puntuacion: 10` | **400** | Mensaje de puntuación inválida. La reseña **no** se modifica. |
| 4.3 | `PUT /api/resenas/99999` con body válido | **404** | `"Reseña no encontrada"` |
| 4.4 | `DELETE /api/resenas/54` | **204** | Sin body |
| 4.5 | `DELETE /api/resenas/54` (repetido) | **404** | `"Reseña no encontrada"` |

El test 4.1 es el que valida directamente que el bug B3 está arreglado: antes el PUT respondía con los valores antiguos, ahora con los nuevos.

#### Lote 5 — Favoritos (verifica B1 también en la otra entidad LAZY)

| # | Petición | Status | Verificación |
|---|---|---|---|
| 5.1 | `POST /api/favoritos` con `{usuario:{id:5}, album:{id:16}}` | **200** | `usuario` y `album` (con `artista` anidado) poblados, `fechaAgregado` rellena. |
| 5.2 | Mismo POST repetido | **400** | `"El álbum ya está en favoritos"` |
| 5.3 | `GET /api/favoritos/existe?usuarioId=5&albumId=16` | **200** | Body: `true`. Con `albumId=999` → `false`. |
| 5.4 | `DELETE /api/favoritos?usuarioId=5&albumId=16` | **204** | Sin body |
| 5.5 | `DELETE` repetido | **404** | `"El álbum no está en favoritos"` |
| 5.6 | `GET /api/favoritos?usuarioId=5` | **200** | Array con todos los favoritos de maría — verificado: 6 elementos, cada uno con `usuario` y `album.artista` totalmente poblados. |

Esto confirma que la solución del B1 se aplica de forma uniforme: cualquier entidad con `@ManyToOne(LAZY)` se serializa correctamente ahora.

#### Lote 6 — Verificación del bug B4 (login con error en JSON)

Después de arreglar `AuthController` para lanzar `ReglaNegocioException`:

```bash
$ curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@musicreviews.com","password":"WRONG"}'

HTTP/1.1 400
Content-Type: application/json
{"status":400,"mensaje":"Email o contraseña incorrectos","timestamp":"..."}
```

**Antes** del arreglo: status 401 con `Content-Type: text/plain` y body `Email o contraseña incorrectos`.
**Después**: 400 con JSON uniforme. El frontend ya puede hacer `res.json()` sin que reviente.

### 3.3 Tests unitarios — `./mvnw test`

El proyecto tiene 38 tests JUnit + Mockito en `src/test/java/.../service/`. Se ejecutaron tras cada arreglo del backend para asegurar que no se rompía nada de la lógica existente.

| Clase de test | # tests | Qué cubre |
|---|---|---|
| `ArtistaServiceTest` | 7 | CRUD básico, búsqueda por nombre, manejo de id inexistente |
| `AlbumServiceTest` | 11 | Búsquedas paginadas (por título, género, artista), borrado, errores |
| `EstadisticasServiceTest` | 5 | `resumen()`, `topAlbumes()`, `topArtistas()`, etc. con repos mockeados |
| `FavoritoServiceTest` | 7 | Add/remove, detección de duplicados, validación de id |
| `ResenaServiceTest` | 11 | Validación de puntuación, duplicados, CRUD, `findByUsuarioYAlbum` |
| `UsuarioServiceTest` | 7 | Email/username únicos, actualizar último login, borrado, no encontrado |
| **Total** | **38** | — |

**Estado al cerrar la sesión:** los 38 verdes.

```
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Durante la sesión hubo **un test rojo** (`ResenaServiceTest.crear_conDatosValidos_guardaYDevuelveResena`) por el bug B5 documentado más arriba. Se arregló añadiendo `@Mock EntityManager entityManager` en el test.

### 3.4 ¿Por qué tantas formas de probar?

Cada nivel atrapa cosas distintas:

| Nivel | Atrapa |
|---|---|
| Tests unitarios con Mockito | Lógica de servicio aislada (puntuación, duplicados, throws) |
| `curl` directo | Diagnóstico bruto, ver headers, ver respuestas crudas, automatizar |
| Postman manual | Flujos de varios pasos con variables (login → token → POST resena) |
| Frontend real | Integración end-to-end, encoding UTF-8, CORS, manejo de respuestas no-200 |

Los bugs B1, B2, B3 los habrían atrapado tests de integración con `@SpringBootTest` + `MockMvc` (que serializaran respuestas reales), pero el proyecto no tiene de esos. Por eso pasaron desapercibidos hasta que se hicieron las pruebas con Postman desde la perspectiva de "voy a consumir esto desde el frontend".

El bug B4 lo habría atrapado un test de integración del controller que verificara `Content-Type: application/json` en respuestas de error, o el primer intento del frontend de hacer `res.json()` sobre una respuesta 401.

Como aprendizaje para el resto del TFG: **a partir del paso 3 las llamadas reales del frontend funcionan como tests de integración informales**. Cualquier comportamiento raro en el navegador que no esté en los tests unitarios apunta probablemente a un bug del mismo tipo que estos cinco.

---

## 4. Paso 3 — Navbar dinámico

### El problema que resuelve

Hasta ahora el navbar era estático: siempre mostraba "Entrar" y "Registrarse" y un avatar hardcoded con la letra "P" enlazado a `/perfil/pablo_music`. Daba igual si el usuario había hecho login o no, la barra se veía idéntica. Eso es incoherente:

- Si estoy logueado, no tiene sentido que me siga ofreciendo "Entrar".
- Si no estoy logueado, no debería ver el icono ♥ de favoritos (es una página privada) ni un avatar inventado.
- No había botón de cerrar sesión, lo que hacía imposible salir desde la UI.

El paso 3 es **el primer sitio fuera de los formularios de auth donde se consume el contexto**. Hasta ahora `AuthContext` solo se usaba en `Login.jsx` y `Registro.jsx` para escribir. Ahora el `Navbar` lo lee y reacciona.

### Cómo lee el contexto

```jsx
import { useAuth } from "../../context/AuthContext";

export default function Navbar() {
  const { usuario, logout } = useAuth();
  // ...
}
```

`useAuth()` devuelve el objeto que el `<AuthContext.Provider>` puso en la "caja" desde `main.jsx`. Como `AuthProvider` envuelve a toda la app, **cualquier componente** puede llamar a `useAuth()` sin recibir nada por props. React además se encarga de re-renderizar el navbar automáticamente cuando `usuario` cambia (al hacer login o logout).

### Renderizado condicional

El navbar tiene dos variantes según haya o no sesión. Se implementa con un único operador ternario sobre `usuario`:

```jsx
{usuario ? (
  <>
    {/* Sesión activa: favoritos, avatar, salir */}
  </>
) : (
  <>
    {/* Sin sesión: entrar, registrarse */}
  </>
)}
```

Esto funciona porque `usuario` es `null` cuando no hay sesión (falsy) y un objeto cuando sí (truthy). Los `<>...</>` (Fragment) son necesarios porque cada rama tiene varios hermanos y el ternario solo puede devolver un nodo.

| Elemento | Sin sesión | Con sesión |
|---|---|---|
| Logo, links Inicio / Explorar / Top Álbumes | ✅ | ✅ |
| Buscador (icono lupa) | ✅ | ✅ |
| Botones "Entrar" / "Registrarse" | ✅ | ❌ |
| Icono ♥ Favoritos | ❌ | ✅ |
| Avatar circular con inicial del usuario | ❌ | ✅ |
| Botón "Salir" | ❌ | ✅ |
| Link "Admin" | ❌ | Solo si `rol === "ADMIN"` |

### El logout

```jsx
import { useNavigate } from "react-router-dom";

const navigate = useNavigate();

function handleLogout() {
  logout();         // limpia estado React + localStorage (vía AuthContext)
  navigate("/");    // lleva al home
}
```

Dos cosas a destacar:

1. **`navigate("/")` es importante.** Si el usuario hace logout estando en `/favoritos`, sin la redirección se quedaría en esa página viendo lo que tenía cacheado (o un error si la página exige sesión, lo veremos en el paso 4). Devolverlo al home es seguro y predecible.

2. **`<button onClick={handleLogout}>` y NO `<Link>`.** Logout es una **acción**, no navegación. Usar `<Link>` confundiría al usuario y a los lectores de pantalla, que anuncian "enlace a..." cuando es un botón el que está activando algo.

### El avatar dinámico

Antes el navbar tenía:

```jsx
<Link to="/perfil/pablo_music">
  <span>P</span>
</Link>
```

Hardcoded a un perfil que ni siquiera existía en la BD ("pablo_music"). Ahora:

```jsx
<Link to={`/perfil/${usuario.username}`}>
  <span>{usuario.username.charAt(0).toUpperCase()}</span>
</Link>
```

- `usuario.username` viene del backend al hacer login. Si maría inicia sesión, será `maria_indie`.
- `charAt(0).toUpperCase()` saca la primera letra y la pone en mayúscula. Para `maria_indie` queda **M**.
- La URL del perfil es la real del usuario, así que el botón apunta a `/perfil/maria_indie` y eso enlaza con la página de perfil que en pasos posteriores cargará sus reseñas y favoritos.

### El link Admin condicional

```jsx
{usuario?.rol === "ADMIN" && (
  <Link to="/admin">Admin</Link>
)}
```

`usuario?.rol` usa optional chaining: si `usuario` es null, `null?.rol` es `undefined`, que no es igual a `"ADMIN"` → el bloque entero se evalúa a `false` y React no renderiza nada. Equivalente a:

```js
(usuario === null ? undefined : usuario.rol) === "ADMIN"
```

pero mucho más corto. Sin optional chaining, `usuario.rol` cuando `usuario` es `null` lanzaría un `TypeError`.

### Detalles pequeños arreglados de paso

- **Logo ahora es `<Link to="/">`.** Antes era un `<div>` no clicable. Es convención que el logo lleve al inicio.
- **`aria-label` en los iconos sin texto** (lupa, corazón, avatar). Los lectores de pantalla anuncian el atributo en lugar de "imagen sin descripción". Accesibilidad básica.
- **`onClick={handleLogout}` en lugar de `onClick={() => { logout(); navigate("/"); }}`** inline. Función nombrada porque hace dos cosas y el inline pierde legibilidad rápido.

### Verificación

| Paso | Esperado | Cumplido |
|---|---|---|
| Cargar `localhost:5173` sin sesión | "Entrar" y "Registrarse" visibles, sin ♥ ni avatar | ✅ |
| Login con maría → ir a `/` | Avatar "M" en verde, ♥ visible, "Salir" visible, sin botones de auth | ✅ |
| Refrescar la página estando logueado | Sigue logueado (gracias al localStorage del AuthContext) | ✅ |
| Click en avatar | Lleva a `/perfil/maria_indie` | ✅ |
| Click en "Salir" | Limpia sesión, redirige a `/`, vuelven los botones "Entrar"/"Registrarse" | ✅ |
| Login con un usuario rol ADMIN | Aparece link "Admin" en la navbar | (pendiente — no hay usuario admin en la BD de prueba todavía) |

### Lo que NO está hecho aún (a propósito)

- **`/favoritos` sigue siendo accesible sin sesión** escribiendo la URL a mano. El navbar la oculta pero la ruta no está protegida. Eso es el **paso 4**.
- El avatar es solo la inicial. Cuando un usuario suba foto de perfil habrá que mostrarla. Eso es el **paso 7** (páginas de usuario).
- El link "Admin" se muestra correctamente, pero la página `/admin` aún no comprueba el rol. Lo arreglará el paso 4 con una protección por rol.

Cada cosa en su paso para no mezclar responsabilidades.

---

## 5. Bug del frontend descubierto al probar el flujo real (B6)

Al verificar manualmente el navbar dinámico, el primer intento de login con maría desde la UI (`localhost:5173`) devolvía sistemáticamente **400 "Email o contraseña incorrectos"**. Las mismas credenciales funcionaban perfectamente desde `curl` y desde Postman. Algo se estaba perdiendo entre el formulario y el backend.

### Diagnóstico

Pasos seguidos para localizarlo:

1. Confirmar que el backend funciona: `curl -X POST /api/auth/login` con las credenciales → 200 + token. ✅
2. Verificar que CORS no bloquea: la petición llega al backend (devuelve 400, no falla con `Failed to fetch`). ✅
3. Mirar la **Network → Payload** en F12. Ahí se vería que el body real era `{"email":"","password":""}` — strings vacíos.

Es decir, el formulario enviaba campos vacíos aunque en pantalla se viera el texto introducido.

### Causa raíz

El componente `FormInput.jsx` tenía esta firma:

```jsx
export default function FormInput({ label, type, placeholder, id, error }) {
  return (
    <div>
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} placeholder={placeholder} className="..." />
    </div>
  );
}
```

**No declaraba `value` ni `onChange` como props** y tampoco los pasaba al `<input>`. Pero `Login.jsx` y `Registro.jsx` los usaban:

```jsx
<FormInput
  value={email}
  onChange={e => setEmail(e.target.value)}
  ...
/>
```

Esos props **se descartaban silenciosamente**. El `<input>` real no era controlado por React. Lo que ocurría:

1. El usuario teclea `maria@musicreviews.com` → el navegador muestra el texto en el campo (DOM).
2. React no se entera de nada porque no hay `onChange` conectado.
3. El estado `email` en `Login.jsx` sigue siendo `""`.
4. Submit → `login("", "")` → backend recibe `{"email":"","password":""}` → 400.

El bug pasaba desapercibido al ver la maquetación: las letras aparecen, los validadores HTML5 (`type="email"`) funcionan, todo se ve bien. Solo se manifiesta al hacer submit real.

### Solución

Que `FormInput` propague al `<input>` cualquier prop estándar mediante el operador rest:

```jsx
export default function FormInput({ label, type = "text", placeholder, id, error = false, ...rest }) {
  return (
    <div>
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        type={type}
        placeholder={placeholder}
        className="..."
        {...rest}    // ← propaga value, onChange, name, autoComplete, required, etc.
      />
    </div>
  );
}
```

`{...rest}` incluye automáticamente `value` y `onChange` cuando los pasen, pero también `name`, `autoComplete="email"`, `required`, etc. Esto es la convención estándar para componentes que envuelven elementos HTML — de hecho cualquier librería de UI hace esto.

### Verificación

Tras el fix, recargar el navegador (HMR de Vite ya había aplicado el cambio) y probar el login: maría entró a la primera, el navbar cambió a la versión "con sesión" como se esperaba.

### Lo que se aprendió

- **Un componente que envuelve un elemento HTML estándar (`input`, `button`, `select`, ...) debe usar `{...rest}` para propagar props desconocidos.** Es el patrón que usa cualquier librería seria (MUI, Chakra, Radix). Si solo declaras los props que crees que vas a usar, te encontrarás con esto en cuanto alguien intente añadir un atributo nuevo.
- **Las pruebas visuales no son pruebas funcionales.** El formulario "se veía bien" — letras aparecían, validadores HTML5 reaccionaban — pero no enviaba lo que el usuario tecleaba. Solo se descubrió al hacer un flujo de extremo a extremo.
- **F12 → Network → Payload es la herramienta más útil** para aislar bugs frontend ↔ backend. Mirar lo que se envía, no lo que crees que se envía.

### Por qué se cuenta como B6

B1-B5 fueron bugs del **backend** detectados al integrar. B6 es del **frontend**, en un componente reutilizable que afectaba a Login, Registro y a cualquier futura página con `FormInput` (Editar perfil, etc.). Lo recogemos aquí para que la lista de bugs de la fase 4 esté completa.

---

## 6. Resumen de cambios durante esta sesión

### Backend

| Fichero | Cambio | Bug |
|---|---|---|
| `application.properties` | `spring.jpa.open-in-view: false → true` | B1 |
| `model/Usuario.java` | Quitar `@JsonAutoDetect`, añadir `@JsonIgnoreProperties` | B2 |
| `model/Album.java`, `Artista.java`, `Resena.java`, `Favorito.java` | Añadir `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` | B2 |
| `service/ResenaService.java` | Quitar `entityManager.refresh()` en `actualizar()` | B3 |
| `controller/AuthController.java` | Lanzar `ReglaNegocioException` en lugar de devolver texto plano | B4 |
| `test/.../ResenaServiceTest.java` | Añadir `@Mock EntityManager entityManager` | B5 |

**8 ficheros del backend, 38/38 tests verdes.**

### Frontend

| Fichero | Cambio | Paso / Bug |
|---|---|---|
| `services/auth.js` | Capa de red: `POST /api/auth/login` y `/register` | Paso 1 |
| `context/AuthContext.jsx` | Estado React de `usuario` y `token`, persistencia en localStorage, hook `useAuth()` | Paso 1 |
| `main.jsx` | Envolver `<App />` con `<AuthProvider>` | Paso 1 |
| `pages/Login.jsx`, `pages/Registro.jsx` | Conectados a `useAuth()` con manejo de error y estado de carga | Paso 2 |
| `components/layout/Navbar.jsx` | Renderizado condicional según `usuario`, logout, avatar real, link Admin condicional | Paso 3 |
| `components/ui/FormInput.jsx` | Propagar `{...rest}` al `<input>` para que value/onChange funcionen | B6 |
| **Limpieza** | Borrar `App.css`, `react.svg`, `vite.svg`, `SESSION_LOG.md`, 6 README desactualizados, carpeta `hooks/` vacía | — |

**6 ficheros tocados + 10 borrados de basura/docs antiguos.**

---

## 7. Estado al cerrar esta entrega

✅ **Paso 1 (AuthContext) completo.**
✅ **Paso 2 (Login + Registro funcionales contra el backend) completo** — incluye el fix de B6 (FormInput sin propagar `value`/`onChange`), descubierto al hacer el primer login real desde el navegador.
✅ **Paso 3 (Navbar dinámico) completo** — primer consumo de `useAuth()` fuera de los formularios de auth. Renderizado condicional sin/con sesión, logout funcional, avatar real con username del usuario, link Admin condicionado al rol.
✅ **Login end-to-end verificado en el navegador** (no solo Postman): maría inicia sesión desde el formulario, el navbar reacciona, la sesión persiste al refrescar y el logout funciona.
✅ **Backend con 5 bugs arreglados y todas las relaciones LAZY serializando correctamente.**
✅ **38/38 tests unitarios verdes.**
✅ **Postman documentado con flujo end-to-end del login + CRUD de reseñas y favoritos.**

🔜 **Siguiente: paso 4 (Rutas protegidas).** Hoy `/favoritos`, `/admin`, `/crear-resena`, `/editar-perfil` son accesibles aunque no haya sesión escribiendo la URL a mano — solo están "ocultas" en el navbar. Hay que crear un componente `<RutaProtegida>` que comprueba el contexto y redirige a `/login` si no hay usuario, y un `<RutaAdmin>` que además exija `rol === "ADMIN"`.

---

## Anexo — Por qué cada decisión

Esta sección recoge las justificaciones técnicas sueltas para tenerlas a mano en la defensa.

**¿Por qué un Context y no Redux/Zustand?**
La app no tiene estado global complejo. Solo necesita compartir `usuario` y `token`. Context resuelve eso en 50 líneas sin librerías extra. Redux sería *over-engineering* para un TFG.

**¿Por qué localStorage y no cookies?**
El backend devuelve un JWT en el body, no como cookie. Para que el frontend lo guarde y lo mande en el header `Authorization: Bearer ...`, lo más simple es localStorage. Las cookies HttpOnly serían más seguras contra XSS pero requieren cambiar el backend para que las setee y que el frontend las mande automáticamente — más complejo.

**¿Por qué `fetch` y no Axios?**
`fetch` es nativo del navegador desde hace años. La app tiene 2 endpoints de auth y unos pocos de datos: una librería de 30KB extra no se justifica. Si en el futuro hace falta interceptores (renovar token automáticamente, por ejemplo), se reevalúa.

**¿Por qué guardar `usuario` en localStorage en vez de derivarlo del JWT?**
El JWT contiene `email` y `rol`, pero no `id` ni `username`. Para mostrar el username en el navbar sin tener que llamar al backend al cargar la página, lo más simple es guardarlo aparte. Coste: 100 bytes en localStorage.

**¿Por qué el `error` en `services/auth.js` lanza `Error` en lugar de devolver un objeto?**
Para que el componente que llama use `try/catch` (idiomático en JS async) en lugar de `if (result.error)`. También así se propaga limpiamente por la cadena `loginService → AuthContext.login → handleSubmit`.
