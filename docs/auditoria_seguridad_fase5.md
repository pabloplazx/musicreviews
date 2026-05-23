# Auditoría de Seguridad — Fase 5

**Fecha:** 2026-05-23  
**Alcance:** Backend Spring Boot + Frontend React (MusicReviews)  
**Método:** Revisión estática de código + análisis de configuración  

---

## Resumen ejecutivo

Se han identificado **3 hallazgos corregidos** en esta auditoría, **1 falso positivo** descartado y **varios hallazgos aceptados** con justificación de alcance para un TFG. No se detectaron inyecciones SQL ni XSS activos gracias al uso de JPA (consultas parametrizadas) y React (escapado automático de JSX).

---

## Hallazgos corregidos

### C1 — Endpoints de Spotify accesibles sin autenticación
**Riesgo:** Alto  
**Archivo:** `SecurityConfig.java:79`  
**Descripción:** Todos los endpoints `/api/spotify/**` tenían `permitAll()`, permitiendo que cualquier usuario anónimo disparara importaciones masivas desde la API de Spotify, agotando la cuota de la aplicación o provocando denegación de servicio.  
**Corrección:** Cambiado a `.hasRole("ADMIN")`. Solo un administrador puede importar artistas y álbumes.  
**Estado:** ✅ Corregido en commit `auditoria(security): fase 5`

---

### C2 — `GET /api/usuarios/{id}` expone email y datos sensibles
**Riesgo:** Medio  
**Archivo:** `UsuarioController.java:34`  
**Descripción:** El endpoint devolvía el objeto `Usuario` completo, incluyendo el email del usuario, accesible sin autenticación por cualquiera que conociese el ID numérico.  
**Corrección:** El endpoint ahora devuelve `UsuarioResumenDTO` (solo `id`, `username`, `fotoPerfil`), igual que el resto de endpoints públicos de usuario.  
**Estado:** ✅ Corregido en commit `auditoria(security): fase 5`

---

### C3 — `fechaUltimoLogin` serializada en respuestas públicas
**Riesgo:** Bajo  
**Archivo:** `Usuario.java:60`  
**Descripción:** El campo `fechaUltimoLogin` se incluía en las respuestas JSON, permitiendo a cualquiera conocer cuándo un usuario se conectó por última vez. Facilita análisis de actividad y puede suponer un problema de privacidad.  
**Corrección:** Añadida anotación `@JsonIgnore`. El campo se sigue actualizando internamente pero nunca se serializa.  
**Estado:** ✅ Corregido en commit `auditoria(security): fase 5`

---

## Falsos positivos descartados

### F1 — Credenciales en control de versiones
**Descripción:** El auditor señaló `application.properties` como expuesto en git.  
**Realidad:** Existe un commit previo (`Eliminar application.properties del seguimiento de git`) que lo eliminó del tracking. El archivo solo existe en local y en el servidor, nunca en el repositorio público. El `.gitignore` de Maven excluye este archivo. Solo está versionado `application.properties.example` con valores de plantilla.  
**Estado:** ✅ No aplica

---

## Hallazgos aceptados (fuera de alcance del TFG)

| ID | Hallazgo | Justificación |
|---|---|---|
| A1 | CSRF deshabilitado | Estándar en APIs REST con JWT stateless. El frontend envía siempre `Authorization: Bearer`, nunca cookies de sesión. |
| A2 | Sin rate limiting en login | Requiere infraestructura adicional (Redis + bucket4j). Justificado para producción real, fuera de alcance del TFG. Documentado como limitación conocida. |
| A3 | Sin refresh tokens | Trabajo en curso — Tarea #2 del proyecto. |
| A4 | HTTPS no forzado en Spring | Delegado al proxy nginx que ya gestiona TLS con certificado Let's Encrypt en zentimes.es. Spring opera detrás del proxy en red interna. |
| A5 | `System.out.println` en SpotifyService | Los endpoints de Spotify ahora requieren ADMIN. Las trazas no exponen datos de usuarios finales, solo nombres de artistas en importaciones administrativas. |
| A6 | Contraseña mínima 6 caracteres | Límite razonable para un TFG. En producción real se recomendaría 12 caracteres con política de complejidad. |
| A7 — | Sin auditoría de acciones admin | Fuera de alcance. El historial de cambios queda registrado en los logs de Docker. |

---

## Protecciones activas verificadas

| Protección | Mecanismo | Estado |
|---|---|---|
| Inyección SQL | JPA + Hibernate (consultas parametrizadas) | ✅ Activo |
| XSS | React escapa JSX automáticamente | ✅ Activo |
| Contraseñas | BCrypt con salt automático | ✅ Activo |
| Autenticación | JWT firmado con HMAC-SHA512 | ✅ Activo |
| Autorización | Spring Security por roles (USER/ADMIN) | ✅ Activo |
| Verificación de propiedad | Service verifica dueño antes de modificar | ✅ Activo |
| CORS | Lista blanca de orígenes | ✅ Activo |
| Datos sensibles en git | `application.properties` en `.gitignore` | ✅ Activo |
| Contraseña en JSON | `@JsonProperty(WRITE_ONLY)` | ✅ Activo |
| TLS en producción | nginx + Let's Encrypt en zentimes.es | ✅ Activo |
| Importación Spotify | Restringida a ADMIN | ✅ Corregido |
| Email en API pública | `UsuarioResumenDTO` sin email | ✅ Corregido |
| Fecha último login | `@JsonIgnore` | ✅ Corregido |

---

## Limitaciones conocidas y documentadas

- Sin verificación de email en el registro (en desarrollo — Tarea #2)
- Sin refresh tokens (en desarrollo — Tarea #2)
- Sin bloqueo de cuenta tras intentos fallidos de login
- Sin auditoría persistente de acciones administrativas
- Sin rate limiting por IP

Estas limitaciones están documentadas honestamente y son habituales en proyectos de este alcance. No suponen riesgo crítico dado el volumen de usuarios actual de la plataforma.

---

## Historial de auditorías anteriores

| Fecha | Documento |
|---|---|
| 2026-04-28 | [`auditoria_seguridad.md`](auditoria_seguridad.md) — verificación de propiedad en operaciones de modificación |
