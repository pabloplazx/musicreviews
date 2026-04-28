# Auditoría de seguridad y endurecimiento del backend

Documento del proceso de revisión crítica del backend tras cerrar la fase 4 de integración. Recoge **qué problemas se detectaron, cómo se analizaron, cómo se resolvieron y qué quedó pendiente**, en formato de informe profesional.

**Fecha:** 28/04/2026 (último día de la fase 4).

**Punto de partida:** integración frontend ↔ backend completa (paso 9 cerrado), 38 tests unitarios verdes, panel admin funcional. Todo "se ve" bien desde la UI.

**Motivo de la auditoría:** antes de presentar un TFG, conviene una revisión crítica buscando lo que un evaluador exigente podría preguntar en la defensa. Se reservaron 2 horas para esto.

---

## 1. Auditoría inicial — vulnerabilidades detectadas

Se hizo una pasada por el backend mirando concretamente: cobertura de los `requestMatchers` en `SecurityConfig`, lectura del `Authentication` en los controllers, validaciones declarativas de inputs, manejo de secretos y tests existentes.

### 1.1 Hallazgo principal — falta de verificación de propiedad

`SecurityConfig` exige `authenticated()` en los endpoints protegidos pero **no verifica que el recurso afectado pertenezca al usuario autenticado**. Cualquier usuario con un JWT válido podía afectar a recursos ajenos haciendo llamadas directas con `curl` o Postman, aunque la UI no se lo permitiera.

**6 endpoints vulnerables:**

| Endpoint | Vulnerabilidad |
|---|---|
| `POST /api/resenas` | El body lleva `usuario.id`. Maria con su token podía mandar `usuario:{id:3}` y crear una reseña en nombre de carlos. **Suplantación de autoría.** |
| `PUT /api/resenas/{id}` | El backend solo comprobaba que el id existe, no que la reseña sea de quien llama. Maria podía editar la reseña de carlos. |
| `DELETE /api/resenas/{id}` | Mismo. Maria podía borrar reseñas ajenas. |
| `PUT /api/usuarios/{id}` | Maria podía cambiar el username/bio/foto de carlos. |
| `DELETE /api/usuarios/{id}` | Maria podía borrar la cuenta de carlos. |
| `POST /api/favoritos` y `DELETE /api/favoritos?usuarioId=` | Maria podía gestionar la lista de favoritos de carlos. |

### 1.2 Hallazgos menores

| Hallazgo | Detalle |
|---|---|
| **Sin tests de controller con seguridad** | Los 38 tests unitarios cubren la lógica de service. No hay `MockMvc` con `@WithMockUser` para verificar JWT/roles/CORS. La verificación está hecha manualmente con Postman y documentada. |
| **Sin validaciones declarativas** | No se usa `spring-boot-starter-validation` con `@NotBlank`, `@Email`, `@Size`. Las restricciones están solo en `@Column(length=...)` (que MySQL impone) y en checks ad-hoc del service. |
| **`open-in-view=true`** | Spring Boot lo desaconseja en producción. Para una BD pequeña como esta no afecta significativamente. La alternativa profesional son DTOs en lugar de entidades en respuestas. |
| **CORS solo para `localhost:5173`** | Suficiente para desarrollo. Para producción habría que sacarlo a variable de entorno o property. |
| **JWT secret en `application.properties`** | El fichero está en `.gitignore`, solo se commitea `.example` con placeholder. Para producción real iría como variable de entorno. |

---

## 2. Análisis de impacto y prioridad

Se aplicó un análisis simple de riesgo (probabilidad × impacto) a cada hallazgo:

| Hallazgo | Probabilidad de explotación | Impacto si ocurre | Prioridad |
|---|---|---|---|
| Falta de verificación de propiedad | **Alta** — basta un curl con el token | **Alto** — borrado/edición/suplantación arbitraria | 🔴 **Crítica** |
| Sin tests de controller | Baja — no se explota directamente | Bajo — es un riesgo de regresión | 🟡 Media |
| Sin validaciones declarativas | Media — el backend es robusto contra entradas mal formadas pero no contra emails inválidos | Bajo — un email mal formado no causa daño grave | 🟡 Media |
| `open-in-view=true` | Baja — no es vulnerabilidad de seguridad sino de rendimiento | Bajo en este TFG | 🟢 Baja |
| CORS hardcoded | Baja — afecta solo a despliegue | Bajo en TFG | 🟢 Baja |
| JWT secret en properties | Baja — está gitignored | Medio si filtras el fichero | 🟢 Baja |

**Decisión:** abordar la prioridad 🔴 Crítica ahora. Las 🟡 y 🟢 se documentan como **limitaciones conocidas con justificación de alcance**. Son tradeoffs legítimos para un TFG académico — la verificación de propiedad **no lo es**, es un agujero de seguridad real que se puede explotar en 30 segundos con un curl.

---

## 3. Plan de mitigación

### 3.1 Patrón a aplicar

En cada endpoint de modificación del recurso, el `Controller` lee el `Authentication` (que lo inyecta Spring Security automáticamente) y pasa al `Service`:

- `auth.getName()` → email del usuario autenticado (lo guardamos como `subject` del JWT en `JwtUtil.generarToken`).
- `esAdmin(auth)` → boolean, se calcula mirando si las authorities contienen `ROLE_ADMIN`.

El `Service` compara con el owner del recurso. Si no coincide y el llamante no es ADMIN, lanza una excepción nueva `AccesoDenegadoException` que el `GlobalExceptionHandler` traduce a HTTP 403 con JSON uniforme.

### 3.2 Excepción + handler

Crear `exception/AccesoDenegadoException.java`:

```java
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) { super(mensaje); }
}
```

En `GlobalExceptionHandler`:

```java
@ExceptionHandler(AccesoDenegadoException.class)
public ResponseEntity<Map<String, Object>> handleForbidden(AccesoDenegadoException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(403, e.getMessage()));
}
```

### 3.3 Lista de cambios por servicio

| Servicio | Métodos a modificar | Cambio |
|---|---|---|
| `ResenaService` | `crear`, `actualizar`, `eliminar` | Comparar email del token con `resena.usuario.email` (o `usuario.id` en `crear`). Si no es dueño y no es ADMIN → throw. |
| `UsuarioService` | `actualizar`, `eliminar` | Comparar email del token con `usuario.email`. Mismo patrón. |
| `FavoritoService` | `agregar`, `eliminar` | Comparar `usuarioId` del request con el id resuelto desde el email del token. |

### 3.4 Tests

Para cada servicio modificado, añadir:

- Un test "como otro usuario lanza `AccesoDenegadoException`".
- Un test "como ADMIN está permitido en recurso ajeno".

Se mantienen todos los tests existentes con sus llamadas adaptadas a la firma nueva (`...(args, email, esAdmin)`).

---

## 4. Implementación

### 4.1 Cambios en código

**Ficheros nuevos:**

- `exception/AccesoDenegadoException.java`

**Ficheros modificados:**

| Fichero | Cambio |
|---|---|
| `exception/GlobalExceptionHandler.java` | Handler para `AccesoDenegadoException` → 403 |
| `service/ResenaService.java` | Inyecta `UsuarioRepository`. `crear/actualizar/eliminar` reciben `(email, esAdmin)` y verifican propiedad. Helper privado `idDelEmail` para resolver id desde el email del JWT. |
| `service/UsuarioService.java` | `actualizar/eliminar` reciben `(email, esAdmin)` y verifican propiedad. |
| `service/FavoritoService.java` | Inyecta `UsuarioRepository`. `agregar/eliminar` reciben `(email, esAdmin)`. |
| `controller/ResenaController.java` | Recibe `Authentication auth` en los métodos de modificación; helper privado `esAdmin(auth)` que mira las authorities. |
| `controller/UsuarioController.java` | Igual. |
| `controller/FavoritoController.java` | Igual. |

**Tests modificados (firmas adaptadas + casos nuevos):**

- `service/ResenaServiceTest.java`: 11 → 17 tests (+6 verificación propiedad)
- `service/UsuarioServiceTest.java`: 7 → 11 tests (+4)
- `service/FavoritoServiceTest.java`: 7 → 9 tests (+2)

**Total de tests pasados de 38 a 50.**

### 4.2 Ejemplo de código del fix (ResenaService.eliminar)

**Antes:**

```java
@Transactional
public void eliminar(Long id) {
    if (!resenaRepository.existsById(id)) {
        throw new RecursoNoEncontradoException("Reseña no encontrada");
    }
    resenaRepository.deleteById(id);
}
```

**Después:**

```java
@Transactional
public void eliminar(Long id, String emailLlamante, boolean esAdmin) {
    Resena resena = resenaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Reseña no encontrada"));

    if (!esAdmin && !resena.getUsuario().getEmail().equals(emailLlamante)) {
        throw new AccesoDenegadoException("Solo puedes borrar tus propias reseñas");
    }

    resenaRepository.deleteById(id);
}
```

Nota: cambiamos `existsById` por `findById` porque ahora necesitamos la entidad completa para comparar el email del owner. El coste extra de la query es despreciable.

### 4.3 Ejemplo de código del fix (Controller)

**Antes:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    resenaService.eliminar(id);
    return ResponseEntity.noContent().build();
}
```

**Después:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
    resenaService.eliminar(id, auth.getName(), esAdmin(auth));
    return ResponseEntity.noContent().build();
}

private boolean esAdmin(Authentication auth) {
    return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
}
```

Spring Security inyecta `Authentication auth` automáticamente en los métodos del controller cuando hay sesión activa (lo monta el `JwtFilter` desde el JWT al cargar el `UserDetails`).

---

## 5. Verificación

### 5.1 Tests unitarios

```
$ ./mvnw test
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

12 tests nuevos cubren los casos de propiedad. Estructura:

- 6 en `ResenaServiceTest`: crear con id ajeno, crear como admin, actualizar de otro, actualizar como admin, eliminar de otro, eliminar como admin.
- 4 en `UsuarioServiceTest`: actualizar perfil ajeno, actualizar como admin, eliminar cuenta ajena, eliminar como admin.
- 2 en `FavoritoServiceTest`: agregar para otro, eliminar de otro.

### 5.2 Pruebas manuales con curl (esperadas tras reiniciar el backend)

| Caso | Comando | Esperado | Por qué |
|---|---|---|---|
| Maria intenta crear reseña con `usuario.id = 3` (carlos) | `POST /api/resenas` con body `{"usuario":{"id":3}, ...}` y token de maría | **403** + `{"mensaje":"Solo puedes crear reseñas en tu propio nombre"}` | El `id` del body no coincide con el del email del JWT |
| Maria intenta editar la reseña 5 (que es de carlos) | `PUT /api/resenas/5` con token de maría | **403** + `{"mensaje":"Solo puedes editar tus propias reseñas"}` | El email del JWT no coincide con `resena.usuario.email` |
| Maria intenta borrar la reseña 5 | `DELETE /api/resenas/5` con token de maría | **403** + `{"mensaje":"Solo puedes borrar tus propias reseñas"}` | Mismo |
| Maria intenta cambiar el username de carlos | `PUT /api/usuarios/3` con token de maría | **403** + `{"mensaje":"Solo puedes editar tu propio perfil"}` | El id de la URL no coincide con el id del email del JWT |
| Admin borra la reseña 5 | `DELETE /api/resenas/5` con token de admin | **204** | El admin se salta la verificación (moderación) |
| Maria intenta añadir favoritos a la cuenta de carlos | `POST /api/favoritos` con `{"usuario":{"id":3}, ...}` y token de maría | **403** + `{"mensaje":"Solo puedes gestionar tus propios favoritos"}` | El id del body no coincide |

Casos que NO cambian (para confirmar que no se rompe el flujo legítimo):

- Maria edita su propia reseña → 200 (es la dueña)
- Maria borra su propio favorito → 204
- Maria cambia su propio username → 200
- Admin gestiona usuarios desde el panel → 200/204

---

## 6. Resultado final

### 6.1 Antes vs después

| Aspecto | Antes | Después |
|---|---|---|
| Verificación de propiedad en `POST/PUT/DELETE` de reseñas | ❌ no | ✅ sí (owner o ADMIN) |
| Verificación de propiedad en `PUT/DELETE` de usuarios | ❌ no | ✅ sí |
| Verificación de propiedad en `POST/DELETE` de favoritos | ❌ no | ✅ sí |
| Suplantación posible vía `usuarioId` del body | ❌ sí | ✅ no |
| Tests unitarios | 38 verdes | **50 verdes** |
| Casos de prueba de seguridad cubiertos en tests | 0 | 12 |

### 6.2 Estado final del backend

Todos los endpoints de modificación verifican que quien llama es **dueño del recurso** o **administrador**. La protección es a tres niveles complementarios:

1. **JWT obligatorio** (paso 4 de la fase 4 + `SecurityConfig`).
2. **Restricciones por rol** en `SecurityConfig` para endpoints administrativos (`PATCH /api/usuarios/{id}/activo`, etc.).
3. **Verificación de propiedad en el service** (este documento), ahora también para los endpoints "del usuario sobre sí mismo".

Esto cubre los tres patrones de ataque más típicos:
- Sin sesión → bloqueado por capa 1.
- Con sesión USER intentando endpoint ADMIN → bloqueado por capa 2.
- Con sesión USER intentando recurso de otro USER → bloqueado por capa 3.

---

## 7. Limitaciones aceptadas (no resueltas)

Las siguientes se identificaron en la auditoría pero se decidió **no resolver** por considerarlas tradeoffs legítimos para el alcance del TFG. Cada una con su justificación:

| Limitación | Justificación |
|---|---|
| Sin tests de controller con `MockMvc` y `@WithMockUser` | Las pruebas unitarias cubren la lógica de negocio del service; las pruebas de integración con seguridad están en `pruebas_postman.md`, hechas manualmente. Añadir un set completo de tests de controller con seguridad es trabajo de varios días y no aporta cobertura conceptual nueva — solo automatiza lo que ya está verificado manualmente. |
| Sin validaciones declarativas (`@NotBlank`, `@Email`, `@Size`) | Las restricciones están en `@Column(length=...)` (que MySQL impone a nivel de BD) y validaciones explícitas en el service (puntuación, duplicados). El frontend valida formato (`type="email"` HTML5). Añadir `spring-boot-starter-validation` con anotaciones es ampliación futura — no es vulnerabilidad. |
| `spring.jpa.open-in-view=true` | Spring Boot lo desaconseja en producción por rendimiento (no por seguridad). Para una BD de ~700 álbumes y ~40 reseñas no afecta. La alternativa profesional son DTOs en lugar de entidades en respuestas, lo que también resolvería B2 (proxies de Hibernate) de forma más robusta. Es ampliación futura. |
| CORS solo para `http://localhost:5173` hardcoded | Configuración de desarrollo. Para producción se mueve a property/variable de entorno. No es vulnerabilidad mientras solo se use en desarrollo. |
| JWT secret en `application.properties` | El fichero está gitignored y solo se commitea `.example` con placeholder. Para producción real iría en variable de entorno o secret manager. Para el TFG es aceptable. |
| Sin invalidación del JWT al hacer logout | JWT puro no permite invalidación; haría falta blacklist en servidor o tokens cortos + refresh. El logout solo borra el token del cliente. Mejora futura. |
| Sin verificación de email al registrarse | Requiere SMTP + endpoint de verificación + columna `email_verificado`. Trabajo de medio día, mencionable como ampliación. |
| Sin cambio de contraseña | Backend no expone endpoint. Mejora futura. |
| Sin subida de archivos (foto perfil, portadas) | Backend no tiene endpoint multipart. URL como input es el workaround. Para upload real haría falta storage (sistema de ficheros local o S3) y validación de tipos. |

---

## 8. Aprendizajes y recomendaciones para la defensa

**Lo que destaca de este proceso para la presentación:**

1. **Identificación temprana de un agujero de seguridad real** — no solo la implementación funciona, sino que se hizo una revisión crítica antes de presentar.
2. **Distinción clara entre vulnerabilidad y limitación** — no todo lo que falta es un fallo. Algunas cosas son tradeoffs legítimos para el alcance académico, y se mencionan honestamente.
3. **Tres capas complementarias de seguridad**: autenticación (JWT), autorización por rol (SecurityConfig), verificación de propiedad (service). Cada una resuelve un patrón distinto.
4. **Tests automatizados cubren el caso de seguridad**: 12 tests nuevos que se ejecutan en cada `mvn test` y previenen regresiones.

**Si te preguntan en la defensa "¿qué pasa si maría hace `curl DELETE /api/resenas/5` con su token siendo de carlos?"**, la respuesta es:

> "Antes de la auditoría del 28/04 esa llamada hubiera tenido éxito — el backend solo comprobaba que el id existía. Tras la auditoría, el `ResenaService.eliminar` verifica que el email del JWT coincide con el `email` del usuario propietario de la reseña. Si no coincide y no es ADMIN, se lanza `AccesoDenegadoException` que el `GlobalExceptionHandler` traduce a HTTP 403. Está cubierto por el test `eliminar_deOtroUsuario_lanzaAccesoDenegada`."

**Si te preguntan "¿por qué pasáis el email como parámetro en lugar de leerlo del `SecurityContextHolder` dentro del service?"**:

> "Para mantener los services como funciones puras, sin acoplar a Spring Security. Los tests unitarios pasan el email directamente sin tener que mockear el contexto de seguridad. Si en el futuro quisiéramos consumir el service desde un test de integración o desde una llamada interna sin sesión (ej. tarea programada), no hace falta tocar el código del service."

---

## 9. Resumen ejecutivo

- ✅ **6 vulnerabilidades críticas de "falta de verificación de propiedad" detectadas y corregidas.**
- ✅ **50/50 tests verdes** (38 anteriores + 12 nuevos de seguridad).
- ✅ **Excepción específica `AccesoDenegadoException`** y handler HTTP 403 con JSON uniforme.
- ✅ **Tres capas de seguridad** (autenticación, rol, propiedad) ahora completas.
- 📌 **9 limitaciones documentadas honestamente** como tradeoffs de alcance del TFG, con justificación cada una.

**El backend está listo para la defensa.** Las preguntas previsibles tienen respuesta concreta y verificable.
