# MusicReviews

Aplicación web de reseñas de álbumes musicales, similar a Letterboxd pero orientada a música.
Los usuarios pueden buscar álbumes, escribir reseñas con puntuación de 0.5 a 5 estrellas, y guardar favoritos. Incluye panel de administración para gestión de catálogo y usuarios.

Proyecto desarrollado como Trabajo de Fin de Grado del CFGS DAM (IES Rosa Chacel).

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 4.0.5 + Maven + Spring Security 7 + JWT + Bean Validation |
| Frontend | React 19 + Vite 6 + Tailwind CSS 4 + React Router 7 — [`musicreviews-frontend`](https://github.com/pabloplazx/musicreviews-frontend) (repo separado) |
| Base de datos | MySQL 8 (en contenedor con datos preseed para evaluación local; Aiven para desarrollo activo) |
| Contenerización | Docker + Docker Compose (multi-stage builds, healthchecks, persistencia con volúmenes) |
| Integración continua | GitHub Actions — publicación automática de imágenes en GHCR en cada push a `main` |

---

## Estructura del proyecto

```
musicreviews/
├── .github/workflows/
│   └── docker-publish.yml        ← CI: build y publicación de imágenes en GHCR
├── backend/
│   └── backend/                  ← Proyecto Spring Boot
│       ├── Dockerfile            ← Multi-stage build (Maven → JDK)
│       ├── .dockerignore
│       ├── src/main/java/com/musicreviews/backend/
│       │   ├── model/            ← Entidades JPA con Bean Validation
│       │   ├── repository/       ← Acceso a la BD (Spring Data JPA)
│       │   ├── service/          ← Lógica de negocio
│       │   ├── controller/       ← Endpoints REST
│       │   ├── security/         ← JWT (JwtUtil, JwtFilter, UserDetailsServiceImpl)
│       │   ├── exception/        ← Excepciones tipadas + GlobalExceptionHandler
│       │   ├── dto/              ← DTOs con validaciones declarativas
│       │   ├── SecurityConfig.java
│       │   └── README.md         ← Documentación completa de la API
│       ├── src/test/java/        ← 50 tests unitarios (JUnit 5 + Mockito)
│       └── src/main/resources/
│           └── application.properties.example
├── musicreviews-frontend/        ← (clonar aparte, repo independiente)
│   ├── Dockerfile                ← Multi-stage build (Node → nginx)
│   ├── nginx.conf                ← Configuración con SPA fallback
│   └── ...
├── database/
│   ├── schema.sql                ← Esquema de tablas (referencia)
│   ├── seed.sql                  ← Dump de datos preseed (cargado por el contenedor MySQL)
│   ├── dump-aiven.sh             ← Script para regenerar seed.sql desde Aiven
│   └── seed_data.py              ← Script Python alternativo para sembrar datos vía API
├── docs/                         ← Documentación técnica completa (ver sección al final)
├── docker-compose.yml            ← Orquestación de los 3 servicios (mysql + backend + frontend)
├── .env.example                  ← Plantilla de variables de entorno
└── README.md                     ← Este archivo
```

---

## Cómo levantar el proyecto

Hay dos formas de poner en marcha la aplicación. La **opción A (Docker)** es la recomendada para evaluación o demostración. La **opción B (local)** es la habitual durante el desarrollo activo.

### Opción A — Docker (recomendada)

Esta opción levanta toda la pila (base de datos preseed + backend + frontend) con un único comando, sin necesidad de instalar Java, Node ni MySQL en la máquina anfitriona.

#### Requisitos previos
- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado y en ejecución
- Git

#### Pasos

1. Clonar los dos repositorios en directorios paralelos:

   ```bash
   git clone https://github.com/pabloplazx/musicreviews.git
   git clone https://github.com/pabloplazx/musicreviews-frontend.git
   ```

   La estructura resultante debe ser:
   ```
   .
   ├── musicreviews/
   └── musicreviews-frontend/
   ```

2. Entrar en el repositorio principal y preparar las variables de entorno:

   ```bash
   cd musicreviews
   cp .env.example .env
   ```

   Editar `.env` con valores reales (si solo se desea evaluar, los placeholders del ejemplo son funcionales para todo excepto la regeneración del seed con `dump-aiven.sh`).

3. Levantar el stack:

   ```bash
   docker compose up --build
   ```

   La primera ejecución descarga las imágenes base e instala dependencias (5–10 minutos según conexión y hardware). Los arranques subsiguientes con caché caliente toman aproximadamente 30 segundos.

4. Acceder a la aplicación:

   | Recurso | URL |
   |---|---|
   | Aplicación web (frontend) | http://localhost |
   | API REST (backend) | http://localhost:8080/api |
   | Base de datos MySQL (cliente externo) | `localhost:3307` (usuario y contraseña en `.env`) |

5. Detener la pila:

   ```bash
   # Ctrl+C en la terminal del docker compose up, y opcionalmente:
   docker compose down            # conserva el volumen mysql_data (datos persisten)
   docker compose down -v         # borra el volumen (reset completo, recarga seed la próxima vez)
   ```

#### Variante: usar imágenes preconstruidas desde GHCR (sin build local)

Las imágenes del backend y frontend se publican automáticamente en GitHub Container Registry en cada push a `main`. Si se desea evitar el build local, basta con omitir la flag `--build`:

```bash
docker compose up
```

Docker descarga las imágenes desde `ghcr.io/pabloplazx/musicreviews-backend:latest` y `ghcr.io/pabloplazx/musicreviews-frontend:latest` (segundos en lugar de minutos).

Documentación completa del proceso de dockerización (decisiones, problemas encontrados y soluciones) en [`docs/dockerizacion.md`](docs/dockerizacion.md).

---

### Opción B — Desarrollo local (sin Docker)

Esta opción es la habitual durante el desarrollo activo, ya que permite hot-reload del frontend y depuración del backend desde el IDE.

#### Requisitos previos
- Java 21
- Maven (incluido como wrapper `mvnw`, no requiere instalación)
- Node 20+
- Cuenta en [Aiven](https://aiven.io) con servicio MySQL activo, o MySQL 8 local

#### Configuración del backend

1. Copiar el archivo de ejemplo y rellenar con credenciales reales:

   ```
   backend/backend/src/main/resources/application.properties.example
       → application.properties
   ```

   Para Aiven:
   ```properties
   spring.datasource.url=jdbc:mysql://<host>:<port>/defaultdb?useSSL=true&requireSSL=true
   spring.datasource.username=avnadmin
   spring.datasource.password=TU_PASSWORD_AIVEN
   ```

   Las credenciales se obtienen en el panel de Aiven → servicio MySQL → pestaña *Connection information*.

2. Arrancar el backend:

   ```bash
   cd backend/backend
   ./mvnw spring-boot:run        # Linux/macOS
   ./mvnw.cmd spring-boot:run    # Windows
   ```

   La API queda disponible en `http://localhost:8080`.

#### Configuración del frontend

1. Crear un `.env` en la raíz del repositorio del frontend:

   ```
   VITE_API_URL=http://localhost:8080/api
   ```

2. Instalar dependencias y arrancar el servidor de desarrollo:

   ```bash
   cd musicreviews-frontend
   npm install
   npm run dev
   ```

   La aplicación queda disponible en `http://localhost:5173`.

---

## Tests

El backend dispone de **50 tests unitarios** que cubren la lógica de negocio principal y la verificación de propiedad de recursos (incorporada en la auditoría de seguridad del 28/04/2026):

| Clase de test | Tests | Cobertura |
|---|---|---|
| `ResenaServiceTest` | 17 | CRUD + validaciones + duplicados + verificación de propiedad |
| `UsuarioServiceTest` | 11 | Registro, actualización con verificación, eliminación con verificación |
| `FavoritoServiceTest` | 9 | CRUD de favoritos + verificación de propiedad |
| `ArtistaServiceTest` | 7 | CRUD de artistas |
| `EstadisticasServiceTest` | 5 | Rankings, top álbumes, estadísticas generales |
| `BackendApplicationTests` | 1 | Carga del contexto de Spring Boot |

Ejecución:

```bash
cd backend/backend
./mvnw test
```

---

## Manejo de errores

Todos los endpoints devuelven errores en formato JSON uniforme gracias al `GlobalExceptionHandler`:

```json
{
    "timestamp": "2026-04-30T12:34:56.789",
    "status": 400,
    "mensaje": "Errores de validación",
    "errores": {
        "email": "Formato de email inválido",
        "password": "La contraseña debe tener al menos 6 caracteres"
    }
}
```

| Código | Significado |
|---|---|
| `400` | Validación fallida (email mal formado, campo vacío, regla de negocio violada) |
| `401` | No autenticado (sin token JWT o token inválido) |
| `403` | Autenticado pero sin permisos sobre el recurso (no es propietario ni ADMIN) |
| `404` | Recurso no encontrado |

---

## Documentación

Toda la documentación técnica del proceso de desarrollo está en `docs/`. Se recomienda leerla en este orden si se desea seguir la evolución del proyecto:

| Documento | Contenido |
|---|---|
| [`tests_unitarios.md`](docs/tests_unitarios.md) | Cobertura y convenciones de los 50 tests |
| [`pruebas_postman.md`](docs/pruebas_postman.md) | Pruebas manuales de API + bugs documentados y resueltos |
| [`migracion_aiven.md`](docs/migracion_aiven.md) | Migración de MySQL local a Aiven (cloud) |
| [`seguridad_autenticacion.md`](docs/seguridad_autenticacion.md) | JWT, BCrypt, CORS, protección de credenciales |
| [`refactorizacion_backend.md`](docs/refactorizacion_backend.md) | Optimizaciones aplicadas al backend |
| [`frontend.md`](docs/frontend.md) | Proceso de desarrollo del frontend (fases 3 y 4) |
| [`integracion.md`](docs/integracion.md) | Fase 4: integración frontend ↔ backend, AuthContext, bugs corregidos |
| [`auditoria_seguridad.md`](docs/auditoria_seguridad.md) | Auditoría de seguridad y endurecimiento del backend |
| [`validaciones_declarativas.md`](docs/validaciones_declarativas.md) | Bean Validation: anotaciones, manejador de errores y verificación |
| [`dockerizacion.md`](docs/dockerizacion.md) | Dockerización completa: arquitectura, decisiones, problemas y soluciones |
| [`tailwind-guide.md`](docs/tailwind-guide.md) | Catálogo de clases Tailwind utilizadas |
| `referencias/`, `diseño/`, `importacion/` | Material de apoyo del diseño y la importación desde Spotify |

API REST completa: [`backend/backend/src/main/java/com/musicreviews/backend/README.md`](backend/backend/src/main/java/com/musicreviews/backend/README.md)

**Anexo:** registro cronológico personal del desarrollo (estilo bitácora) en [`docs/anexos/diario_bitacora.md`](docs/anexos/diario_bitacora.md). Mantenido como apoyo a la defensa del proyecto.

---

## Estado del proyecto

- **Backend completado y desplegable.** 50/50 tests verdes. API REST funcional con JWT, **tres capas de seguridad** (autenticación + roles + verificación de propiedad), validaciones declarativas (Bean Validation), integración con Spotify y Last.fm, panel admin con endpoints específicos, búsqueda unificada (`?q=`) y orden parametrizable (`?sort=`).
- **Frontend completado.** 15 pantallas implementadas e integradas al 100% con el backend. CRUD completo de reseñas, gestión de favoritos, panel admin funcional.
- **Stack dockerizado.** Tres contenedores (mysql + backend + frontend) orquestados con Docker Compose, persistencia mediante volúmenes nombrados, healthchecks y publicación automática de imágenes en GHCR.

### Limitaciones conocidas

Documentadas honestamente con justificación de alcance:

- Sin verificación de email en el registro
- Sin cambio de contraseña ni recuperación
- Sin subida real de archivos (URL como solución de transición)
- Sin funcionalidad de "seguir artista"
- Sin orden "Mejor valorados" en el catálogo (requiere agregado de reseñas a nivel de BD)
- Sin tests de controller con `MockMvc` (cobertura mediante tests de service + verificación manual con Postman)
- Sin HTTPS en el stack Docker (apropiado para entorno local; un despliegue público requeriría proxy reverso con certificados)

Detalle completo de causas y justificaciones en [`docs/integracion.md`](docs/integracion.md), [`docs/auditoria_seguridad.md`](docs/auditoria_seguridad.md) y [`docs/dockerizacion.md`](docs/dockerizacion.md).
