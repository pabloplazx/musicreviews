# MusicReviews

Aplicación web de reseñas de álbumes musicales, similar a Letterboxd pero orientada a música.
Los usuarios pueden buscar álbumes, escribir reseñas con puntuación del 1 al 5 y guardar favoritos.

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot + Maven + Spring Security + JWT |
| Frontend | React + Vite (en desarrollo) |
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
    ├── pruebas_postman.md        ← Registro de pruebas y bugs resueltos
    ├── migracion_aiven.md        ← Proceso de migración de BD local a Aiven (MySQL cloud)
    ├── seguridad_autenticacion.md ← JWT, BCrypt, CORS y protección de contraseñas
    ├── refactorizacion_backend.md ← Optimizaciones y refactorizaciones del backend
    ├── importacion/              ← Scripts y documentación del proceso de importación desde Spotify
    └── referencias/              ← Enlaces e inspiración para el diseño del frontend
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

El backend cuenta con **37 tests unitarios** (+ 1 test de contexto) que cubren la lógica de negocio principal:

| Clase de test | Tests | Qué cubre |
|---|---|---|
| `ResenaServiceTest` | 11 | Crear, actualizar, eliminar reseñas — validaciones y duplicados |
| `UsuarioServiceTest` | 7 | Registro, actualización, login, desactivación |
| `FavoritoServiceTest` | 7 | Agregar, eliminar, listar favoritos |
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
- **Pruebas Postman y bugs resueltos:** `docs/pruebas_postman.md`
- **Proceso de importación desde Spotify:** `docs/importacion/proceso_importacion.md`
- **Migración de la BD a Aiven:** `docs/migracion_aiven.md`
- **Seguridad y autenticación JWT:** `docs/seguridad_autenticacion.md`
- **Refactorización y optimización del backend:** `docs/refactorizacion_backend.md`
- **Referencias de diseño:** `docs/referencias/referencias.md`
- **Diagramas:** carpeta `docs/`
