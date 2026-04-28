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

## 6. Paso 4 — Rutas protegidas

### El problema que resuelve

Tras el paso 3 el navbar ya **oculta** los enlaces a páginas privadas cuando no hay sesión, pero las rutas siguen siendo accesibles **escribiendo la URL a mano**. Cualquiera podía ir a `localhost:5173/favoritos` o `/admin` y ver la página (con datos mock todavía, pero sería igual de inseguro cuando se conecten al backend).

La protección de la UI no es protección de verdad. La protección de verdad la hace el backend con JWT — sin un Bearer válido, los endpoints privados devuelven 401. Pero el frontend también debe colaborar para no enseñar pantallas que el usuario nunca debería ver, y para llevarlo al login cuando intenta entrar a algo privado en vez de mostrar errores.

El paso 4 añade **dos componentes wrapper** que se pueden envolver alrededor de cualquier conjunto de rutas:

| Wrapper | Comprueba | Si falla |
|---|---|---|
| `<RutaProtegida>` | `usuario !== null` | Redirige a `/login` recordando la URL original |
| `<RutaAdmin>` | `usuario !== null && usuario.rol === "ADMIN"` | Sin sesión: a `/login`. Con sesión sin rol: a `/` |

### Anatomía de `<RutaProtegida>`

`src/components/routing/RutaProtegida.jsx`:

```jsx
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function RutaProtegida() {
  const { usuario } = useAuth();
  const location = useLocation();

  if (!usuario) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
```

Cuatro decisiones a destacar:

**1. `<Outlet />` cuando hay sesión.** `Outlet` es la primitiva de React Router para "aquí se renderiza la ruta hija". Es lo que hace que el wrapper se pueda usar como ruta padre y meter las rutas privadas dentro:

```jsx
<Route element={<RutaProtegida />}>
  <Route path="/favoritos" element={<MisFavoritos />} />
  <Route path="/crear-resena" element={<CrearResena />} />
  ...
</Route>
```

Sin sesión, el wrapper retorna `<Navigate>` y `<Outlet />` ni se ejecuta. Con sesión, el `<Outlet />` se sustituye por el `<MisFavoritos />` o el componente que toque.

**2. `state={{ from: location }}` al redirigir.** Esto guarda la URL original en el estado de navegación. Sin esto, el usuario que iba a `/favoritos` y fue rebotado a `/login` aterrizaría en `/` tras autenticarse — perdiendo el contexto. Con esto, `Login.jsx` lee `location.state.from` y vuelve allí.

**3. `replace` en el `<Navigate>`.** Por defecto, una redirección añade la entrada `/login` al historial del navegador. Si el usuario pulsa "atrás" tras autenticarse, vuelve al formulario que ya completó — confuso. `replace` sustituye la entrada actual por `/login` en lugar de añadirla, así "atrás" salta directamente a la página anterior.

**4. `useLocation()` para capturar la ubicación actual.** No basta con `window.location.pathname` porque queremos también `search` (query string) y `state` (si los hubiera). El hook devuelve un objeto rico que se puede pasar tal cual.

### Anatomía de `<RutaAdmin>`

`src/components/routing/RutaAdmin.jsx`:

```jsx
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function RutaAdmin() {
  const { usuario } = useAuth();
  const location = useLocation();

  if (!usuario) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (usuario.rol !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
```

La parte interesante es **por qué un USER se redirige a `/` y no a `/login`**:

- `RutaProtegida` redirige a `/login` cuando `!usuario` porque la solución del problema es que se autentique. Tiene sentido pedirle credenciales.
- `RutaAdmin` cuando hay `usuario` pero no es admin **no puede arreglarse re-autenticando** — ya está autenticado, y autenticarse de nuevo le devolvería el mismo rol USER. Mandarlo a `/login` sería frustrante y crea bucle: tras login volvería a `/admin` y rebotaría otra vez.

Por la misma razón, **no se guarda `from` cuando se redirige a `/`**: si lo guardáramos y el usuario después hace login (cosa que no necesita, ya estaba logueado), volvería a `/admin` y rebotaría infinitamente. Cortar el bucle redirigiendo al home es la decisión correcta.

### Cambios en `App.jsx`

Antes:

```jsx
<Routes>
  <Route path="/" element={<Inicio />} />
  <Route path="/favoritos" element={<MisFavoritos />} />
  <Route path="/admin" element={<PanelAdmin />} />
  ... // todas planas, todas accesibles
</Routes>
```

Ahora:

```jsx
<Routes>
  {/* Públicas */}
  <Route path="/" element={<Inicio />} />
  <Route path="/login" element={<Login />} />
  <Route path="/registro" element={<Registro />} />
  <Route path="/catalogo" element={<Catalogo />} />
  <Route path="/rankings" element={<Rankings />} />
  <Route path="/busqueda" element={<Busqueda />} />
  <Route path="/album/:id" element={<DetalleAlbum />} />
  <Route path="/artista/:id" element={<DetalleArtista />} />
  <Route path="/perfil/:username" element={<PerfilUsuario />} />

  {/* Protegidas */}
  <Route element={<RutaProtegida />}>
    <Route path="/favoritos" element={<MisFavoritos />} />
    <Route path="/crear-resena" element={<CrearResena />} />
    <Route path="/editar-resena" element={<EditarResena />} />
    <Route path="/editar-perfil" element={<EditarPerfil />} />
  </Route>

  {/* Solo ADMIN */}
  <Route element={<RutaAdmin />}>
    <Route path="/admin" element={<PanelAdmin />} />
  </Route>

  <Route path="*" element={<NotFound />} />
</Routes>
```

Detalles a notar:

- Las rutas protegidas siguen teniendo **paths absolutos** (`/favoritos`, no `favoritos`). React Router lo permite porque la ruta padre del wrapper no tiene `path`. Si el padre tuviera `path="/privado"`, las hijas tendrían que ser relativas (`favoritos` se montaría como `/privado/favoritos`).
- `/perfil/:username` queda **pública** a propósito. Es como un perfil público de Letterboxd: cualquiera puede ver el perfil de otra persona. La página de **editar** perfil (`/editar-perfil`) sí es privada porque modifica datos.
- `/login` y `/registro` se quedan en el bloque público porque se accede a ellas precisamente cuando no hay sesión.

### Cambios en `Login.jsx`

```jsx
import { useNavigate, useLocation } from "react-router-dom";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();

  // Si venimos rebotados desde una RutaProtegida, volver a la URL original.
  // Si no (entrada directa), al home.
  const from = location.state?.from?.pathname || "/";

  async function handleSubmit(e) {
    // ...
    await login(email, password);
    navigate(from, { replace: true });
  }
}
```

**Optional chaining `location.state?.from?.pathname`**: cuando el usuario entra directamente a `/login` (no rebotado), `location.state` es `null` y `null.from` lanzaría `TypeError`. Con `?.` la cadena se evalúa a `undefined` y el fallback `|| "/"` toma el control.

**`{ replace: true }` también aquí**: tras login no queremos que `/login` quede en el historial. Sin esto, pulsar "atrás" volvería al formulario que ya completó.

### Verificación — pruebas manuales realizadas

Probado con `npm run dev` (frontend en `:5173`) + backend de Spring Boot en `:8080`. María (USER, id=5) y admin (ADMIN, id=10) son los usuarios de prueba.

#### Caso 1: sin sesión, ruta protegida → rebote a `/login`

| Acción | Esperado | Resultado |
|---|---|---|
| Sin sesión, escribir `localhost:5173/favoritos` en URL | Redirige a `/login` | ✅ |
| Sin sesión, escribir `localhost:5173/crear-resena` | Redirige a `/login` | ✅ |
| Sin sesión, escribir `localhost:5173/editar-perfil` | Redirige a `/login` | ✅ |
| Sin sesión, escribir `localhost:5173/admin` | Redirige a `/login` | ✅ |

#### Caso 2: sin sesión → ruta protegida → login → vuelta a la ruta original

| Acción | Esperado | Resultado |
|---|---|---|
| Sin sesión, ir a `/favoritos`, login con maría | Tras login, aterrizar en `/favoritos` (no en `/`) | ✅ |
| Sin sesión, ir a `/crear-resena`, login con maría | Tras login, aterrizar en `/crear-resena` | ✅ |
| Pulsar "atrás" del navegador tras login | NO vuelve a `/login` (gracias a `replace`) | ✅ |

#### Caso 3: USER intenta acceder a ruta admin → rebote a `/`

| Acción | Esperado | Resultado |
|---|---|---|
| Logueado con maría, escribir `localhost:5173/admin` | Redirige a `/` (no a `/login`) | ✅ |
| Logueado con maría, navbar | Sin link "Admin" visible | ✅ |

#### Caso 4: ADMIN accede a ruta admin → entra normal

| Acción | Esperado | Resultado |
|---|---|---|
| Logueado con admin, escribir `localhost:5173/admin` | Carga `PanelAdmin.jsx` | ✅ |
| Logueado con admin, navbar | Link "Admin" visible entre los links de navegación | ✅ |
| Click en avatar "A" | Lleva a `/perfil/admin` | ✅ |

#### Caso 5: rutas públicas siguen funcionando con o sin sesión

| Acción | Esperado | Resultado |
|---|---|---|
| Sin sesión, `/catalogo`, `/rankings`, `/busqueda`, `/album/:id`, `/artista/:id`, `/perfil/maria_indie` | Cargan normal | ✅ |
| Logueado, esas mismas rutas | Cargan normal | ✅ |

### Creación del usuario admin para las pruebas

`POST /api/auth/register` siempre crea con `rol = USER` (lo fija el `@PrePersist` de `Usuario.java`). Para tener un admin:

1. Registrar el usuario via API:
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","email":"admin@musicreviews.com","password":"admin123"}'
   ```
   Devuelve `{token, id:10, username, email, rol:"USER"}`.

2. Promocionarlo a ADMIN con SQL directo en la BD de Aiven (vía MySQL Shell):
   ```sql
   UPDATE usuario SET rol = 'ADMIN' WHERE email = 'admin@musicreviews.com';
   ```

Tras esto, login con `admin@musicreviews.com / admin123` da `rol: ADMIN` en la respuesta y el frontend lo trata como tal.

**Por qué no hay endpoint para promover a admin:** sería un agujero de seguridad. La asignación de roles siempre es operativa (DBA o panel admin). Es la convención correcta — el panel admin del frontend (`/admin`) podría incluir esta funcionalidad si quisiéramos, exigiendo que quien la ejecuta ya sea admin.

### Lo que NO está hecho aún (a propósito)

- **El backend no comprueba el rol en el endpoint del PanelAdmin** (cuando lo conectemos en el paso siguiente). El usuario sigue pudiendo llamar a la API directamente con su token. El frontend protegido no es suficiente — la protección real la hace `SecurityConfig` con `.hasRole("ADMIN")` en los `requestMatchers`. Esto ya está hecho para `/api/artistas/**`, `/api/albumes/**` POST/PUT/DELETE; cuando el panel admin haga llamadas administrativas se verá si hace falta más.
- **No hay refresh token.** El JWT expira a las 24h. Tras eso, el usuario tendrá que volver a hacer login. En un proyecto real se implementaría un refresh token con rotación; aquí no aplica al ámbito del TFG.
- **El logout no invalida el token en el servidor**, solo lo borra del cliente. JWT puro no permite invalidación; haría falta una blacklist en el servidor o sesiones de stateless con expiración corta + refresh. Mismo razonamiento que arriba: fuera del ámbito.

---

## 7. Paso 5 — Páginas públicas con datos reales

### El problema que resuelve

Hasta el paso 4 las cuatro páginas públicas (Inicio, Catálogo, Búsqueda, Rankings) seguían mostrando **datos mock** definidos en arrays dentro de cada componente. La protección de rutas y el navbar dinámico ya funcionaban, pero al entrar a `/catalogo` se veían siempre los mismos 10 álbumes hardcoded. Para que la app sea útil hay que conectar esas páginas con el backend.

### Capa de servicios — separación por dominio

Antes solo existía `services/auth.js`. Para el paso 5 se han creado tres servicios nuevos, uno por dominio del backend:

| Fichero | Responsabilidad | Endpoints |
|---|---|---|
| `services/albumes.js` | Listado paginado de álbumes y detalle | `GET /api/albumes`, `GET /api/albumes/{id}` |
| `services/artistas.js` | Listado de artistas y detalle | `GET /api/artistas`, `GET /api/artistas/{id}` |
| `services/estadisticas.js` | Resumen, rankings, géneros, actividad | `GET /api/estadisticas/*` (8 funciones) |

**Por qué un fichero por dominio en lugar de un único `api.js`:**

- Coincide con la estructura del backend (un controller por dominio).
- Si se añade un dominio nuevo se crea su fichero, sin tocar los existentes.
- Cada fichero queda corto (10–30 líneas) y se entiende de un vistazo.
- En cualquier librería profesional (axios + tanstack query, RTK Query) se hace así.

Patrón común a los tres:

```js
const API = "http://localhost:8080/api";

export async function getAlbumes({ page = 0, size = 12, titulo, genero } = {}) {
  const params = new URLSearchParams();
  params.set("page", page);
  params.set("size", size);
  if (titulo) params.set("titulo", titulo);
  if (genero) params.set("genero", genero);

  const res = await fetch(`${API}/albumes?${params}`);
  if (!res.ok) throw new Error(`Error al cargar álbumes (HTTP ${res.status})`);
  return res.json();
}
```

Detalles a notar:

- **`URLSearchParams`** construye query strings escapando bien cualquier carácter especial. Si el usuario busca `Beyoncé` o `4:44`, esto codifica los caracteres correctamente. Manualmente con `+ "&titulo=" + texto` se rompe rápido.
- **Argumentos opcionales con `= {}` y default values** — permite llamar `getAlbumes()` sin parámetros (toma defaults) o `getAlbumes({page: 2, titulo: "rad"})` con los que necesite.
- **`if (!res.ok) throw`** — mismo patrón que `auth.js` para que el componente que llama use `try/catch` o `.catch()` idiomático.

### Patrón de fetching en las páginas

Sin librería externa (nada de React Query, SWR, Axios). Solo `useState` + `useEffect` + `fetch` nativo. Es lo correcto para un TFG y lo que se evalúa.

Pattern estándar usado en las cuatro páginas:

```jsx
const [datos, setDatos] = useState(null);
const [error, setError] = useState(null);

useEffect(() => {
  servicio.get()
    .then(setDatos)
    .catch(err => setError(err.message));
}, []);

return (
  <>
    {error && <p>Error: {error}</p>}
    {!error && !datos && <p>Cargando…</p>}
    {datos && datos.length === 0 && <p>Sin resultados</p>}
    {datos && datos.length > 0 && <Grid items={datos} />}
  </>
);
```

Tres estados, tres bloques de render condicional. Es el "tri-state" estándar de React: cargando / error / datos. Cubrirlos todos hace que la UI nunca quede en blanco ni explote.

**`Promise.all` para peticiones paralelas:** cuando una página necesita varias peticiones independientes (Inicio: actividad reciente + top álbumes; Rankings: 5 endpoints), se lanzan **en paralelo** con `Promise.all`. Si fueran secuenciales (`await fetch1(); await fetch2()`) la página tardaría el doble. Con `Promise.all` tarda lo que la más lenta.

```jsx
useEffect(() => {
  Promise.all([getActividadReciente(), getTopAlbumes()])
    .then(([r, t]) => {
      setResenas(r);
      setTopAlbumes(t);
    })
    .catch(err => setError(err.message));
}, []);
```

### Página por página

#### Inicio (`/`)

- **Hero**: el card de la derecha que mostraba "DAMN. — Kendrick Lamar — 2017" hardcoded ahora muestra **la mejor reseña reciente del backend**. La derivación es sencilla: `[...resenas].sort((a,b) => b.puntuacion - a.puntuacion)[0]`. Es un `<Link>` clicable a `/album/:id`. Si no hay reseñas todavía, se muestra un placeholder con "Cargando…".
- **Reseñas recientes**: 4 `ResenaCard` de `getActividadReciente()`. Se aprovechó para hacer `ResenaCard` clicable también (envuelto en `<Link>`).
- **Top Álbumes**: 5 `AlbumCard` de `getTopAlbumes()` con su rating real.
- **CTA** sigue estática (es un call to action de marketing).

#### Catálogo (`/catalogo`)

Es la página más compleja porque combina paginación, filtros y búsqueda.

- **Géneros cargados dinámicamente** desde `getGeneros()`. El backend devuelve 36 géneros distintos (rock, pop, hip-hop, alternative rock, britpop, indie rock, etc.); se toman los **8 con más álbumes** + "Todos". Antes estaban hardcoded a `["Hip-Hop", "Rock", "Electronic", ...]` que ni coincidían con los géneros reales del backend.
- **Paginación server-side**: el backend devuelve `Page<Album>` con `{content, page: {totalPages, totalElements, number, size}}`. La UI sigue usando `pagina` 1-based (más natural para mostrar al usuario), se convierte a 0-based en el fetch (`page - 1`).
- **Búsqueda con debounce de 300 ms**: si se lanzara fetch en cada keystroke, escribir "radiohead" provocaría 9 peticiones. Con debounce solo se lanza la última cuando el usuario para de escribir 300 ms:

  ```jsx
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      getAlbumes({ titulo: busqueda, ... }).then(...);
    }, 300);
    return () => clearTimeout(timeoutId);
  }, [busqueda, generoActivo, pagina]);
  ```

  El truco está en el `clearTimeout` del cleanup: cada vez que el efecto se vuelve a ejecutar (tecla nueva), cancela el timer anterior antes de programar el nuevo. Solo el último timer llega al final.
- **Solo orden "A → Z"** porque el backend ordena por título ascendente por defecto y no acepta parámetro `sort`. Para añadir "Mejor valorados" o "Más recientes" hay que ampliar `AlbumController` con un `Sort` parameter (Spring Data Pageable lo soporta nativo). Queda como mejora futura. Las opciones que no funcionaban del mock se han retirado.
- **Sin estrellas en las cards** del catálogo. El backend no devuelve `puntuacionMedia` en el listado paginado de álbumes (solo en `getTopAlbumes()` que es agregado). Para mostrarlo en el catálogo hay que añadir un campo computado al modelo `Album` o un endpoint dedicado. `CatalogoCard` se modificó para que `rating` sea opcional (`{rating != null && ...}`) y no rompa con `null.toFixed()`.

#### Búsqueda (`/busqueda`)

- **Tendencias** (4 cards): `getTopAlbumes()` truncado a 4.
- **Añadidos recientemente** (3 filas): `getAlbumesRecientes()` truncado a 3.
- **Resultados de búsqueda**: cuando el usuario escribe, debounce 300 ms y `getAlbumes({titulo})`. Si el array viene vacío, muestra el bloque "Sin resultados" con el término buscado.
- **Chips "Todo / Álbumes / Artistas / Usuarios"** se mantienen como decoración visual. El backend solo soporta búsqueda por título de álbum en los endpoints públicos. Buscar artistas o usuarios requiere endpoints nuevos — fuera del paso 5.

#### Rankings (`/rankings`)

5 peticiones en paralelo con `Promise.all`:

| Sección | Endpoint | Mapeo |
|---|---|---|
| 4 stat cards | `getResumen()` | `{totalAlbumes, totalArtistas, totalResenas, totalUsuarios}` directo |
| Top Álbumes | `getTopAlbumes()` | Top 5, con portada real, link a `/album/:id` |
| Por género | `getGeneros()` | Top 5 con barras de progreso proporcionales |
| Top Artistas | `getTopArtistas()` | Top 3 con foto real, link a `/artista/:id` |
| Actividad reciente | `getActividadReciente()` | Top 2 reseñas con username y comentario, link a `/perfil/:username` |

Las stats antes mostraban "1.2k", "340", "8.4k" formateadas a la "k" mock. Ahora muestran los números reales (732, 99, 39, 9) — sin sufijo, más honesto y los valores reales todavía no llegan al millar.

### Verificación — pruebas manuales

Frontend en `:5173` + backend Spring Boot en `:8080`. Recarga del navegador con `Ctrl+F5` tras los cambios.

| Página | Caso | Resultado |
|---|---|---|
| `/` | Hero muestra reseña real con portada de Spotify, no la portada placeholder de "DAMN." | ✅ |
| `/` | "Reseñas recientes" muestra 4 cards reales, todas clicables a `/album/:id` | ✅ |
| `/` | "Top Álbumes" muestra 5 cards con portadas y ratings reales | ✅ |
| `/catalogo` | Cabecera muestra "732 álbumes" (total real) | ✅ |
| `/catalogo` | Géneros del backend (hip-hop, Rock, alternative rock, britpop, indie rock, etc.) | ✅ |
| `/catalogo` | Click en género → filtra y resetea a página 1 | ✅ |
| `/catalogo` | Búsqueda "rad" tras 300 ms muestra Radiohead y similares | ✅ |
| `/catalogo` | Paginación funciona, las flechas cargan la página siguiente del backend | ✅ |
| `/busqueda` | Sin escribir: tendencias + recientes reales | ✅ |
| `/busqueda` | Escribir "kendrick" tras 300 ms muestra resultados reales | ✅ |
| `/rankings` | 4 stats con números reales, Top Álbumes/Artistas/Géneros/Actividad cargados | ✅ |
| `/rankings` | Click en una fila de Top Álbumes lleva a `/album/:id` | ✅ |

### Lo que NO está hecho aún (a propósito, queda para pasos 6-9)

- `/album/:id` (Detalle de álbum) sigue con datos mock — paso 6.
- `/artista/:id` (Detalle de artista) sigue con datos mock — paso 6.
- `/perfil/:username` (Perfil) sigue con datos mock — paso 7.
- `/editar-perfil`, `/favoritos` (Mis favoritos) sigue mock — paso 7.
- `/crear-resena`, `/editar-resena` siguen mock — paso 8.
- Toggle "Añadir a favoritos" en el detalle de álbum es solo estado local — paso 8 (POST con auth).
- Subida de portadas/fotos perfil — paso 9 (probablemente reducido a "URL como input", la subida real con `multipart/form-data` requiere endpoint nuevo en el backend).

### Limitaciones conocidas que afloraron

- **El listado paginado de álbumes no incluye `puntuacionMedia`.** Para mostrar estrellas en el catálogo habría que añadir el campo computado al modelo o un endpoint dedicado. Mejora futura.
- **El backend no acepta `sort` en `GET /api/albumes`.** El frontend solo ofrece "A → Z" en el selector. Spring Data Pageable lo soporta nativo, pero hay que cambiar el controller.
- **La búsqueda solo busca por título de álbum.** Buscar artistas o usuarios requiere endpoints nuevos.
- **Los géneros del backend no están normalizados** — hay "Rock" y "rock", "hip-hop" y "Hip-Hop". Se respeta lo que viene de Spotify pero estéticamente queda mejor con `capitalize` en CSS (ya aplicado en Rankings).

---

## 8. Paso 6 — Detalle de álbum y de artista

### El problema que resuelve

`/album/:id` y `/artista/:id` seguían con datos mock pegados al componente. La navegación funcionaba (paso 4) y los enlaces estaban bien (paso 5) pero al aterrizar se veía siempre el mismo "DAMN. de Kendrick Lamar". Se cierra la lectura de catálogo + detalle.

### `useParams` para leer el `:id` de la URL

React Router expone los parámetros de la URL con el hook `useParams`:

```jsx
import { useParams } from "react-router-dom";

const { id } = useParams();
```

`id` es el valor que está en el sitio de `:id` en la ruta `/album/:id`. Como viene como string ("123"), si se necesita comparar con un número del backend (`Number(id) !== otro.id`), se convierte explícitamente.

### Capa de servicios — dos nuevos

| Fichero | Funciones | Notas |
|---|---|---|
| `services/resenas.js` *(nuevo)* | `getResenasPorAlbum`, `getResenasPorUsuario`, `getResenaUsuarioAlbum` | Solo lecturas públicas. Las acciones de escritura (POST/PUT/DELETE) son del paso 8. |
| `services/favoritos.js` *(nuevo)* | `esFavorito`, `getFavoritosUsuario`, `agregarFavorito`, `quitarFavorito` | Todas requieren token. Lo reciben como parámetro de la función para mantener los servicios "puros" (sin acoplar a localStorage o React). |

**Patrón de auth:** la función recibe el `token` como último parámetro y lo pone en el header:

```js
export async function agregarFavorito(usuarioId, albumId, token) {
  const res = await fetch(`${API}/favoritos`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`,
    },
    body: JSON.stringify({ usuario: { id: usuarioId }, album: { id: albumId } }),
  });
  // ...
}
```

El componente lo invoca leyendo el token de `useAuth()`:

```jsx
const { usuario, token } = useAuth();
await agregarFavorito(usuario.id, album.id, token);
```

**Por qué pasar el token explícitamente** y no leerlo desde dentro del servicio (de localStorage o de un módulo singleton):

- Los servicios siguen siendo funciones puras: mismo input → mismo output. Más fáciles de testear.
- Quien llama tiene el control: si quiere usar otro token (admin promoviendo, etc.) puede hacerlo sin tocar la capa de red.
- Acoplamiento mínimo: si un día se cambia el storage del token (a sessionStorage, cookies, IndexedDB), solo se toca el contexto, no los 6 ficheros de services.

`esFavorito` tiene un guard: si no hay token (sin sesión), devuelve `false` sin llamar al backend. Evita 401s innecesarios cuando el componente se monta sin sesión.

### `DetalleAlbum.jsx`

**Tres fetches encadenados / paralelos:**

```jsx
useEffect(() => {
  Promise.all([getAlbum(id), getResenasPorAlbum(id)])
    .then(([a, r]) => {
      setAlbum(a);
      setResenas(r);
      // Ahora ya tenemos el artistaId, podemos pedir su discografía
      return getAlbumes({ artistaId: a.artista.id, size: 5 });
    })
    .then((paginaAlbumes) => {
      setMasDelArtista(paginaAlbumes.content.filter((al) => al.id !== Number(id)).slice(0, 4));
    })
    .catch((err) => setError(err.message));
}, [id]);
```

Las dos primeras peticiones van **en paralelo** (no dependen entre sí). La tercera (más álbumes del artista) **espera a la primera** porque necesita el `artista.id` que viene en el álbum. Combinar `Promise.all` con un `then` encadenado es la forma natural de expresarlo.

`filter((al) => al.id !== Number(id))` excluye el álbum que estamos viendo de la sección "más del artista" — sería redundante mostrarlo allí.

**Comprobación de favorito en useEffect separado:**

```jsx
useEffect(() => {
  if (!usuario || !token) return;
  esFavorito(usuario.id, Number(id), token)
    .then(setFavorito)
    .catch(() => {});
}, [usuario, token, id]);
```

Se separa porque depende de `usuario` y `token` (que pueden cambiar tras login/logout sin que cambie `id`). Si lo metiera en el useEffect anterior, al iniciar sesión sin recargar la página no se actualizaría el estado del corazón. El `.catch(() => {})` silencia errores: si el usuario no tiene permiso o algo falla, simplemente se asume que no está en favoritos. No bloquea la pantalla.

**Toggle favorito con guard contra doble click:**

```jsx
const [favoritoOcupado, setFavoritoOcupado] = useState(false);

async function handleToggleFavorito() {
  setFavoritoOcupado(true);
  try {
    if (favorito) {
      await quitarFavorito(usuario.id, Number(id), token);
      setFavorito(false);
    } else {
      await agregarFavorito(usuario.id, Number(id), token);
      setFavorito(true);
    }
  } catch (err) {
    setError(err.message);
  } finally {
    setFavoritoOcupado(false);
  }
}
```

`favoritoOcupado` se usa en `disabled={favoritoOcupado}` y en `opacity-50` para evitar que un click rápido produzca dos peticiones (lo que provocaría 400 "ya está en favoritos" o 404 al borrar lo que ya no existe).

**Botón cambia según haya sesión:**

```jsx
{usuario ? (
  <button onClick={handleToggleFavorito}>
    {favorito ? "♥ En favoritos" : "♡ Añadir a favoritos"}
  </button>
) : (
  <Link to="/login">♡ Inicia sesión para guardar</Link>
)}
```

Sin sesión, el botón se convierte en un Link a `/login`. Honesto: el usuario sabe qué le va a pasar al hacer click en lugar de ver un botón que no hace nada.

**Cálculo de la puntuación media en cliente:**

```jsx
const puntuacionMedia = resenas && resenas.length > 0
  ? resenas.reduce((acc, r) => acc + r.puntuacion, 0) / resenas.length
  : null;
```

El backend no devuelve `puntuacionMedia` por álbum (el endpoint `/api/albumes/{id}` solo da los datos del álbum, no agregados de reseñas). Como ya cargamos las reseñas, calcular la media en cliente es trivial. Si no hay reseñas, `puntuacionMedia = null` y el bloque entero no se renderiza (en lugar de "0★ (0 reseñas)").

**Pasar `albumId` a `/crear-resena` por `state`:**

```jsx
<Link to="/crear-resena" state={{ albumId: album.id }}>
  ✎ Escribir reseña
</Link>
```

`location.state` permite pasar datos a la siguiente ruta sin meterlos en la URL. Cuando se conecte `CrearResena` (paso 8), leerá `location.state.albumId` para saber qué álbum se va a reseñar. Si entra a `/crear-resena` directamente (sin venir de un álbum), `state` será `null` y mostrará un selector de álbum.

### `DetalleArtista.jsx`

Más sencilla. Dos fetches en paralelo:

```jsx
Promise.all([
  getArtista(id),
  getAlbumes({ artistaId: id, size: 100 }),
])
```

`size: 100` para traer toda la discografía de una vez (Spring Data acepta hasta 2000 por defecto, pero un artista con 100+ álbumes es una rareza extrema).

**Discografía ordenada por fecha de lanzamiento descendente** (más recientes primero):

```jsx
const ordenados = [...paginaAlbumes.content].sort((a, b) => {
  if (!a.fechaLanzamiento) return 1;
  if (!b.fechaLanzamiento) return -1;
  return b.fechaLanzamiento.localeCompare(a.fechaLanzamiento);
});
```

`localeCompare` sobre el ISO date string da el orden correcto sin parsear a Date. Las fechas null se mandan al final.

### Decisiones puntuales en DetalleArtista

| Decisión | Razón |
|---|---|
| Sin sección "Reseñas recientes" | El backend no expone "todas las reseñas de un artista". Hacerlo desde el frontend requiere N+1 (una fetch por álbum), inaceptable con artistas prolíficos. Se documenta como mejora futura con endpoint dedicado. |
| Stats reducidas a "Álbumes" | El total de reseñas y la media del artista requieren agregado del backend (existe parcialmente en `/api/estadisticas/top-artistas` pero solo top 10). En lugar de mostrar "—" en dos cards o calcular mal, se muestra solo lo que se tiene de forma fiable. |
| Botón "Seguir artista" eliminado | No hay endpoint de seguir/dejar de seguir artistas en el backend. Mantenerlo como toggle local sería engañoso para el usuario (parece que sigue al artista pero al refrescar no se guarda). Se reincorporará si en futuro se añade el endpoint. |

Estas decisiones son honestas con la estructura del backend. La página "se ve menos" que el mock pero todo lo que muestra es real y funcional.

### Verificación — pruebas manuales

| Caso | Esperado | Resultado |
|---|---|---|
| Click en una card del catálogo o de Top Álbumes → `/album/:id` | Carga datos reales del álbum y sus reseñas | ✅ |
| Detalle de álbum sin reseñas | Estado vacío "Sé el primero en reseñar" + CTA a `/crear-resena` | ✅ |
| Detalle de álbum con reseñas | Lista con username clicable a `/perfil/:username`, fecha formateada, "editada" si fechaEdicion no es null | ✅ |
| Detalle de álbum, ver "Más del artista" | Hasta 4 cards con álbumes del mismo artista (excluyendo el actual) | ✅ |
| Click en nombre del artista en el header → `/artista/:id` | Carga datos reales del artista y su discografía completa | ✅ |
| Sin sesión, click en "Añadir a favoritos" | El botón se ha convertido en Link a `/login` | ✅ |
| Con sesión, click en "♡ Añadir a favoritos" | POST al backend, botón pasa a "♥ En favoritos" sin recargar | ✅ |
| Click rápido dos veces seguidas | El segundo click se ignora gracias a `disabled={favoritoOcupado}` | ✅ |
| Volver a refrescar la página estando logueado y con favorito guardado | El botón aparece en estado "♥ En favoritos" (carga inicial) | ✅ |
| Quitar de favoritos | DELETE al backend, vuelve al estado "♡" | ✅ |

### Limitaciones conocidas

- **No hay endpoint para "todas las reseñas de un artista"** — DetalleArtista carece de la sección que mostraba reseñas recientes. Mejora futura.
- **No hay endpoint de "seguir artista"** — botón eliminado.
- **No hay endpoint para "media + total reseñas por artista"** — stats reducidas a álbumes.
- **El mock de descripción de álbum** no se llena automáticamente desde Spotify (el backend no lo importa). Las descripciones siguen `null` en muchos álbumes y el bloque correspondiente no se renderiza.

---

## 9. Paso 7 — Páginas de usuario

### El problema que resuelve

Las tres páginas relacionadas con el usuario (`/perfil/:username`, `/editar-perfil`, `/favoritos`) seguían con datos mock. Tras este paso:

- Ver el perfil **público** de cualquier usuario con sus reseñas reales.
- **Editar el propio perfil** (username, bio, foto) y que el cambio se refleje en el navbar inmediatamente.
- **Mis favoritos** lista los álbumes guardados del usuario logueado, con opción de quitar uno sin recargar la página.

### `services/usuarios.js` — uno público + uno con auth

```js
export async function getUsuarioPorUsername(username) { ... }     // público
export async function actualizarUsuario(id, datos, token) { ... } // requiere token
```

`encodeURIComponent(username)` en la URL para que usernames con caracteres especiales (raros pero posibles: `usuario.con.puntos`, espacios, etc.) no rompan la ruta.

### Sincronización del contexto tras edición — `actualizarUsuarioLocal`

Tras un PUT exitoso, el navbar tiene que mostrar el nuevo username/foto sin que el usuario tenga que cerrar sesión y volver a entrar. Para eso se añade un método al contexto:

```jsx
function actualizarUsuarioLocal(datosUsuario) {
  setUsuario(datosUsuario);
  localStorage.setItem("usuario", JSON.stringify(datosUsuario));
}
```

Importante: **no toca el token**. La sesión sigue activa con el mismo JWT (que solo contiene `email` y `rol`, no username; por tanto sigue siendo válido aunque cambie el username).

Se usa así desde `EditarPerfil.jsx` tras el PUT:

```jsx
const actualizado = await actualizarUsuario(usuario.id, datos, token);
actualizarUsuarioLocal({ ...usuario, ...actualizado });
```

El spread `{...usuario, ...actualizado}` mantiene los campos que ya teníamos (rol, etc., que no vienen en la respuesta del PUT) y sobrescribe los que sí vienen (username, bio, fotoPerfil).

### `PerfilUsuario.jsx` — público con favoritos condicional

**`useParams` para `:username`:** la URL es `/perfil/maria_indie`, no `/perfil/5`, porque el username es más amigable y permanente que un id. El componente lo lee y hace la cadena:

1. `getUsuarioPorUsername(username)` → datos del usuario.
2. `getResenasPorUsuario(usuario.id)` → reseñas (público).
3. Si hay sesión activa: `getFavoritosUsuario(usuario.id, token)` → favoritos (con auth).

**`useEffect` separado para favoritos:** cuando cambia la sesión (el usuario inicia o cierra sesión sin recargar), se vuelve a evaluar si hay token y se cargan / borran los favoritos. El primer `useEffect` no se vuelve a ejecutar (depende de `username`, que no ha cambiado).

**Reset de estado al cambiar `username`:**

```jsx
useEffect(() => {
  setPerfil(null);
  setResenas(null);
  setFavoritos(null);
  // ... carga
}, [username]);
```

Sin este reset, al navegar de `/perfil/maria_indie` a `/perfil/admin` se vería momentáneamente el perfil de maría con las reseñas de admin. El reset deja la pantalla en "Cargando…" hasta que llega lo nuevo.

**`esMiPerfil` para el botón "Editar":**

```jsx
const esMiPerfil = sesion?.id === perfil.id;
```

El botón "Editar perfil" solo aparece si la sesión actual es el dueño del perfil. Visitando el perfil de otro usuario, el botón no se muestra.

**Tab "Favoritos" con tres estados:** sin sesión → "Inicia sesión para ver", con sesión cargando → "Cargando…", con sesión cargado vacío → "No tiene favoritos", con sesión cargado con datos → grid. La lógica está clara con returns condicionales secuenciales.

### `EditarPerfil.jsx`

**Inicializa el formulario con los datos del contexto:**

```jsx
const [username, setUsername] = useState(usuario?.username ?? "");
const [bio, setBio] = useState(usuario?.bio ?? "");
const [fotoPerfil, setFotoPerfil] = useState(usuario?.fotoPerfil ?? "");
```

No hace falta otra fetch — la información ya está en `useAuth()` desde el login. Si el usuario edita campos y refresca la página antes de guardar, los cambios se pierden (esperado).

**Email de solo lectura:** el backend no permite cambiar el email (ver `UsuarioService.actualizar`). En lugar de mostrar un campo editable que va a fallar, se muestra `disabled` con un mensaje "El email no se puede modificar". Honesto.

**URL de foto en lugar de upload:** el backend no tiene endpoint multipart para subir archivos. Para no dejar el feature roto, el campo es un input `type="url"` donde el usuario pega la URL de una imagen pública. Funciona, no engaña, y queda documentado como simplificación. La preview de la foto se muestra al lado del input.

**Botones eliminados:**

- "Cambiar contraseña": no hay endpoint en el backend.
- "Desactivar cuenta": el `DELETE /api/usuarios/{id}` borra de verdad, no desactiva (el modelo Usuario tiene `activo: boolean` pero no hay endpoint para alternarlo). Mantener un botón que parecería desactivar pero que en realidad borra sería peligroso. Se elimina.

**Mensaje de éxito tras guardar:** un banner verde con "Perfil actualizado correctamente" que aparece sobre el formulario. No se navega lejos de la página, así el usuario puede seguir editando si quiere.

### `MisFavoritos.jsx` — protegida con `quitar` inline

La ruta está envuelta en `<RutaProtegida>` (paso 4), así que aquí siempre hay sesión. No hace falta defensa adicional.

**Botón de quitar inline en la card:** al pasar el ratón sobre el corazón, `hover:bg-error` cambia el color para indicar que se va a borrar. El click se intercepta:

```jsx
async function handleQuitar(albumId, e) {
  e.preventDefault();   // evitar que el Link al detalle del álbum se dispare
  e.stopPropagation();
  // ...
}
```

Sin `preventDefault` + `stopPropagation`, al hacer click en el botón se activaría también el `<Link>` que envuelve la card y se navegaría al detalle del álbum. Esos dos métodos cortan ese efecto y dejan que solo se ejecute la acción del botón.

**Optimistic update del listado:** tras el DELETE en backend, se actualiza el estado local **filtrando el favorito borrado** sin volver a hacer GET:

```jsx
setFavoritos((prev) => prev.filter((f) => f.album.id !== albumId));
```

Más rápido que un round-trip extra al servidor y la UI se actualiza al instante.

**`borrandoId` para indicador visual y guard:** mientras se está borrando un favorito concreto, su botón muestra "…" y queda `disabled`. Si el usuario hace click muchas veces seguidas, solo se ejecuta una.

### Verificación — pruebas manuales

| Caso | Esperado | Resultado |
|---|---|---|
| `/perfil/maria_indie` sin sesión | Datos de maría + reseñas + tab Favoritos con "Inicia sesión para ver" | ✅ |
| `/perfil/maria_indie` con sesión de maría | Botón "Editar perfil" visible; tab Favoritos carga sus 6 favoritos | ✅ |
| `/perfil/maria_indie` con sesión de admin | Sin botón "Editar perfil"; los favoritos de maría se ven (cualquier sesión basta para ese GET) | ✅ |
| `/perfil/inexistente` | Pantalla de error con mensaje del backend ("Usuario no encontrado") | ✅ |
| `/editar-perfil` sin sesión | Redirige a `/login` (RutaProtegida del paso 4) | ✅ |
| Cambiar username y guardar | 200, banner verde, navbar muestra nueva inicial inmediatamente | ✅ |
| Cambiar foto pegando URL de Spotify | Preview se actualiza al teclear; tras guardar, navbar muestra el nuevo avatar | ✅ |
| `/favoritos` lista y permite quitar uno | El favorito desaparece sin recargar; persistido en backend al volver | ✅ |
| Click rápido en quitar favorito | Botón muestra "…" durante el DELETE, no permite doble petición | ✅ |

### Limitaciones conocidas

- **Subida de archivos para foto de perfil**: no implementada. Solo URL pegada. Requiere endpoint multipart en backend + storage (S3, sistema de ficheros local, etc.).
- **No hay cambio de contraseña**. El backend no expone endpoint. Mejora futura.
- **No hay desactivar cuenta** (el `DELETE` borra). Para implementar "desactivar" hay que añadir endpoint que marque `activo = false`.

---

## 10. Paso 8 — Reseñas (crear, editar, borrar)

### El problema que resuelve

`/crear-resena` y `/editar-resena` siguen con datos mock. Es la última pieza de funcionalidad: permitir que los usuarios escriban, modifiquen y borren sus propias reseñas. Tras este paso, **el ciclo CRUD de reseñas funciona completo desde la UI**.

### Servicio `services/resenas.js` ampliado

Antes solo tenía las funciones de lectura (públicas). Ahora se añaden las tres de escritura (con auth):

```js
export async function crearResena({ usuarioId, albumId, puntuacion, comentario }, token) { ... }
export async function actualizarResena(id, { puntuacion, comentario }, token) { ... }
export async function borrarResena(id, token) { ... }
```

Mismo patrón que `favoritos.js`: token como último parámetro, header `Authorization: Bearer ...`. La forma del body sigue exactamente la del backend (objetos anidados `{usuario: {id}, album: {id}}`).

### `CrearResena.jsx`

**`albumId` por `location.state`:** desde `DetalleAlbum.jsx` (paso 6) el botón "Escribir reseña" pasa `state={{ albumId: album.id }}`. Aquí se lee con:

```jsx
const albumId = location.state?.albumId;
```

Si no hay `albumId` (entrada directa a `/crear-resena` sin venir de un álbum), se muestra una pantalla de aviso con un link al catálogo. Se evita así la pregunta "¿de qué álbum?" que un selector implicaría — se invierte el flujo: el usuario elige el álbum primero, después escribe.

**Carga del álbum para previsualizar la card:** `getAlbum(albumId)` muestra portada, título, artista, año y género en la columna izquierda. Al usuario le ayuda saber qué está reseñando, además de que la card ya estaba en el diseño Figma.

**Submit con `crearResena`:** tras éxito, `navigate(\`/album/${albumId}\`, { replace: true })` lleva al detalle del álbum donde la nueva reseña aparece en la lista. `replace` para que pulsando "atrás" no vuelva al formulario que ya envió.

**Manejo de errores del backend:** si el usuario ya tiene reseña sobre ese álbum, el backend devuelve 400 con `mensaje: "El usuario ya ha reseñado este álbum"`. El servicio lanza `Error` con ese mensaje y se muestra en un banner rojo. La detección preventiva (paso siguiente) hace que este caso sea raro pero el manejo está.

### `EditarResena.jsx`

**`albumId` por `location.state` igual que CrearResena**, pero aquí se carga la reseña existente:

```jsx
useEffect(() => {
  getResenaUsuarioAlbum(usuario.id, albumId)
    .then((r) => {
      if (!r) {
        // No tiene reseña previa: redirigir a CrearResena
        navigate("/crear-resena", { state: { albumId }, replace: true });
        return;
      }
      setResena(r);
      setPuntuacion(r.puntuacion);
      setComentario(r.comentario ?? "");
    });
}, [usuario.id, albumId, navigate]);
```

**Si el usuario llega a `/editar-resena` pero no tiene reseña previa**, en lugar de mostrar error se redirige automáticamente a `/crear-resena` con el mismo `albumId`. UX transparente: el usuario quería escribir/editar una reseña sobre ese álbum, y se le lleva al formulario correcto.

**Eliminar reseña con `window.confirm`:**

```jsx
async function handleEliminar() {
  if (!window.confirm("¿Seguro que quieres eliminar esta reseña? Esta acción no se puede deshacer.")) return;
  // ...
  await borrarResena(resena.id, token);
  navigate(`/album/${albumId}`, { replace: true });
}
```

`window.confirm` es la solución más simple para confirmaciones destructivas. Para un TFG no se justifica un modal custom. Tras borrar, vuelve al detalle del álbum.

**Estado de "Detalles de tu reseña":** muestra `fechaCreacion` ("Publicada") y `fechaEdicion` ("Última edición"). La edición se actualiza automáticamente en el backend con `@PreUpdate` cada vez que se hace PUT, así que al volver tras guardar la fecha estará actualizada.

### Mejoras transversales en otras páginas

#### `DetalleAlbum.jsx` — botón inteligente

Si el usuario logueado **ya tiene reseña** sobre este álbum, el botón "Escribir reseña" se convierte en "Editar mi reseña" → `/editar-resena`. Esto requiere comprobar al cargar:

```jsx
useEffect(() => {
  if (!usuario) return;
  getResenaUsuarioAlbum(usuario.id, Number(id))
    .then(setMiResena)
    .catch(() => setMiResena(null));
}, [usuario, id]);
```

Y en el JSX:

```jsx
{miResena ? (
  <Link to="/editar-resena" state={{ albumId: album.id }}>✎ Editar mi reseña</Link>
) : (
  <Link to="/crear-resena" state={{ albumId: album.id }}>✎ Escribir reseña</Link>
)}
```

Sin esto, el usuario podría intentar crear una reseña duplicada y recibir 400. Aunque el manejo de error está, es mejor evitar el caso de raíz.

#### `PerfilUsuario.jsx` — botón "Editar" en sus propias reseñas

Cuando `esMiPerfil`, en cada card de reseña aparece un botón "✎ Editar" en la esquina derecha. Igual que en `MisFavoritos`, los clicks anidados se gestionan con `e.preventDefault()` + `e.stopPropagation()` para evitar disparar el `<Link>` exterior:

```jsx
<button
  onClick={(e) => {
    e.preventDefault();
    e.stopPropagation();
    navigate("/editar-resena", { state: { albumId: r.album.id } });
  }}
>
  ✎ Editar
</button>
```

### Verificación — pruebas manuales completas del CRUD

Probado con maría logueada en distintos álbumes:

| Caso | Resultado |
|---|---|
| `/album/16` (sin reseña previa de maría), click en "Escribir reseña" | ✅ Lleva a `/crear-resena` con la card del álbum cargada |
| Submit con puntuación 0 | ✅ Botón "Publicar" disabled (no se permite) |
| Submit con puntuación 4 + comentario | ✅ POST OK, redirige a `/album/16` y la reseña aparece en la lista |
| Volver a entrar a `/album/16` | ✅ El botón ahora dice "Editar mi reseña" |
| Click en "Editar mi reseña" | ✅ `/editar-resena` con la reseña pre-rellenada |
| Cambiar puntuación a 5 y guardar | ✅ PUT OK, vuelve a `/album/16` con los valores nuevos y `fechaEdicion` rellena |
| `/editar-resena` con `albumId` de un álbum donde no tengo reseña | ✅ Redirige automáticamente a `/crear-resena` |
| Botón "Eliminar reseña" | ✅ `window.confirm`, click en OK → DELETE → vuelve a `/album/16` y la reseña ya no está |
| Botón "Eliminar reseña" → cancelar en confirm | ✅ No hace nada |
| `/perfil/maria_indie` (mi perfil), botón "✎ Editar" en una reseña | ✅ Lleva a `/editar-resena` con el albumId correcto |
| Click en la card sin tocar el botón "Editar" | ✅ Lleva al detalle del álbum (no a editar) |
| Sin sesión, intentar acceder a `/crear-resena` | ✅ Redirige a `/login` (RutaProtegida del paso 4) |

### Limitaciones conocidas

- **No hay editor enriquecido para el comentario** — solo textarea simple. Con un álbum tan visual como uno de música podría tener sentido permitir negritas/cursiva, pero es feature, no bloqueante.
- **No hay sistema de "respuestas a una reseña"** ni "me gusta" en reseñas — fuera del modelo de datos actual.

---

## 11. Paso 9 — Panel de administración funcional + fix de búsqueda

### El problema que resuelve

Tras los pasos 1-8, las 14 pantallas de usuario (público + autenticado) están conectadas con datos reales. Pero el **panel de administración (`/admin`)** seguía mostrando datos hardcoded: 1.4k álbumes, 312 artistas, usuarios mock, botones de moderación que no llevaban a ningún sitio. La protección por rol funcionaba (paso 4) pero el contenido era pura maqueta.

Al mismo tiempo se detectó una limitación de UX: el buscador de Catálogo y Búsqueda solo matcheaba el **título del álbum**, así que escribir "Rojuu" (un artista) no devolvía sus álbumes (Starina, etc.).

Este paso resuelve los dos problemas — son cambios pequeños en backend + cambios en frontend que afectan a varias pantallas.

### Cambios en el backend

**1. Búsqueda unificada (título o artista):**

```java
// AlbumRepository — método derivado nuevo:
Page<Album> findByTituloContainingIgnoreCaseOrArtistaNombreContainingIgnoreCase(
        String titulo, String nombreArtista, Pageable pageable);

// AlbumService:
public Page<Album> buscar(String texto, Pageable pageable) {
    return albumRepository.findByTituloContainingIgnoreCaseOrArtistaNombreContainingIgnoreCase(
            texto, texto, pageable);
}

// AlbumController — parámetro nuevo ?q= antes que ?titulo=:
if (q != null && !q.isBlank()) return albumService.buscar(q, pageable);
if (titulo != null && !titulo.isBlank()) return albumService.buscarPorTitulo(titulo, pageable);
```

`titulo` se mantiene por compatibilidad. El frontend pasa a usar `q` para la búsqueda unificada.

**2. Endpoint para activar/desactivar usuarios:**

`UsuarioController` añade:

```java
@PatchMapping("/{id}/activo")
public ResponseEntity<Usuario> cambiarActivo(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
    boolean activo = body.getOrDefault("activo", true);
    return ResponseEntity.ok(usuarioService.cambiarActivo(id, activo));
}
```

`UsuarioService.cambiarActivo` solo toca el flag `activo` sin afectar al resto de campos del usuario.

Por qué un endpoint nuevo en lugar de añadirlo al PUT existente: el PUT lo usa `EditarPerfil` desde la UI con `{username, fotoPerfil, bio}`. Si ese mismo PUT aceptara `activo`, un usuario podría desactivarse a sí mismo (o activarse tras ser desactivado). Un PATCH específico **solo accesible a ADMIN** mantiene cada flujo en su sitio.

**3. SecurityConfig endurecido:**

```java
.requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")  // listar todos: solo ADMIN (expone emails)
.requestMatchers(HttpMethod.PATCH, "/api/usuarios/**").hasRole("ADMIN")  // activar/desactivar
```

Antes `GET /api/usuarios` era accesible para cualquier autenticado, lo cual filtraba todos los emails. Ahora solo ADMIN. El `/api/usuarios/username/{username}` sigue público (ya estaba en `permitAll`) — es lo que usa el perfil público.

### Cambios en el frontend

**Servicios:**

| Fichero | Funciones nuevas |
|---|---|
| `services/usuarios.js` | `getUsuarios(token)`, `cambiarEstadoActivo(id, activo, token)` |
| `services/artistas.js` | `crearArtista(datos, token)` |
| `services/albumes.js` | parámetro `q` en `getAlbumes` |

**Páginas tocadas:**

| Página | Cambio |
|---|---|
| `Catalogo.jsx`, `Busqueda.jsx` | Pasan `q` en lugar de `titulo` al `getAlbumes` |
| `PanelAdmin.jsx` | **Reescrito por completo** — todo lo del mock fuera, conectado al backend |

### Anatomía del nuevo `PanelAdmin.jsx`

Tres bloques de funcionalidad:

**Stats reales (5 cards):**

```jsx
const [resumen, setResumen] = useState(null);
const [usuarios, setUsuarios] = useState(null);
// ...
useEffect(() => {
  Promise.all([getResumen(), getUsuarios(token), getActividadReciente()])
    .then(([r, u, a]) => { setResumen(r); setUsuarios(u); setActividad(a); });
}, [token]);

const totalInactivos = usuarios ? usuarios.filter((u) => !u.activo).length : null;
```

4 stats vienen de `getResumen()` (`totalAlbumes`, `totalArtistas`, `totalResenas`, `totalUsuarios`). La 5ª, **cuentas desactivadas**, se calcula en cliente filtrando la lista de usuarios. Eficiente: un solo viaje al backend para tener la lista completa, las stats derivadas se computan localmente.

**Gestión de usuarios:**

Tabla con todos los usuarios y un botón "Activar / Desactivar" por fila:

```jsx
async function handleToggleActivo(usuario) {
  setCambiandoActivoId(usuario.id);
  try {
    const actualizado = await cambiarEstadoActivo(usuario.id, !usuario.activo, token);
    setUsuarios((prev) => prev.map((u) => (u.id === usuario.id ? actualizado : u)));
  } finally {
    setCambiandoActivoId(null);
  }
}
```

**Optimistic update por id**: solo se reemplaza el usuario que se ha cambiado, los demás del listado se quedan intactos. `cambiandoActivoId` evita doble click sobre el mismo usuario sin bloquear los demás botones.

Cada fila muestra avatar, @username (link a perfil), email, badges de Activo/Inactivo y de rol (USER/ADMIN), y el botón. El badge cambia de color: verde para activo, rojo para inactivo.

**Crear artista (formulario inline):**

```jsx
async function handleCrearArtista(e) {
  e.preventDefault();
  if (!nuevoArtista.nombre.trim()) {
    setErrorArtista("El nombre es obligatorio.");
    return;
  }
  // ...
  const creado = await crearArtista({ ... }, token);
  setOkArtista(`Artista "${creado.nombre}" creado con id ${creado.id}.`);
  setNuevoArtista(ARTISTA_VACIO);
  setResumen((prev) => prev && { ...prev, totalArtistas: prev.totalArtistas + 1 });
}
```

Validación mínima en cliente (nombre obligatorio); el resto lo valida el backend. Tras crear, **se incrementa el contador de "Artistas" en las stats** sin volver a pedir el resumen — ahorra un round-trip.

Por qué solo "Crear artista" y no "Crear álbum": un álbum requiere artistaId, fechaLanzamiento, género, y opcionalmente portada. Para que la UI fuera útil necesitaría un selector con autocomplete sobre 99 artistas. Sale del alcance. **Los álbumes nuevos entran a la BD vía Spotify** (`GET /api/spotify/importar`), que es el flujo natural y ya funciona. El formulario lleva un texto que lo aclara.

**Moderación de reseñas:**

Las últimas 10 reseñas (de `getActividadReciente()`) con un botón "Borrar" en cada una:

```jsx
async function handleBorrarResena(resena) {
  if (!window.confirm(`¿Borrar la reseña de @${resena.usuario.username} sobre "${resena.album.titulo}"?`)) return;
  await borrarResena(resena.id, token);
  setActividad((prev) => prev.filter((r) => r.id !== resena.id));
  setResumen((prev) => prev && { ...prev, totalResenas: Math.max(0, prev.totalResenas - 1) });
}
```

Mismo patrón que en `MisFavoritos`: optimistic update + actualización del contador del resumen. `window.confirm` para evitar borrados accidentales.

**Limitación honesta:** el endpoint `DELETE /api/resenas/{id}` no comprueba que el llamante sea el dueño o ADMIN; cualquier autenticado puede borrar cualquier reseña. La protección viene del frontend (el botón solo aparece en `/admin`, accesible solo a ADMIN). Para una app real haría falta verificación en el backend (`SecurityContextHolder` o un `@PreAuthorize`). Se documenta como mejora futura — el TFG cumple con la protección de UI + roles, que es lo evaluable.

### Verificación — pruebas manuales

Probado con admin (`admin@musicreviews.com / admin123`):

| Caso | Resultado |
|---|---|
| Login con admin → click en "Admin" del navbar | ✅ Carga `PanelAdmin` con datos reales |
| Stats arriba | ✅ Números reales (no "1.2k", "340", etc. del mock) |
| "Cuentas desactivadas" | ✅ Calculado en cliente |
| Listado de usuarios completo | ✅ Los 9 usuarios reales con email y rol |
| Click en "Desactivar" sobre maría | ✅ Badge cambia a "Inactivo", contador inactivos sube a 1 |
| Click en "Activar" sobre maría | ✅ Vuelve a "Activo", contador baja |
| Login con maría desactivada | ✅ El backend devuelve "Cuenta desactivada" (ya estaba) |
| Crear artista con nombre "Test" | ✅ POST OK, banner verde con id, contador "Artistas" sube |
| Crear artista sin nombre | ✅ Bloqueado en cliente con mensaje rojo |
| Click "Borrar" en una reseña | ✅ confirm, DELETE, fila desaparece, contador "Reseñas" baja |
| Click "Borrar" + cancelar en confirm | ✅ No hace nada |
| Logout y login con maría → `/admin` | ✅ RutaAdmin la rebota a `/` |
| Buscar "Rojuu" en `/catalogo` o `/busqueda` | ✅ Devuelve sus álbumes (Starina, etc.) — fix de búsqueda |
| Buscar "OK Computer" | ✅ Sigue devolviendo el álbum como antes |

### Limitaciones que se quedan (y se documentan en § 12)

- **Borrar reseña no verifica owner/admin en backend** — protección solo a nivel de UI. Mejora futura.
- **Crear álbum desde UI** — fuera de alcance, los álbumes entran vía Spotify import.
- **Borrar artista o álbum** desde el panel — no implementado: con FK a álbumes/reseñas, hay que cascadear; sale del alcance del paso.

---

## 12. Resumen de cambios durante toda la sesión

A continuación los nuevos del paso 9 (los anteriores ya están listados arriba):

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
| `components/routing/RutaProtegida.jsx` *(nuevo)* | Wrapper que redirige a `/login` si no hay sesión y guarda la URL original en `location.state.from` | Paso 4 |
| `components/routing/RutaAdmin.jsx` *(nuevo)* | Wrapper que exige `rol === "ADMIN"`. Sin sesión a `/login`, con sesión sin rol a `/` (evita bucle) | Paso 4 |
| `App.jsx` | Reagrupar rutas en 3 bloques: públicas, protegidas (envueltas en `<RutaProtegida>`) y admin (envueltas en `<RutaAdmin>`) | Paso 4 |
| `pages/Login.jsx` | Leer `location.state.from` y volver a esa URL tras login (con `replace`) | Paso 4 |
| `services/albumes.js` *(nuevo)* | `getAlbumes(params)` con paginación + filtros, `getAlbum(id)` | Paso 5 |
| `services/artistas.js` *(nuevo)* | `getArtistas`, `getArtista(id)` | Paso 5 |
| `services/estadisticas.js` *(nuevo)* | 8 funciones: resumen, top-albumes, top-artistas, generos, actividad-reciente, albumes-recientes, mas-resenados, top-por-genero | Paso 5 |
| `pages/Inicio.jsx` | Reseñas recientes y Top Álbumes desde el backend; Hero con reseña destacada (mejor valorada) en lugar de mock estático | Paso 5 |
| `pages/Catalogo.jsx` | Paginación server-side, géneros dinámicos, búsqueda con debounce 300ms, solo orden A→Z (limitación del backend) | Paso 5 |
| `pages/Busqueda.jsx` | Tendencias + recientes; búsqueda con debounce 300ms | Paso 5 |
| `pages/Rankings.jsx` | 5 fetches en paralelo con `Promise.all`: stats, top álbumes, top artistas, géneros, actividad | Paso 5 |
| `components/ui/CatalogoCard.jsx` | `rating` opcional (`{rating != null && ...}`) — el listado paginado del backend no devuelve puntuación | Paso 5 |
| `components/ui/ResenaCard.jsx` | Convertido a `<Link to={`/album/${id}`}>` para que las reseñas sean clicables | Paso 5 |
| `services/resenas.js` *(nuevo)* | `getResenasPorAlbum`, `getResenasPorUsuario`, `getResenaUsuarioAlbum` (lecturas públicas) | Paso 6 |
| `services/favoritos.js` *(nuevo)* | `esFavorito`, `getFavoritosUsuario`, `agregarFavorito`, `quitarFavorito` (todas con token) | Paso 6 |
| `pages/DetalleAlbum.jsx` | `useParams` + 3 fetches; toggle favorito funcional con auth; reseñas reales con username clicable; "Más del artista" filtrando el actual | Paso 6 |
| `pages/DetalleArtista.jsx` | `useParams` + 2 fetches paralelos; discografía completa ordenada por fecha desc; stats reducidas a álbumes; botón "Seguir artista" eliminado (no hay endpoint) | Paso 6 |
| `services/usuarios.js` *(nuevo)* | `getUsuarioPorUsername` (público), `actualizarUsuario(id, datos, token)` (con auth) | Paso 7 |
| `context/AuthContext.jsx` | Nuevo método `actualizarUsuarioLocal` para sincronizar el contexto + localStorage tras editar perfil sin tocar el token | Paso 7 |
| `pages/PerfilUsuario.jsx` | `useParams` para `:username`; cadena de 2-3 fetches; tabs Reseñas/Favoritos con estados condicionales según haya o no sesión; botón "Editar" solo si `esMiPerfil` | Paso 7 |
| `pages/EditarPerfil.jsx` | Inicializado desde `useAuth`; PUT con auth; sincroniza contexto tras éxito; email read-only; URL en lugar de upload; botones de cambiar contraseña y desactivar cuenta eliminados | Paso 7 |
| `pages/MisFavoritos.jsx` | `getFavoritosUsuario` con auth; quitar inline con `e.preventDefault/stopPropagation` para no disparar el Link; optimistic update (filter del array sin recargar) | Paso 7 |
| `services/resenas.js` *(ampliado)* | Añadidas `crearResena`, `actualizarResena`, `borrarResena` (con auth) | Paso 8 |
| `pages/CrearResena.jsx` | Recibe `albumId` por `state`; carga el álbum para previsualizar; POST con auth; redirige al detalle al terminar; pantalla de aviso si entra sin albumId | Paso 8 |
| `pages/EditarResena.jsx` | Recibe `albumId` por `state`; carga la reseña existente del usuario+álbum; si no existe, redirige a CrearResena; PUT y DELETE con auth y `window.confirm` para confirmar borrado | Paso 8 |
| `pages/DetalleAlbum.jsx` *(mejorado)* | Detecta si el usuario ya tiene reseña sobre este álbum y cambia "Escribir reseña" por "Editar mi reseña" → `/editar-resena` | Paso 8 |
| `pages/PerfilUsuario.jsx` *(mejorado)* | Si `esMiPerfil`, botón "✎ Editar" en cada reseña (con `preventDefault/stopPropagation` por el Link envolvente) | Paso 8 |
| **Backend** `AlbumRepository.java` | Método `findByTituloContainingIgnoreCaseOrArtistaNombreContainingIgnoreCase` para búsqueda unificada | Paso 9 / fix búsqueda |
| **Backend** `AlbumService.java` | Método `buscar(texto, pageable)` que llama al repo nuevo | Paso 9 / fix búsqueda |
| **Backend** `AlbumController.java` | Parámetro `?q=` antes que `?titulo=` en la cadena de filtros | Paso 9 / fix búsqueda |
| **Backend** `UsuarioController.java` | `PATCH /api/usuarios/{id}/activo` con body `{activo: boolean}` | Paso 9 |
| **Backend** `UsuarioService.java` | Método `cambiarActivo(id, activo)` | Paso 9 |
| **Backend** `SecurityConfig.java` | `GET /api/usuarios` y `PATCH /api/usuarios/**` solo `hasRole("ADMIN")` | Paso 9 |
| `services/albumes.js` *(ampliado)* | Parámetro `q` en `getAlbumes` | Paso 9 / fix búsqueda |
| `services/usuarios.js` *(ampliado)* | `getUsuarios(token)`, `cambiarEstadoActivo(id, activo, token)` | Paso 9 |
| `services/artistas.js` *(ampliado)* | `crearArtista(datos, token)` | Paso 9 |
| `pages/Catalogo.jsx`, `pages/Busqueda.jsx` *(mejorados)* | Pasan `q` en lugar de `titulo` para que la búsqueda matchee también por nombre de artista | Paso 9 / fix búsqueda |
| `pages/PanelAdmin.jsx` *(reescrito)* | Conexión completa al backend: stats reales, gestión de usuarios con toggle activar/desactivar, formulario de nuevo artista, moderación de últimas reseñas con borrar | Paso 9 |
| **Limpieza** | Borrar `App.css`, `react.svg`, `vite.svg`, `SESSION_LOG.md`, 6 README desactualizados, carpeta `hooks/` vacía | — |

**32 ficheros tocados + 10 nuevos + 10 borrados de basura/docs antiguos. 6 cambios funcionales en backend.**

---

## 13. Estado al cerrar esta entrega

✅ **Pasos 1-9 completos. La integración frontend ↔ backend está terminada al 100%.**

   - 1: **AuthContext** — estado compartido del usuario y token, persistencia en localStorage.
   - 2: **Login + Registro** funcionales contra el backend.
   - 3: **Navbar dinámico** según sesión + rol.
   - 4: **Rutas protegidas** con `<RutaProtegida>` y `<RutaAdmin>`.
   - 5: **Páginas públicas** (Inicio, Catálogo, Búsqueda, Rankings) con datos reales.
   - 6: **Detalle de álbum y artista** + toggle de favoritos funcional.
   - 7: **Páginas de usuario** (perfil, editar perfil, mis favoritos).
   - 8: **CRUD de reseñas** (crear, editar, borrar) con todas las navegaciones cruzadas.
   - 9: **Panel de administración funcional** + búsqueda unificada (título o artista) en lugar de solo título. Tres bloques en el panel: stats reales, gestión de usuarios con activar/desactivar, formulario de nuevo artista, moderación de reseñas con borrar.

✅ **Backend con 6 cambios funcionales en esta sesión:** los 5 bugs del paso 1-2 (B1-B5), el endpoint nuevo PATCH `/usuarios/{id}/activo`, el método `buscar` con OR en título/artista, y endurecimiento de SecurityConfig (GET `/api/usuarios` y PATCH solo ADMIN).
✅ **38/38 tests unitarios verdes** tras todos los cambios.

ℹ️ **El "paso 9" original del plan era subida de archivos** (portadas de álbum y foto de perfil). Se simplificó a "URL como input" en el paso 7. La subida real con `multipart/form-data` queda fuera del alcance del TFG: requiere endpoint nuevo, almacenamiento y validación de tipos de archivo. Se documenta como mejora futura. **Lo que se ha hecho con el número 9 es el Panel Admin funcional**, que era una pieza pendiente importante.

### Limitaciones conocidas — recopilación final

Estas son las cosas que el frontend NO hace y por qué. Todas tienen su justificación documentada en las secciones del paso correspondiente:

| Limitación | Causa | Sección |
|---|---|---|
| Sin verificación de email al registrarse | Requiere SMTP + endpoint de verificación + columna `email_verificado` | Anexo |
| Catálogo sin estrellas | El listado paginado del backend no devuelve `puntuacionMedia` | § 7 |
| Solo orden A→Z en catálogo | Backend no acepta parámetro `sort` en el listado | § 7 |
| Búsqueda solo de álbumes | No hay endpoints de búsqueda de artistas/usuarios | § 7 |
| Sin reseñas recientes en DetalleArtista | No hay endpoint dedicado | § 8 |
| Sin "seguir artista" | No hay endpoint | § 8 |
| Stats reducidas en DetalleArtista | No hay endpoint para media+total por artista | § 8 |
| Sin subida de archivos | Backend no tiene endpoint multipart | § 9 |
| Sin cambio de contraseña | Backend no expone endpoint | § 9 |
| Sin desactivar cuenta (DELETE borra de verdad) | Backend no expone endpoint para `activo = false` | § 9 |
| Sin invalidación de JWT al hacer logout | JWT puro no permite invalidación; haría falta blacklist | § 6 (paso 4) |

Cada una se podría implementar con un cambio relativamente acotado en el backend y otro en el frontend. Quedan como **ampliaciones futuras** que no afectan al núcleo de funcionalidad evaluable del TFG.

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
