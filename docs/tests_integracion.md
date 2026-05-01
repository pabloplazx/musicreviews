# Tests de integración — MusicReviews Backend

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| JUnit 5 | 5.x (Spring Boot managed) | Framework de tests |
| Spring Boot Test | 4.0.5 | `@SpringBootTest` levanta el contexto Spring entero |
| MockMvc | 4.x (Spring Boot managed) | Cliente HTTP en memoria contra el `DispatcherServlet` |
| Spring Security Test | 4.x (Spring Boot managed) | Aplica el filtro de seguridad real al `MockMvc` |
| H2 Database | runtime (managed) | Base de datos en memoria, modo MySQL, schema regenerado en cada arranque |

A diferencia de los unitarios, estos tests **arrancan el contexto Spring completo** (controller + service + JPA + security) y atacan los endpoints REST como caja negra a través de `MockMvc`. La diferencia clave: **no se mockea nada**. Repositorios, validadores, filtros de seguridad y serialización JSON pasan por su flujo real.

La base de datos durante los tests es **H2 en memoria** con perfil `test`. No se conecta a Aiven (a diferencia del único test de carga de contexto previo, `BackendApplicationTests`, que sí lo hacía y que no se ha modificado para no perder esa señal).

---

## Configuración del perfil `test`

**Ruta:** `src/test/resources/application-test.properties`

```properties
# H2 en memoria, modo compatible MySQL para que las consultas/dialecto se comporten parecido.
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate — dialecto H2 y schema regenerado en cada test run.
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.jpa.show-sql=false
# open-in-view alineado con application.properties principal — cambiarlo aquí oculta
# bugs de LazyInitializationException que sí ocurren en producción.
spring.jpa.open-in-view=true

# Valores dummy para que arranque el contexto (sin estos, @Value falla).
# Los tests no llaman a Spotify/Last.fm reales.
spotify.client-id=test-client-id
spotify.client-secret=test-client-secret
lastfm.api-key=test-lastfm-key

# JWT — clave de test (mínimo 32 caracteres por HMAC-SHA512).
jwt.secret=test-secret-key-at-least-32-chars-long-aaaa
jwt.expiration-ms=86400000
```

Notas de diseño relevantes para defender el TFG:

- **`MODE=MySQL`** y `NON_KEYWORDS=USER` en la URL de H2 hacen que los nombres de tabla y palabras reservadas se comporten igual que en MySQL (Aiven). Sin esto, `usuario` (tabla y palabra reservada en H2) rompía las inserciones.
- **`ddl-auto=create-drop`** garantiza que cada arranque del contexto regenera el schema. No depende de seeds previos ni de migraciones.
- **`open-in-view=true`** se mantiene igual que en producción a propósito. Inicialmente se puso en `false` ("buena práctica"), pero los tests de reseñas empezaron a fallar con `LazyInitializationException` — el mismo bug que ocurriría en producción si se desactivase. Mantenerlos alineados evita falsos verdes.
- **Credenciales dummy** (Spotify, Last.fm, JWT) son obligatorias porque varios `@Value` se resuelven al arrancar el contexto. Los tests no invocan APIs externas, así que cualquier valor sirve.

---

## Cómo ejecutar los tests

```bash
cd backend/backend

# Ejecutar todos los tests (unitarios + integración)
./mvnw test                                         # Linux/Mac
mvnw.cmd test                                       # Windows

# Solo los tests de integración (clases que terminan en IntegrationTest)
./mvnw test -Dtest='*IntegrationTest'

# Una clase concreta
./mvnw test -Dtest=AuthControllerIntegrationTest

# Un test concreto
./mvnw test -Dtest=AuthControllerIntegrationTest#login_conPasswordIncorrecta_devuelve400
```

Los tests usan el sufijo `IntegrationTest` (en lugar de `IT`, que no es recogido por Surefire por defecto) para que se ejecuten dentro de la fase `test` estándar de Maven y aparezcan en el mismo reporte que los unitarios. No se ha añadido el plugin Failsafe para mantener la configuración mínima.

---

## Resumen de cobertura

| Clase de test | Endpoints cubiertos | Tests | Resultado |
|---|---|---|---|
| `AuthControllerIntegrationTest` | `POST /api/auth/register`, `POST /api/auth/login` | 6 | ✅ |
| `AlbumControllerIntegrationTest` | `GET /api/albumes`, `GET /api/albumes/{id}` | 5 | ✅ |
| `ArtistaControllerIntegrationTest` | `GET /api/artistas`, `GET /api/artistas/{id}` | 4 | ✅ |
| `ResenaControllerIntegrationTest` | `GET /api/resenas`, `POST /api/resenas` (sin auth) | 4 | ✅ |
| `UsuarioControllerIntegrationTest` | `GET /api/usuarios/username/{username}` | 2 | ✅ |
| `EstadisticasControllerIntegrationTest` | `GET /api/estadisticas/*` (resumen, top-albumes, top-artistas, géneros, recientes) | 5 | ✅ |
| `FavoritoControllerIntegrationTest` | `GET /api/favoritos`, `POST /api/favoritos` (verificación de seguridad sin token) | 2 | ✅ |

**Total integración: 28 tests — todos passing ✅**
**Total proyecto (unitarios + integración): 78 tests — todos passing ✅**

Cobertura por página del frontend:

| Página | Endpoints consumidos | Cubierto por |
|---|---|---|
| Login | `POST /api/auth/login` | `AuthControllerIntegrationTest` |
| Registro | `POST /api/auth/register` | `AuthControllerIntegrationTest` |
| Catálogo | `GET /api/albumes` | `AlbumControllerIntegrationTest` |
| Búsqueda | `GET /api/albumes`, `GET /api/artistas` | `AlbumControllerIntegrationTest`, `ArtistaControllerIntegrationTest` |
| Detalle Álbum | `GET /api/albumes/{id}`, `GET /api/resenas?albumId=` | `AlbumControllerIntegrationTest`, `ResenaControllerIntegrationTest` |
| Detalle Artista | `GET /api/artistas/{id}`, `GET /api/albumes?artistaId=` | `ArtistaControllerIntegrationTest`, `AlbumControllerIntegrationTest` |
| Rankings | `GET /api/estadisticas/*` | `EstadisticasControllerIntegrationTest` |
| Perfil Usuario | `GET /api/usuarios/username/{username}`, `GET /api/resenas?usuarioId=` | `UsuarioControllerIntegrationTest`, `ResenaControllerIntegrationTest` |
| Crear Reseña | `POST /api/resenas` (requiere auth) | `ResenaControllerIntegrationTest` (verifica el bloqueo sin token) |
| Mis Favoritos | `GET/POST /api/favoritos` (requiere auth) | `FavoritoControllerIntegrationTest` (verifica el bloqueo sin token) |

---

## `AuthControllerIntegrationTest` — 6 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/AuthControllerIntegrationTest.java`

Verifica el flujo completo de autenticación contra H2: alta de usuario con BCrypt, generación de JWT, login con verificación de contraseña hasheada, y validaciones declarativas de los DTOs.

### `POST /api/auth/register`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `register_conDatosValidos_devuelve200ConToken` | Username + email + contraseña válidos | 200, JSON con `token`, `username`, `email`, `rol=USER` |
| `register_conEmailDuplicado_lanzaExcepcion` (`register_conEmailDuplicado_devuelve400`) | Ya existe un usuario con ese email | 400 (regla de negocio) |
| `register_conEmailMalFormado_devuelve400` | Email sin formato válido | 400 (validación `@Email`) |

### `POST /api/auth/login`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `login_conCredencialesValidas_devuelve200ConToken` | Email + contraseña correctos sobre usuario activo | 200, JSON con `token` y `email` |
| `login_conPasswordIncorrecta_devuelve400` | Email correcto, contraseña incorrecta | 400, no se filtra qué campo era erróneo |
| `login_conEmailInexistente_devuelve400` | Email que no existe en la BD | 400, mismo mensaje genérico que password incorrecta |

> El mismo mensaje genérico en `password incorrecta` y `email inexistente` es intencionado: evita que un atacante enumere usuarios registrados en la app.

---

## `AlbumControllerIntegrationTest` — 5 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/AlbumControllerIntegrationTest.java`

Cubre los endpoints públicos del catálogo (páginas Catálogo, Búsqueda, Detalle Álbum). Cada test prepara un álbum con su artista en H2 antes del request.

### `GET /api/albumes`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `listarAlbumes_devuelvePaginaConElAlbumCreado` | Listado paginado por defecto (`page=0`, `size=12`) | 200, `$.content[0].titulo` y `$.page.totalElements=1` |
| `listarAlbumes_filtroPorTituloParcial_devuelveCoincidencias` | `?titulo=Computer` sobre álbum "OK Computer" | 200, una coincidencia |
| `listarAlbumes_filtroPorTituloSinCoincidencias_devuelvePaginaVacia` | `?titulo=ZZZ-no-existe` | 200, página vacía (`totalElements=0`) |

> En Spring Boot 4 los metadatos de `Page` se serializan bajo `$.page` (no en raíz). Los tests usan `$.page.totalElements` en lugar de `$.totalElements`.

### `GET /api/albumes/{id}`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `obtenerPorId_existente_devuelve200ConDatos` | ID válido, álbum con artista cargado | 200, JSON con `titulo` y `artista.nombre` |
| `obtenerPorId_inexistente_devuelve404` | ID que no existe | 404 (`RecursoNoEncontradoException`) |

---

## `ArtistaControllerIntegrationTest` — 4 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/ArtistaControllerIntegrationTest.java`

Cubre la API pública de artistas (página Detalle Artista, filtro de Búsqueda).

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `listarArtistas_devuelveListaConElCreado` | Sin filtros — devuelve todos los artistas | 200, lista de tamaño 1 |
| `listarArtistas_filtroPorNombreParcial_devuelveCoincidencias` | `?nombre=Kendrick` sobre "Kendrick Lamar" | 200, una coincidencia |
| `obtenerPorId_existente_devuelve200ConDatos` | ID válido | 200, JSON con `nombre` y `pais` |
| `obtenerPorId_inexistente_devuelve404` | ID que no existe | 404 |

---

## `ResenaControllerIntegrationTest` — 4 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/ResenaControllerIntegrationTest.java`

Cubre la lectura pública de reseñas (Detalle Álbum, Perfil Usuario) y verifica que la creación está protegida por seguridad. El setup crea un usuario, un álbum y una reseña vinculados.

### Lecturas (públicas)

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `obtenerPorAlbumId_devuelveListaConLaResenaCreada` | `?albumId=` válido | 200, lista con `puntuacion=4.5` y `comentario="Obra maestra"` |
| `obtenerPorUsuarioId_devuelveListaConLaResenaCreada` | `?usuarioId=` válido | 200, lista de tamaño 1 |
| `obtener_sinParametros_devuelve400` | Sin `albumId` ni `usuarioId` | 400 (`ReglaNegocioException`: "Se requiere albumId o usuarioId") |

### Seguridad

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `crearResena_sinAutenticar_devuelve401o403` | `POST /api/resenas` sin header `Authorization` | 401 o 403 (depende del filtro JWT) |

> El test acepta tanto 401 como 403 porque el comportamiento exacto depende de la cadena de filtros: con el filtro JWT activo, una petición sin token suele responder 403 al no resolverse el `Authentication`.

---

## `UsuarioControllerIntegrationTest` — 2 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/UsuarioControllerIntegrationTest.java`

Cubre el único endpoint público de usuarios: la consulta por username (página Perfil Usuario). El resto de operaciones requieren autenticación y están cubiertas por los tests unitarios de `UsuarioService`.

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `obtenerPorUsername_existente_devuelve200ConDatosPublicos` | Username válido | 200, JSON con `username` y `bio`. **Nunca incluye `password`** (`@JsonProperty(WRITE_ONLY)`) |
| `obtenerPorUsername_inexistente_devuelve404` | Username que no existe | 404 |

> El test del campo `password` es defensa en profundidad: comprueba en integración real que el `WRITE_ONLY` de Jackson sí oculta el hash en la respuesta JSON, además de la validación que ya hacen los unitarios.

---

## `EstadisticasControllerIntegrationTest` — 5 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/EstadisticasControllerIntegrationTest.java`

Verifica que los endpoints de Rankings devuelven 200 con la estructura esperada. La lógica de cálculo (medias, ordenaciones, distribuciones) está cubierta por `EstadisticasServiceTest` (unitario) — aquí se verifica solo el cableado HTTP/JSON.

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `resumen_devuelve200ConTotales` | `GET /api/estadisticas/resumen` con 1 álbum y 1 artista en BD | 200, `totalAlbumes=1`, `totalArtistas=1` |
| `topAlbumes_devuelveLista` | `GET /api/estadisticas/top-albumes` | 200, JSON array |
| `topArtistas_devuelveLista` | `GET /api/estadisticas/top-artistas` | 200, JSON array |
| `distribucionGeneros_devuelveLista` | `GET /api/estadisticas/generos` | 200, JSON array |
| `albumesRecientes_devuelveLista` | `GET /api/estadisticas/albumes-recientes` | 200, JSON array |

---

## `FavoritoControllerIntegrationTest` — 2 tests

**Ruta:** `src/test/java/com/musicreviews/backend/controller/FavoritoControllerIntegrationTest.java`

Toda la API de favoritos requiere autenticación (página Mis Favoritos). Estos tests verifican el bloqueo de seguridad sin necesidad de simular el flujo completo de JWT — la lógica de negocio está cubierta por `FavoritoServiceTest`.

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `listarFavoritos_sinAutenticar_estaBloqueado` | `GET /api/favoritos?usuarioId=1` sin token | 401 o 403 |
| `agregarFavorito_sinAutenticar_estaBloqueado` | `POST /api/favoritos` sin token | 401 o 403 |

---

## Estructura común de los tests

Todos los tests de integración siguen el mismo patrón:

```java
@SpringBootTest
@ActiveProfiles("test")              // ← carga application-test.properties (H2)
class XxxControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private XxxRepository xxxRepository;
    // ...otros repositorios para limpiar/preparar datos

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())  // ← aplica el SecurityFilterChain real
                .build();

        // Borrado en orden: hijas (con FK) antes que padres
        resenaRepository.deleteAll();
        favoritoRepository.deleteAll();
        albumRepository.deleteAll();
        artistaRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Datos de prueba para este test class
        // ...
    }

    @Test
    void nombreDelTest_condicion_resultadoEsperado() throws Exception {
        mockMvc.perform(get("/api/...")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campo").value("esperado"));
    }
}
```

Decisiones de diseño:

- **Limpieza explícita en `@BeforeEach`** (en vez de `@Transactional` con rollback) porque los flujos de auth y JWT pueden involucrar transacciones internas que no rollbackearían limpiamente. `deleteAll()` en orden descendente (hijas con FK antes que padres) garantiza estado limpio entre tests sin sorpresas.
- **`springSecurity()` aplicado al MockMvcBuilder** activa el `SecurityFilterChain` real. Sin esto, todos los endpoints serían accesibles sin token y los tests de "sin auth → bloqueado" pasarían en falso.
- **Convención de nombres `metodo_condicion_resultadoEsperado`** mantenida desde los unitarios para que el reporte de JUnit sea autoexplicativo.
- **`MockMvc` sobre `TestRestTemplate`** porque el primero no abre puerto HTTP, es más rápido (~10× menos overhead por test) y permite assertions sobre `jsonPath` sin parsear manualmente.

---

## Comparativa con los tests unitarios

| Aspecto | Tests unitarios | Tests de integración |
|---|---|---|
| Velocidad de ejecución | < 30 ms por clase | 100–500 ms por clase (arranca contexto) |
| Cobertura | Lógica de servicios aislada | Flujo completo controller + service + JPA + security + serialización |
| Dependencias | Mockito (sin BD, sin Spring) | H2 + Spring Boot Test |
| Falsos positivos por mocks | Posibles (firma del repo cambia) | No (BD real) |
| Bugs que detectan | Reglas de negocio, validaciones de servicio | Mappings JSON, configuración de seguridad, validaciones de DTO, dialecto SQL, transacciones |
| Cuándo escribir cada uno | Una rama lógica nueva en un servicio | Un endpoint nuevo o un cambio en su contrato HTTP |

Los dos tipos son complementarios: los unitarios detectan regresiones rápidas en la lógica, los de integración detectan errores que solo aparecen al juntar todas las capas (típicamente: serialización LAZY, configuración de seguridad, formato de respuesta de Spring Data).
