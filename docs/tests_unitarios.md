# Tests unitarios — MusicReviews Backend

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| JUnit 5 | 5.x (Spring Boot managed) | Framework de tests |
| Mockito | 5.x (Spring Boot managed) | Mocks de repositorios y dependencias |
| `@ExtendWith(MockitoExtension.class)` | — | Integración JUnit 5 + Mockito sin arrancar Spring |

Los tests son **unitarios puros**: no arrancan el contexto de Spring Boot, no conectan a la base de datos y no levantan el servidor HTTP. Cada servicio se prueba de forma aislada usando mocks que simulan el comportamiento de los repositorios.

---

## Cómo ejecutar los tests

```bash
cd backend/backend

# Ejecutar todos los tests
./mvnw test                                    # Linux/Mac
mvnw.cmd test                                  # Windows

# Ejecutar una clase concreta
./mvnw test -Dtest=ResenaServiceTest

# Ejecutar un test concreto
./mvnw test -Dtest=ResenaServiceTest#crear_conDatosValidos_guardaYDevuelveResena
```

---

## Resumen de cobertura

| Clase de test | Servicio probado | Tests | Resultado |
|---|---|---|---|
| `ResenaServiceTest` | `ResenaService` | 11 | ✅ |
| `UsuarioServiceTest` | `UsuarioService` | 7 | ✅ |
| `FavoritoServiceTest` | `FavoritoService` | 7 | ✅ |
| `ArtistaServiceTest` | `ArtistaService` | 7 | ✅ |
| `EstadisticasServiceTest` | `EstadisticasService` | 5 | ✅ |
| `BackendApplicationTests` | Contexto Spring Boot | 1 | ✅ (requiere BD activa) |

**Total: 38 tests — todos passing ✅**

---

## `ResenaServiceTest` — 11 tests

**Ruta:** `src/test/java/com/musicreviews/backend/service/ResenaServiceTest.java`

Mock utilizado: `ResenaRepository`

### `crear()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `crear_conDatosValidos_guardaYDevuelveResena` | Reseña válida (puntuación 4, sin duplicado) | Guarda y devuelve la reseña |
| `crear_conPuntuacionCero_lanzaExcepcion` | Puntuación = 0 (fuera de rango) | `ReglaNegocioException`: "La puntuación debe estar entre 1 y 5" |
| `crear_conPuntuacionSeis_lanzaExcepcion` | Puntuación = 6 (fuera de rango) | `ReglaNegocioException`: "La puntuación debe estar entre 1 y 5" |
| `crear_conResenaYaExistente_lanzaExcepcion` | El usuario ya reseñó ese álbum | `ReglaNegocioException`: "El usuario ya ha reseñado este álbum" |

### `actualizar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `actualizar_conDatosValidos_actualizaYDevuelveResena` | ID existente, puntuación válida (5) | Actualiza puntuación y comentario |
| `actualizar_conPuntuacionInvalida_lanzaExcepcion` | ID existente, puntuación = 7 | `ReglaNegocioException` — sin guardar |
| `actualizar_conIdInexistente_lanzaExcepcion` | ID que no existe en la BD | `RecursoNoEncontradoException` |

### `eliminar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `eliminar_conIdExistente_eliminaCorrectamente` | ID válido | Llama a `deleteById` |
| `eliminar_conIdInexistente_lanzaExcepcion` | ID que no existe | `RecursoNoEncontradoException`: "Reseña no encontrada" — sin borrar |

### Consultas

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `obtenerPorAlbum_devuelveLista` | `albumId` válido | Delega en `findByAlbumId` y devuelve lista |
| `obtenerPorUsuario_devuelveLista` | `usuarioId` válido | Delega en `findByUsuarioId` y devuelve lista |

---

## `UsuarioServiceTest` — 7 tests

**Ruta:** `src/test/java/com/musicreviews/backend/service/UsuarioServiceTest.java`

Mock utilizado: `UsuarioRepository`

### `guardar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `guardar_conDatosUnicos_guardaYDevuelveUsuario` | Email y username nuevos | Guarda y devuelve el usuario |
| `guardar_conEmailDuplicado_lanzaExcepcion` | Email ya registrado | `ReglaNegocioException`: "Ya existe un usuario con ese email" |
| `guardar_conUsernameDuplicado_lanzaExcepcion` | Username ya registrado | `ReglaNegocioException`: "Ya existe un usuario con ese username" |

### `actualizar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `actualizar_conIdExistente_actualizaCampos` | ID válido, datos nuevos | Actualiza username, fotoPerfil y bio |
| `actualizar_conIdInexistente_lanzaExcepcion` | ID que no existe | `RecursoNoEncontradoException`: "Usuario no encontrado" |

### `eliminar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `eliminar_conIdExistente_eliminaCorrectamente` | ID válido | Llama a `deleteById` |
| `eliminar_conIdInexistente_lanzaExcepcion` | ID que no existe | `RecursoNoEncontradoException`: "Usuario no encontrado" — sin borrar |

---

## `FavoritoServiceTest` — 7 tests

**Ruta:** `src/test/java/com/musicreviews/backend/service/FavoritoServiceTest.java`

Mocks utilizados: `FavoritoRepository`, `EntityManager`

> `EntityManager` es necesario porque `FavoritoService.agregar` llama a `entityManager.refresh()` tras el `save` para forzar la recarga de las relaciones desde la BD (ver `refactorizacion_backend.md`).

### `agregar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `agregar_conFavoritoNuevo_guardaCorrectamente` | Álbum no está en favoritos | Llama a `save` y a `entityManager.refresh` |
| `agregar_conFavoritoYaExistente_lanzaExcepcion` | Álbum ya está en favoritos | `ReglaNegocioException`: "El álbum ya está en favoritos" — sin guardar |

### `eliminar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `eliminar_conFavoritoExistente_eliminaCorrectamente` | Favorito existente | Llama a `deleteByUsuarioIdAndAlbumId` |
| `eliminar_conFavoritoInexistente_lanzaExcepcion` | Favorito que no existe | `RecursoNoEncontradoException`: "El álbum no está en favoritos" — sin borrar |

### `esFavorito()` y `obtenerPorUsuario()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `esFavorito_cuandoExiste_devuelveTrue` | Álbum en favoritos | `true` |
| `esFavorito_cuandoNoExiste_devuelveFalse` | Álbum no en favoritos | `false` |
| `obtenerPorUsuario_devuelveLista` | Usuario con favoritos | Delega en `findByUsuarioId` y devuelve lista |

---

## `ArtistaServiceTest` — 7 tests

**Ruta:** `src/test/java/com/musicreviews/backend/service/ArtistaServiceTest.java`

Mock utilizado: `ArtistaRepository`

### `guardar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `guardar_llamaASaveYDevuelveArtista` | Artista nuevo | Llama a `save` y devuelve el artista |

### `actualizar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `actualizar_conIdExistente_actualizaTodosLosCampos` | ID válido, datos nuevos | Actualiza nombre, foto, biografía, género y país |
| `actualizar_conIdInexistente_lanzaExcepcion` | ID que no existe | `RecursoNoEncontradoException`: "Artista no encontrado" |

### `eliminar()`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `eliminar_conIdExistente_eliminaCorrectamente` | ID válido | Llama a `deleteById` |
| `eliminar_conIdInexistente_lanzaExcepcion` | ID que no existe | `RecursoNoEncontradoException`: "Artista no encontrado" — sin borrar |

### Consultas

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `buscarPorNombre_devuelveLista` | Búsqueda por nombre parcial | Delega en `findByNombreContainingIgnoreCase` |
| `obtenerPorId_cuandoExiste_devuelveArtista` | ID existente | Devuelve `Optional` con el artista |

---

## `EstadisticasServiceTest` — 5 tests

**Ruta:** `src/test/java/com/musicreviews/backend/service/EstadisticasServiceTest.java`

Mocks utilizados: `AlbumRepository`, `ArtistaRepository`, `ResenaRepository`, `UsuarioRepository`

| Nombre del test | Caso cubierto | Resultado esperado |
|---|---|---|
| `obtenerResumen_devuelveTotalesCorrectos` | BD con datos | `ResumenDTO` con totales de álbumes (469), artistas (99), reseñas (10), usuarios (3) |
| `obtenerDistribucionGeneros_devuelveListaDeGeneros` | BD con géneros | Lista de `GeneroEstadisticaDTO` con nombre y total por género |
| `obtenerDistribucionGeneros_sinDatos_devuelveListaVacia` | BD sin álbumes | Lista vacía sin errores |
| `obtenerTopAlbumes_sinResenas_devuelveListaVacia` | BD sin reseñas | Lista vacía sin errores |
| `obtenerTopArtistas_sinResenas_devuelveListaVacia` | BD sin reseñas | Lista vacía sin errores |

---

## `BackendApplicationTests` — 1 test

**Ruta:** `src/test/java/com/musicreviews/backend/BackendApplicationTests.java`

Verifica que el contexto de Spring Boot carga correctamente con todas las dependencias configuradas. **Requiere que la base de datos Aiven esté activa** — es el único test que conecta a la BD real.

```bash
./mvnw test -Dtest=BackendApplicationTests
```

---

## Estructura de los tests

Todos los tests de servicio siguen el mismo patrón:

```
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    @Mock
    private XxxRepository xxxRepository;   ← Mock del repositorio

    @InjectMocks
    private XxxService xxxService;         ← Servicio real con el mock inyectado

    @BeforeEach
    void setUp() { /* prepara objetos de prueba */ }

    @Test
    void nombreDelTest_condicion_resultadoEsperado() {
        // 1. Arrange — configura el comportamiento del mock
        when(xxxRepository.metodo()).thenReturn(valor);

        // 2. Act — ejecuta el método bajo prueba
        Xxx resultado = xxxService.metodo(args);

        // 3. Assert — verifica el resultado
        assertEquals(esperado, resultado);
        verify(xxxRepository).metodo();    // verifica que se llamó al repositorio
    }
}
```

El nombre de cada test sigue la convención `metodo_condicion_resultadoEsperado`, lo que hace que el informe de JUnit sea autoexplicativo sin necesidad de leer el código.
