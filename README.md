# MusicReviews

Aplicación web de reseñas de álbumes musicales, similar a Letterboxd pero orientada a música.
Los usuarios pueden buscar álbumes, escribir reseñas con puntuación del 1 al 5 y guardar favoritos.

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 4.0.5 + Maven + Spring Security 7 + JWT |
| Frontend | React 19 + Vite 6 + Tailwind CSS 4 + React Router 7 — [`musicreviews-frontend`](https://github.com/pabloplazx/musicreviews-frontend) (repo separado) |
| Base de datos | MySQL en la nube — [Aiven](https://aiven.io) (plan gratuito permanente) |

---

## Estructura del proyecto

```
MusicReviews_TFG/
├── backend/
│   └── backend/                  ← Proyecto Spring Boot
│       ├── src/main/java/com/musicreviews/backend/
│       │   ├── model/            ← Entidades JPA (tablas de la BD)
│       │   ├── repository/       ← Acceso a la BD (Spring Data JPA)
│       │   ├── service/          ← Lógica de negocio
│       │   ├── controller/       ← Endpoints REST
│       │   ├── security/         ← JWT (JwtUtil, JwtFilter, UserDetailsServiceImpl)
│       │   ├── exception/        ← Excepciones tipadas + GlobalExceptionHandler
│       │   ├── dto/              ← Objetos de transferencia (register, login, auth response)
│       │   ├── SecurityConfig.java
│       │   └── README.md         ← Documentación completa de la API
│       ├── src/test/java/com/musicreviews/backend/
│       │   └── service/          ← 50 tests unitarios (JUnit 5 + Mockito) — 38 base + 12 de la auditoría de seguridad
│       └── src/main/resources/
│           └── application.properties.example
├── database/
│   ├── schema.sql                ← Script SQL para crear la base de datos
│   └── seed_data.py              ← Script Python para cargar datos de ejemplo vía API
└── docs/
    ├── diagrama_arquitectura.png
    ├── diagrama_bd_nuevo.png
    ├── diagrama_clases.png
    ├── diagrama_casos_uso.png
    ├── diario_bitacora.md         ← Bitácora semanal de todo el desarrollo (fases 1-4)
    ├── tests_unitarios.md         ← Documentación completa de los 50 tests unitarios
    ├── auditoria_seguridad.md     ← Auditoría de seguridad (28/04/2026): proceso, mitigación, resultado
    ├── pruebas_postman.md         ← Pruebas Postman, bugs encontrados y resueltos (incl. B1-B5 de fase 4)
    ├── migracion_aiven.md         ← Proceso de migración de BD local a Aiven (MySQL cloud)
    ├── seguridad_autenticacion.md ← JWT, BCrypt, CORS y protección de contraseñas
    ├── refactorizacion_backend.md ← Optimizaciones y refactorizaciones del backend
    ├── frontend.md                ← Proceso de desarrollo del frontend (fases 3 y 4)
    ├── integracion.md             ← Fase 4: integración frontend ↔ backend, AuthContext, bugs B1-B5
    ├── tailwind-guide.md          ← Guía de clases Tailwind utilizadas en el frontend
    ├── diseño/                    ← Capturas Figma y especificación del design system
    ├── importacion/               ← Scripts y documentación del proceso de importación desde Spotify
    └── referencias/               ← Enlaces e inspiración para el diseño del frontend
```

---

## Poner en marcha el proyecto

### Requisitos previos
- Java 21
- Maven
- Cuenta en [Aiven](https://aiven.io) con un servicio MySQL activo (o MySQL local)

### 1. Base de datos

La base de datos está alojada en **Aiven** (MySQL cloud). No es necesario instalar MySQL localmente.

Si quisieras usar MySQL local, puedes ejecutar el script de esquema:
```sql
source database/schema.sql
```

### 2. Configuración
Copiar el archivo de ejemplo y rellenar con tus credenciales de Aiven:
```
backend/backend/src/main/resources/application.properties.example → application.properties
```

Configuración para Aiven:
```properties
spring.datasource.url=jdbc:mysql://<host>:<port>/defaultdb?useSSL=true&requireSSL=true
spring.datasource.username=avnadmin
spring.datasource.password=TU_PASSWORD_AIVEN
```

Los datos de conexión (host, port, password) se encuentran en el panel de Aiven → tu servicio MySQL → pestaña **Connection information**.

### 3. Arrancar el backend
```bash
cd backend/backend
./mvnw spring-boot:run
```

La API quedará disponible en `http://localhost:8080`.

### 4. Cargar datos de ejemplo (opcional)

El script `database/seed_data.py` carga 5 usuarios, 30 reseñas y 25 favoritos a través de la API. Requiere que el backend esté corriendo:

```bash
python -X utf8 database/seed_data.py
```

> La flag `-X utf8` es necesaria en Windows para evitar errores de codificación con caracteres especiales.

---

## Tests

El backend cuenta con **50 tests unitarios** que cubren la lógica de negocio principal y la verificación de propiedad de recursos (auditoría de seguridad del 28/04):

| Clase de test | Tests | Qué cubre |
|---|---|---|
| `ResenaServiceTest` | 17 | CRUD + validaciones + duplicados + verificación de propiedad (owner/admin) |
| `UsuarioServiceTest` | 11 | Registro, actualización (con verificación), eliminación (con verificación) |
| `FavoritoServiceTest` | 9 | CRUD de favoritos + verificación de propiedad |
| `ArtistaServiceTest` | 7 | CRUD de artistas |
| `EstadisticasServiceTest` | 5 | Rankings, top álbumes, estadísticas generales |
| `BackendApplicationTests` | 1 | Carga del contexto de Spring Boot |

```bash
cd backend/backend
./mvnw test
```

---

## Manejo de errores

Todos los endpoints devuelven errores en formato JSON uniforme gracias al `GlobalExceptionHandler`:

```json
{
    "timestamp": "2026-04-20T12:34:56.789",
    "status": 404,
    "mensaje": "Álbum no encontrado"
}
```

- `404` — recurso no encontrado (álbum, artista, usuario, reseña, favorito)
- `400` — regla de negocio violada (reseña duplicada, puntuación inválida, email ya en uso...)
- `401` — no autenticado
- `403` — autenticado pero sin permisos

---

## Documentación

- **API REST completa:** `backend/backend/src/main/java/com/musicreviews/backend/README.md`
- **Bitácora semanal:** `docs/diario_bitacora.md`
- **Tests unitarios:** `docs/tests_unitarios.md`
- **Pruebas Postman y bugs resueltos:** `docs/pruebas_postman.md`
- **Frontend (proceso, decisiones de diseño):** `docs/frontend.md`
- **Integración frontend ↔ backend (fase 4):** `docs/integracion.md`
- **Auditoría de seguridad y endurecimiento:** `docs/auditoria_seguridad.md`
- **Guía de clases Tailwind:** `docs/tailwind-guide.md`
- **Proceso de importación desde Spotify:** `docs/importacion/proceso_importacion.md`
- **Migración de la BD a Aiven:** `docs/migracion_aiven.md`
- **Seguridad y autenticación JWT:** `docs/seguridad_autenticacion.md`
- **Refactorización y optimización del backend:** `docs/refactorizacion_backend.md`
- **Referencias de diseño:** `docs/referencias/referencias.md`
- **Diagramas:** carpeta `docs/`

---

## Estado del proyecto

- **Backend:** completado y desplegable. **50/50 tests verdes** (38 anteriores + 12 nuevos de la auditoría de seguridad). API REST funcional con JWT, **tres capas de seguridad** (autenticación + roles + verificación de propiedad), integración Spotify y Last.fm, panel admin con endpoints específicos (PATCH `/usuarios/{id}/activo`), búsqueda unificada (`?q=`), orden parametrizable (`?sort=`). Ver `docs/diario_bitacora.md` para el historial.
- **Frontend:** 15 pantallas implementadas + integración 100% terminada con el backend (repo `musicreviews-frontend`). Fase 4 cerrada en 9 pasos + auditoría — desde AuthContext hasta panel admin funcional con CRUD de usuarios, artistas y moderación de reseñas. Ver `docs/integracion.md`.

**Auditoría de seguridad (28/04/2026):** revisión crítica del backend antes de la defensa. Detectó y corrigió 6 vulnerabilidades de "falta de verificación de propiedad" (un usuario autenticado podía modificar recursos ajenos vía API directa). Tras el fix, todos los endpoints de modificación verifican que quien llama es **dueño del recurso o ADMIN**. Detalle completo del proceso (auditoría → análisis → mitigación → verificación → resultado) en `docs/auditoria_seguridad.md`.

**Limitaciones conocidas y documentadas honestamente** (todas con justificación de alcance del TFG): sin verificación de email, sin cambio de contraseña, sin subida real de archivos (URL como workaround), sin "seguir artista", sin orden "Mejor valorados" en catálogo (requiere agregado de reseñas), sin tests de controller con MockMvc (cobertura por tests de service + Postman manual). Ver `docs/integracion.md § 13` y `docs/auditoria_seguridad.md § 7` para la lista completa con causas.
