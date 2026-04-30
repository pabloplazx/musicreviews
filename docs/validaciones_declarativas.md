# Validaciones declarativas con Bean Validation

Documento del primer paso de la **Fase 5 — endurecimiento y despliegue**. Recoge la incorporación de validaciones declarativas (Jakarta Bean Validation) al backend para rechazar entradas malformadas antes de que alcancen la capa de servicio.

**Fecha:** 30/04/2026 (inicio de la fase 5).

**Punto de partida:** 50/50 tests unitarios verdes; auditoría de seguridad cerrada; integración frontend ↔ backend operativa. La auditoría detectó como hallazgo de prioridad media que el backend no validaba el formato de las entradas: era posible registrar un usuario con un email malformado, una contraseña excesivamente corta o un nombre de usuario vacío, y la petición se aceptaba hasta que la base de datos o la capa de servicio la rechazaba con un mensaje genérico.

---

## 1. Justificación

La fase 5 contempla un conjunto de cinco mejoras posibles. La incorporación de validaciones declarativas se priorizó como primer paso por tres motivos:

1. **Coste reducido (1-2 h estimadas)**, lo que permite cerrar una mejora completa antes de abordar tareas de mayor envergadura como la dockerización o el despliegue.
2. **Cierra el contrato del backend antes de empaquetarlo.** Detectar una validación faltante una vez construida la imagen Docker obligaría a reconstruirla; resulta preferible consolidar primero la lógica.
3. **Cobertura de un riesgo previsible en defensa.** Sin esta capa, ante la pregunta sobre el comportamiento frente a entradas inválidas, la respuesta debería reconocer que el backend las acepta y delega el rechazo a otra capa. Con validación declarativa, el backend devuelve un código `400 Bad Request` con un mensaje específico por campo.

---

## 2. Análisis previo y decisión de alcance

La planificación inicial proponía aproximadamente treinta anotaciones distribuidas en las entidades `Usuario`, `Resena`, `Album` y `Artista`. Antes de aplicar cambios se revisaron los métodos `actualizar` de cada `service` para determinar cómo se comporta cada `PUT`, dado que añadir restricciones del tipo `@NotBlank` a una entidad utilizada como `@RequestBody` invalida los `PUT` que no envían el conjunto completo de campos.

Resumen del análisis:

| Entidad | Comportamiento del PUT en su service | Validable a nivel de entidad |
|---|---|---|
| `Album` | Sustituye todos los campos (`titulo`, `portada`, `fechaLanzamiento`, `genero`, `descripcion`, `artista`) | Sí — el cliente envía siempre el objeto completo |
| `Artista` | Sustituye todos los campos (`nombre`, `foto`, `biografia`, `genero`, `pais`) | Sí — el cliente envía siempre el objeto completo |
| `Resena` | Actualiza únicamente `puntuacion` y `comentario` | Parcialmente — solo en los campos siempre presentes |
| `Usuario` | Actualiza únicamente `username`, `fotoPerfil` y `bio` | No — anotar `email` o `password` invalidaría el PUT del perfil, ya que el cliente no envía esos campos |

La aplicación literal de la regla *"añadir treinta anotaciones en las entidades"* habría provocado regresiones en los `PUT` de `Usuario` y `Resena`. La estrategia adoptada distingue entre los **DTOs específicos del boundary de creación** y las **entidades reutilizadas como cuerpo de petición**.

### Decisiones finales

| Boundary | Anotaciones aplicadas | Justificación |
|---|---|---|
| `RegisterRequest` (DTO) | `@NotBlank` + `@Size(3-50)` en `username`; `@NotBlank` + `@Email` + `@Size(max=100)` en `email`; `@NotBlank` + `@Size(min=6)` en `password` | Único punto de creación de usuarios. Procede aplicar validación estricta. |
| `LoginRequest` (DTO) | `@NotBlank` + `@Email` en `email`; `@NotBlank` en `password` | Permite descartar peticiones con formato inválido sin consultar la base de datos. |
| `Album` (entidad) | `@NotBlank` + `@Size(max=150)` en `titulo`; `@NotNull` en `artista` | El service realiza una sustitución completa, por lo que las restricciones se cumplen en cada petición. |
| `Artista` (entidad) | `@NotBlank` + `@Size(max=100)` en `nombre` | Mismo razonamiento que `Album`. |
| `Resena` (entidad) | `@NotNull` + `@DecimalMin("0.5")` + `@DecimalMax("5.0")` en `puntuacion` | Único campo presente tanto en `POST` como en `PUT`. Evita una `NullPointerException` en el método `validarPuntuacion` del service cuando el valor llega como `null`. Las relaciones `usuario` y `album` no se anotan al no leerse en el `PUT`. |
| `Usuario` (entidad) | Ninguna | El `PUT` de perfil envía únicamente `username`, `bio` y `fotoPerfil`. Validar `email` o `password` provocaría una regresión. La validación de creación se delega a `RegisterRequest`. |

Total: **15 anotaciones** distribuidas mediante un análisis caso por caso, en lugar de una aplicación uniforme.

---

## 3. Cambios realizados

### 3.1 Dependencia (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

Hibernate Validator, implementación de referencia de Jakarta Bean Validation, se incorpora como dependencia transitiva. Spring Boot detecta su presencia en el classpath y activa el procesamiento de `@Valid` automáticamente, sin configuración adicional.

### 3.2 Anotaciones en DTOs

`dto/RegisterRequest.java` y `dto/LoginRequest.java` se transforman de POJOs sin restricciones a DTOs anotados. Ejemplo (`RegisterRequest`):

```java
@NotBlank(message = "El nombre de usuario es obligatorio")
@Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
private String username;

@NotBlank(message = "El email es obligatorio")
@Email(message = "Formato de email inválido")
@Size(max = 100, message = "El email no puede superar los 100 caracteres")
private String email;

@NotBlank(message = "La contraseña es obligatoria")
@Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
private String password;
```

### 3.3 Anotaciones en entidades

Se anotaron los campos identificados en la tabla anterior. Las anotaciones se ubican antes del `@Column` para mantener el criterio *Jakarta Validation primero, JPA después*. Las anotaciones JPA (`length`, `nullable`) se conservan: la validación de Bean Validation y las restricciones de la base de datos constituyen capas independientes y complementarias.

### 3.4 Aplicación de `@Valid` en los controllers

Cada `@RequestBody` cuya entidad incorpora restricciones se prefijó con `@Valid`. Sin esta anotación, las restricciones de Bean Validation no se procesan. Ficheros modificados:

- `AuthController.register` — `@Valid @RequestBody RegisterRequest`
- `AuthController.login` — `@Valid @RequestBody LoginRequest`
- `AlbumController.crear` y `actualizar` — `@Valid @RequestBody Album`
- `ArtistaController.crear` y `actualizar` — `@Valid @RequestBody Artista`
- `ResenaController.crear` y `actualizar` — `@Valid @RequestBody Resena`

`UsuarioController.actualizar` no incorpora `@Valid` porque la entidad `Usuario` carece de anotaciones de validación, conforme a la decisión documentada en la sección 2.

### 3.5 Manejador de errores en `GlobalExceptionHandler`

Cuando una validación `@Valid` falla, Spring lanza `MethodArgumentNotValidException`. En ausencia de un manejador específico, el cliente recibe un `400` con un cuerpo descriptivo pero poco estructurado. Se incorporó un manejador que devuelve un JSON consistente con el formato del resto de errores de la aplicación:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
    Map<String, String> errores = new LinkedHashMap<>();
    e.getBindingResult().getFieldErrors().forEach(
            err -> errores.put(err.getField(), err.getDefaultMessage())
    );
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now().toString());
    body.put("status", 400);
    body.put("mensaje", "Errores de validación");
    body.put("errores", errores);
    return ResponseEntity.badRequest().body(body);
}
```

Estructura de la respuesta:

```json
{
  "timestamp": "2026-04-30T10:25:19.649580800",
  "status": 400,
  "mensaje": "Errores de validación",
  "errores": {
    "email": "Formato de email inválido",
    "password": "La contraseña debe tener al menos 6 caracteres"
  }
}
```

El campo `errores` es un mapa `campo → mensaje` que permite al frontend señalizar el control concreto que ha fallado, sin necesidad de parsear el mensaje textual.

---

## 4. Resultados

### 4.1 Tests unitarios

```
[INFO] Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

50/50 tests verdes antes y después del cambio. Las anotaciones se aplican en la capa de controller mediante `@Valid`, mientras que los tests unitarios operan a nivel de service, por lo que no se ven afectados. Esta independencia confirma que la separación de capas está correctamente delimitada.

### 4.2 Verificación end-to-end con `curl`

Backend ejecutándose contra Aiven; cinco peticiones lanzadas contra los endpoints públicos:

| Test | Petición | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | `POST /api/auth/register` con `email = "patata"` | 400, error en `email` | `{"errores":{"email":"Formato de email inválido"}}` |
| 2 | `POST /api/auth/register` con `password = "123"` | 400, error en `password` | `{"errores":{"password":"La contraseña debe tener al menos 6 caracteres"}}` |
| 3 | `POST /api/auth/register` con `username = ""` | 400, error en `username` | `{"errores":{"username":"El nombre de usuario debe tener entre 3 y 50 caracteres"}}` |
| 4 | `POST /api/auth/register` con los tres campos inválidos | 400, los tres errores | `{"errores":{"password":"...","email":"...","username":"..."}}` |
| 5 | `POST /api/auth/login` con `email = "noesemail"` | 400, error en `email` | `{"errores":{"email":"Formato de email inválido"}}` |

El test 4 confirma que la respuesta agrega todos los errores en una única petición, en lugar de detenerse en el primero. Este comportamiento permite al frontend marcar simultáneamente todos los controles inválidos.

---

## 5. Limitaciones conocidas y trabajo no abordado

- **`Usuario` sin validación a nivel de entidad.** El boundary de creación (`RegisterRequest`) sí está validado; el de actualización (`PUT` de perfil) no, por aceptar payloads parciales. La refactorización a un DTO específico (`ActualizarPerfilRequest`) con `@NotBlank` en `username` constituye la solución técnicamente correcta, pero se considera fuera del alcance de este paso. La auditoría no clasificó este punto como crítico.
- **Ausencia de tests automatizados de validación.** La verificación se realiza de forma manual mediante `curl`. La cobertura mediante tests `MockMvc` se contempla en la tarea de tests de integración del plan de fase 5.
- **`Resena.usuario` y `Resena.album` sin `@NotNull`.** Se omitieron deliberadamente al no leerse en el `PUT`. La validación efectiva se realiza en `ResenaService.crear`, que verifica la propiedad y la existencia de las relaciones. Una petición `POST` sin `usuario.id` provocaría una `NullPointerException`; se acepta esta limitación al no corresponder a ningún flujo de cliente legítimo.

---

## 6. Conclusión

Cambio acotado en superficie (una dependencia, un manejador y quince anotaciones) con un valor concreto:

- El backend rechaza entradas malformadas con un mensaje útil antes de alcanzar la capa de servicio.
- El frontend recibe un JSON estructurado por campo, directamente aplicable a la señalización de formularios.
- Se cubre un riesgo previsible en la defensa del proyecto.
- Tiempo invertido: aproximadamente una hora, dentro de la estimación inicial.

**Siguiente paso de la fase 5:** dockerización del backend y del frontend mediante `docker-compose`, con el objetivo de garantizar la reproducibilidad del entorno de ejecución.
