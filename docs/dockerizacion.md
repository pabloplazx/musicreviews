# Dockerización del proyecto MusicReviews

Documento del segundo paso de la **Fase 5 — endurecimiento y despliegue**. Recoge la dockerización completa del proyecto, que permite levantar el stack al completo (base de datos, backend y frontend) mediante un único comando, sin necesidad de instalar Java, Node, MySQL ni ninguna otra dependencia en la máquina destino.

**Fecha:** 30/04/2026 (sesión continuación de la fase 5).

**Punto de partida:** validaciones declarativas incorporadas (paso previo de la fase 5); 50/50 tests unitarios verdes; integración frontend ↔ backend operativa contra Aiven; `.gitignore` comprensivo en ambos repositorios; configuración de credenciales centralizada en `application.properties` (gitignored) y URLs del backend hardcodeadas en los servicios del frontend.

---

## 1. Motivación

La dockerización se priorizó como segundo paso de la fase 5 por tres motivos principales:

1. **Reproducibilidad del entorno.** Una persona externa al proyecto (en este caso, la tutora del TFG) debería ser capaz de levantar la aplicación completa sin necesidad de instalar Java 21, Node.js, MySQL Server, ni ninguna otra dependencia. La dockerización satisface este requisito mediante imágenes que encapsulan tanto el código como su entorno de ejecución.

2. **Cobertura del riesgo de defensa.** La pregunta *"¿cómo se despliega?"* es habitual en defensas de TFG. Disponer de un único comando (`docker compose up --build`) que levante todo el stack constituye una respuesta concreta y demostrable, frente a la alternativa de explicar verbalmente la cadena de instalaciones manuales necesarias.

3. **Base para el despliegue en cloud.** Servicios como Render, Railway o Fly.io aceptan directamente un `Dockerfile` o un `docker-compose.yml` como entrada, lo que reduce considerablemente la complejidad de un eventual despliegue público posterior.

---

## 2. Análisis previo y decisiones de arquitectura

### 2.1 Estrategia de base de datos

Se planteó una elección crítica al inicio: utilizar la instancia gestionada de **Aiven** desde los contenedores, o levantar **MySQL en un contenedor local** dentro del propio `docker-compose`.

| Estrategia | Ventajas | Inconvenientes |
|---|---|---|
| **Aiven desde contenedores** | Datos siempre actualizados; sin trabajo adicional de seed | Requiere credenciales para arrancar; la app no es self-contained; problemas potenciales de conectividad o de IP whitelisting |
| **MySQL en contenedor local** | Self-contained; no requiere credenciales externas; reproducible al 100% | Empieza con base de datos vacía; necesita un seed inicial |

Se optó por la **segunda opción** por su mayor reproducibilidad y porque elimina la dependencia de credenciales externas en el momento de la evaluación. La carencia de datos iniciales se resolvió mediante un dump completo de Aiven (sección 3.6), que se carga automáticamente en el contenedor MySQL la primera vez que arranca.

### 2.2 Número de servicios

Se decidió una arquitectura de **tres servicios** orquestados:

```
docker-compose.yml
├── mysql      (BD MySQL 8 con seed automático)
├── backend    (Spring Boot — JAR ejecutable)
└── frontend   (React buildado, servido por nginx)
```

Esta es la división mínima coherente con la separación lógica del proyecto. No se consideró fusionar componentes en un único contenedor ya que cada servicio tiene un ciclo de vida, restart policy y healthcheck independientes.

### 2.3 Estrategia de configuración

Se siguió la convención de **The Twelve-Factor App** (apartado III: Config), separando la configuración del código:

- Las imágenes Docker no contienen credenciales reales — son genéricas y reutilizables.
- Toda la configuración sensible (URL de la BD, usuario, contraseña, JWT secret, claves de APIs externas) se inyecta en tiempo de ejecución mediante variables de entorno definidas en `docker-compose.yml`.
- En `MySQL` los entornos consumen las variables `MYSQL_*` definidas por la imagen oficial.
- En `Spring Boot` se aprovecha el **relaxed binding** que mapea automáticamente variables de entorno a propiedades (por ejemplo, `SPRING_DATASOURCE_URL` → `spring.datasource.url`). Esto evita modificar `application.properties`.

### 2.4 Estrategia de imágenes

Para ambos servicios construidos (backend y frontend) se adoptó el patrón **multi-stage build**:

| Componente | Stage 1 (builder) | Stage 2 (runtime) |
|---|---|---|
| Backend | `maven:3.9-eclipse-temurin-21` (~600 MB, compila el JAR) | `eclipse-temurin:21-jre` (~250 MB, solo ejecuta el JAR) |
| Frontend | `node:20-alpine` (~150 MB, ejecuta `npm run build`) | `nginx:alpine` (~25 MB, sirve el `dist/`) |

Solo la imagen final (stage 2) sobrevive al build. Las herramientas de compilación se descartan, lo que reduce el tamaño de la imagen final, su superficie de ataque y el tiempo de descarga en despliegues subsiguientes.

---

## 3. Cambios realizados

### 3.1 Archivos `.dockerignore`

Antes de cualquier `Dockerfile`, se crearon dos archivos `.dockerignore` (uno por proyecto) para excluir del contexto de build aquellos archivos que no deben formar parte de la imagen.

**Backend** — `musicreviews/backend/backend/.dockerignore`:

```
target/
.git/
.gitignore
.gitattributes
.idea/
.vscode/
*.iml
*.log
*.tmp
HELP.md
src/main/resources/application.properties
```

**Frontend** — `musicreviews-frontend/.dockerignore`:

```
node_modules/
dist/
.git/
.gitignore
.idea/
.vscode/
npm-debug.log*
*.log
.env
.env.local
.env.*.local
```

#### Justificación de cada bloque

| Patrón | Motivo |
|---|---|
| `target/`, `node_modules/`, `dist/` | Artefactos de build locales. Se regeneran dentro del contenedor con dependencias limpias. Sin esta exclusión, el contexto de build se enviaría con cientos de megabytes innecesarios al daemon de Docker, ralentizando significativamente cada `docker build`. |
| `.git/` | Histórico de versiones. No aporta al runtime y aumenta el tamaño del contexto. |
| `.idea/`, `.vscode/`, `*.iml` | Configuración local de IDEs. |
| `application.properties` | **Crítico desde el punto de vista de seguridad.** Contiene credenciales de Aiven, claves de Spotify/Last.fm y el JWT secret. Su inclusión en la imagen permitiría a cualquier inspector de la imagen extraer todas las credenciales. La configuración se inyectará por variables de entorno (sección 3.5). |
| `.env*` (frontend) | Mismo principio: las variables de entorno locales del frontend no deben acabar en la imagen. |

### 3.2 `Dockerfile` del backend

**Ruta:** `musicreviews/backend/backend/Dockerfile`

```dockerfile
# ============================================================
# STAGE 1 — Builder: compila el JAR con Maven y JDK 21
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiamos primero solo el pom.xml para aprovechar la caché de capas
# de Docker: si no cambia el pom, no se vuelven a descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# STAGE 2 — Runtime: imagen ligera con solo JRE para ejecutar
# ============================================================
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Análisis de las decisiones técnicas

- **`maven:3.9-eclipse-temurin-21`**: imagen oficial de Maven 3.9 sobre JDK 21 de **Eclipse Temurin**, la distribución open-source de OpenJDK mantenida por la Eclipse Foundation a través del proyecto Adoptium. Es la opción de referencia para entornos productivos por su licencia gratuita y soporte continuo, en contraste con Oracle JDK que requiere licencia comercial desde 2019.

- **Optimización de la caché de capas**: la copia separada del `pom.xml` y del código fuente permite a Docker reutilizar la capa de descarga de dependencias (`mvn dependency:go-offline`) cuando solo cambia el código. Esto reduce el tiempo de rebuild de varios minutos a pocos segundos en iteraciones subsiguientes.

- **`-DskipTests`**: los tests unitarios se ejecutan manualmente con `./mvnw test` o desde el IDE. Su ejecución dentro del contenedor incrementaría el tiempo de build sin aportar valor adicional al artefacto producido.

- **Imagen runtime `eclipse-temurin:21-jre`**: contiene únicamente el JRE necesario para ejecutar el JAR, sin Maven ni el código fuente. Reduce el tamaño final de la imagen en aproximadamente un 75 %.

### 3.3 `Dockerfile` y `nginx.conf` del frontend

#### `musicreviews-frontend/Dockerfile`

```dockerfile
# ============================================================
# STAGE 1 — Builder: instala dependencias y compila con Vite
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /build

# La URL del backend la inyecta docker-compose en build time
# (Vite la "incrusta" en el bundle JS — no es runtime)
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ============================================================
# STAGE 2 — Runtime: nginx sirviendo los estáticos del dist/
# ============================================================
FROM nginx:alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80
```

#### `musicreviews-frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback: si la ruta no existe como archivo, devuelve index.html
    # y deja que React Router maneje el routing en el cliente
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache largo para assets con hash en el nombre (Vite los genera así)
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

#### Análisis de las decisiones técnicas

- **`node:20-alpine` y `nginx:alpine`**: ambas imágenes están basadas en **Alpine Linux**, una distribución minimalista (~5 MB) que reduce drásticamente el tamaño final de las imágenes y su superficie de ataque.

- **`ARG VITE_API_URL` en build time**: a diferencia de Spring Boot, donde las variables se leen en runtime, **Vite incrusta las variables `VITE_*` directamente en el bundle JavaScript durante el build**. Por este motivo, la variable se define como `ARG` (recibida desde `docker-compose`) y se exporta como `ENV` antes del `npm run build`, garantizando que esté disponible en el momento exacto en que Vite la necesita.

- **`npm ci` en lugar de `npm install`**: `npm ci` requiere la presencia del `package-lock.json` y produce builds reproducibles al instalar exactamente las versiones bloqueadas, sin actualizaciones automáticas. Es la opción recomendada para entornos de CI/CD y contenedores.

- **SPA fallback con `try_files`**: React Router maneja las rutas en el cliente. Cuando un usuario refresca la página estando en `/catalogo`, el navegador realiza una petición HTTP a `/catalogo` que nginx no puede servir (no existe como archivo). La directiva `try_files $uri $uri/ /index.html` instruye a nginx para servir `index.html` ante cualquier ruta no encontrada, permitiendo que React Router asuma el routing en el cliente.

- **Cache de assets con hash**: Vite añade un hash al nombre de cada archivo JavaScript y CSS generado (por ejemplo, `main-a3f9d.js`). Cuando el contenido cambia, el hash cambia, lo que invalida la caché automáticamente. Esto permite establecer una caché de un año (`expires 1y`) sin riesgo de servir versiones obsoletas, mejorando significativamente el rendimiento percibido en visitas subsiguientes.

### 3.4 Externalización de URLs del backend en el frontend

Hasta este punto, los siete servicios del frontend (`src/services/`) tenían la URL del backend hardcodeada:

```javascript
const API = "http://localhost:8080/api";
```

Este valor se sustituyó en los siete archivos por:

```javascript
const API = import.meta.env.VITE_API_URL;
```

Archivos modificados:

| # | Archivo |
|---|---|
| 1 | `src/services/albumes.js` |
| 2 | `src/services/artistas.js` |
| 3 | `src/services/auth.js` |
| 4 | `src/services/estadisticas.js` |
| 5 | `src/services/favoritos.js` |
| 6 | `src/services/resenas.js` |
| 7 | `src/services/usuarios.js` |

Adicionalmente se creó el archivo `musicreviews-frontend/.env` con el valor para desarrollo local:

```
VITE_API_URL=http://localhost:8080/api
```

Vite carga automáticamente las variables del archivo `.env` cuando se ejecuta `npm run dev`, manteniendo el flujo de desarrollo sin modificaciones. En el contexto de Docker, la variable se inyecta desde `docker-compose.yml` mediante `args` en la sección `build` del servicio `frontend`, sobreescribiendo el valor del `.env` para ese build concreto.

### 3.5 Externalización de las credenciales del backend

El planteamiento inicial contemplaba modificar `application.properties` para que cada propiedad sensible apuntase a una variable de entorno con valor por defecto:

```properties
spring.datasource.url=${MYSQL_URL:jdbc:mysql://...aivencloud.com:11527/defaultdb?useSSL=true&requireSSL=true}
```

Esta aproximación falló durante la verificación local. El parser de placeholders de Spring Boot 4.0.5 no resuelve correctamente la sintaxis `${VAR:default}` cuando el valor por defecto contiene los caracteres `:`, `?` y `&`, todos ellos presentes en una URL JDBC con parámetros de query string. El resultado fue un error de Hibernate al inicio:

```
Driver com.mysql.cj.jdbc.Driver claims to not accept jdbcUrl,
${MYSQL_URL:jdbc:mysql://...aivencloud.com:11527/defaultdb?useS
```

El placeholder no se resolvió y la cadena literal se pasó al driver JDBC.

#### Solución adoptada

Se revirtió `application.properties` a su estado original con valores hardcodeados, y se aprovechó el **relaxed binding** nativo de Spring Boot. Esta característica mapea automáticamente variables de entorno a propiedades del framework sin necesidad de modificar el archivo de configuración:

| Variable de entorno (en `docker-compose.yml`) | Propiedad sobreescrita |
|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` |
| `JWT_SECRET` | `jwt.secret` |
| `JWT_EXPIRATION_MS` | `jwt.expiration-ms` |
| `SPOTIFY_CLIENT_ID` | `spotify.client-id` |
| `SPOTIFY_CLIENT_SECRET` | `spotify.client-secret` |
| `LASTFM_API_KEY` | `lastfm.api-key` |

Resultado: el código de configuración no se toca, la imagen Docker no contiene credenciales (gracias a `.dockerignore`), y `docker-compose.yml` actúa como única fuente de verdad para la configuración del entorno containerizado.

### 3.6 Generación del `seed.sql` desde Aiven

La estrategia de "MySQL local en contenedor" requiere un dump inicial de la base de datos de producción para que el contenedor arranque con datos reales. Este dump debía generarse mediante `mysqldump`, herramienta no instalada localmente al solo disponer de MySQL Shell.

La solución consistió en utilizar **Docker para ejecutar `mysqldump`** en un contenedor temporal sin necesidad de instalar el cliente MySQL en el host. Se creó un script `dump-aiven.sh` que encapsula y documenta el proceso:

```bash
#!/usr/bin/env bash
# Genera un dump completo de la base de datos de Aiven en seed.sql
# Lee las credenciales del archivo .env del directorio padre del proyecto.
# Usa Docker para ejecutar mysqldump sin necesidad de instalar el cliente MySQL local.
# Ejecutar desde Git Bash con: bash dump-aiven.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"
OUTPUT="$SCRIPT_DIR/seed.sql"

# Verificar que existe el archivo .env
if [ ! -f "$ENV_FILE" ]; then
  echo "Error: no se encuentra el archivo .env en $ENV_FILE"
  echo "Copia .env.example a .env y rellena las variables AIVEN_*"
  exit 1
fi

# Cargar todas las variables del .env como variables de entorno
set -a
source "$ENV_FILE"
set +a

# Verificar que las variables necesarias están definidas
: "${AIVEN_HOST:?Variable AIVEN_HOST no definida en .env}"
: "${AIVEN_PORT:?Variable AIVEN_PORT no definida en .env}"
: "${AIVEN_USER:?Variable AIVEN_USER no definida en .env}"
: "${AIVEN_PASSWORD:?Variable AIVEN_PASSWORD no definida en .env}"
: "${AIVEN_DATABASE:?Variable AIVEN_DATABASE no definida en .env}"

echo "Volcando $AIVEN_DATABASE desde Aiven a $OUTPUT ..."

docker run --rm mysql:8 mysqldump \
  --host="$AIVEN_HOST" \
  --port="$AIVEN_PORT" \
  --user="$AIVEN_USER" \
  --password="$AIVEN_PASSWORD" \
  --ssl-mode=REQUIRED \
  --no-tablespaces \
  --skip-lock-tables \
  --single-transaction \
  --column-statistics=0 \
  --set-gtid-purged=OFF \
  "$AIVEN_DATABASE" > "$OUTPUT"

echo "Hecho. Tamaño del dump:"
ls -lh "$OUTPUT"
```

#### Justificación de las flags utilizadas

| Flag | Motivo |
|---|---|
| `--ssl-mode=REQUIRED` | Aiven exige conexiones SSL en su instancia gestionada |
| `--no-tablespaces` | Evita la necesidad del privilegio `PROCESS`, que Aiven no concede en bases de datos gestionadas |
| `--skip-lock-tables` | Evita `LOCK TABLES`, también restringido en Aiven |
| `--single-transaction` | Realiza el dump dentro de una transacción REPEATABLE READ, garantizando un snapshot consistente sin bloquear escrituras concurrentes |
| `--column-statistics=0` | Desactiva la consulta de estadísticas de columnas, evitando incompatibilidades entre versiones de cliente y servidor |
| `--set-gtid-purged=OFF` | Excluye los identificadores GTID del dump. Aiven utiliza GTIDs internamente para replicación entre nodos, pero su inclusión provocaría errores al cargar el dump en una instancia MySQL única que no soporta esa configuración |

Tamaño del dump generado: **163 KB**, conteniendo el esquema completo (incluidas las columnas añadidas por Hibernate mediante `ddl-auto=update` posteriormente al `schema.sql` original) y todos los datos: 481 álbumes importados de Spotify, artistas, usuarios, reseñas y favoritos.

### 3.7 `docker-compose.yml`

**Ruta:** `musicreviews/docker-compose.yml`

```yaml
services:

  mysql:
    image: mysql:8
    container_name: musicreviews-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./database/seed.sql:/docker-entrypoint-initdb.d/01-seed.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 40s

  backend:
    image: ghcr.io/pabloplazx/musicreviews-backend:latest
    build:
      context: ./backend/backend
    container_name: musicreviews-backend
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: 'jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true'
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION_MS: ${JWT_EXPIRATION_MS}
      SPOTIFY_CLIENT_ID: ${SPOTIFY_CLIENT_ID}
      SPOTIFY_CLIENT_SECRET: ${SPOTIFY_CLIENT_SECRET}
      LASTFM_API_KEY: ${LASTFM_API_KEY}
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    image: ghcr.io/pabloplazx/musicreviews-frontend:latest
    build:
      context: ../musicreviews-frontend
      args:
        VITE_API_URL: http://localhost:8080/api
    container_name: musicreviews-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

#### Análisis de las decisiones técnicas

**Servicio `mysql`:**

- **`MYSQL_USER` / `MYSQL_PASSWORD`** crean un usuario no-root con permisos restringidos a la base de datos `musicreviews`. El backend conecta con este usuario en lugar de `root`, siguiendo el principio de mínimo privilegio.

- **`mysql_data:/var/lib/mysql`** define un volumen nombrado donde MySQL persiste sus archivos de datos. Sin esto, cada `docker compose down` provocaría la pérdida total de la información. El volumen sobrevive a reinicios del contenedor y solo se elimina explícitamente con `docker compose down -v`.

- **`./database/seed.sql:/docker-entrypoint-initdb.d/01-seed.sql:ro`** monta el dump de Aiven en una carpeta especial reconocida por la imagen oficial de MySQL. Cualquier archivo `.sql` en esa carpeta se ejecuta automáticamente la primera vez que el contenedor arranca con base de datos vacía. El sufijo `:ro` (read-only) impide que el contenedor modifique el archivo. El prefijo `01-` garantiza el orden de ejecución si en el futuro se añaden más archivos.

- **`healthcheck`** ejecuta `mysqladmin ping` cada 10 segundos. La directiva `start_period: 40s` proporciona tiempo de gracia inicial para que MySQL termine de inicializar y cargar el seed (operación que en la primera ejecución puede tardar 30-40 s). Sin healthcheck, el backend podría intentar conectar antes de que MySQL aceptara conexiones.

**Servicio `backend`:**

- **`build: context: ./backend/backend`** indica que la imagen se construye localmente desde el código fuente del proyecto, en lugar de descargarse de un registry. El context apunta a la carpeta donde se encuentra el `Dockerfile`.

- **Conectividad por nombre de servicio** — la URL JDBC apunta a `mysql:3306`, no a `localhost:3306`. Docker Compose crea automáticamente una red privada donde cada servicio es accesible por su nombre. Esta resolución de nombres internos elimina la necesidad de configurar IPs estáticas.

- **`depends_on: mysql: condition: service_healthy`** retrasa el arranque del backend hasta que el healthcheck del servicio MySQL devuelva resultado positivo, evitando el patrón clásico de fallo-reintento durante la inicialización.

**Servicio `frontend`:**

- **`build: context: ../musicreviews-frontend`** apunta al repositorio del frontend, situado un nivel por encima del repositorio actual. Esta organización en dos repositorios paralelos refleja la separación profesional entre código backend y frontend, comúnmente adoptada en industria.

- **`args: VITE_API_URL: http://localhost:8080/api`** pasa la variable al `ARG` del Dockerfile durante el build. **Es importante destacar que este `localhost` se interpreta desde la perspectiva del navegador del usuario, no desde dentro del contenedor**. El frontend es código JavaScript que se ejecuta en el navegador, el cual sí tiene acceso al puerto 8080 del host (mapeado por el servicio backend).

- **`ports: "80:80"`** expone nginx en el puerto HTTP estándar, permitiendo que la aplicación sea accesible desde la URL más limpia posible (`http://localhost`, sin necesidad de especificar puerto).

### 3.8 Ajuste de configuración CORS en el backend

La configuración inicial de CORS en `SecurityConfig.java` solo permitía como origen `http://localhost:5173` (puerto por defecto del servidor de desarrollo de Vite):

```java
config.setAllowedOrigins(List.of("http://localhost:5173"));
```

Al servir el frontend desde nginx en el puerto 80 dentro del contexto Docker, el origen pasa a ser `http://localhost` (sin puerto explícito). El navegador detectaba esta discrepancia y bloqueaba las respuestas del backend con un error genérico *"Failed to fetch"*.

#### Solución adoptada

Se añadió el nuevo origen a la lista de permitidos, manteniendo el origen anterior para no afectar al desarrollo local con `npm run dev`:

```java
// localhost:5173 -> Vite dev server (npm run dev)
// localhost      -> nginx en Docker (puerto 80, sin sufijo)
config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost"));
```

Esta modificación garantiza que ambos entornos (desarrollo con Vite y producción con Docker) puedan consumir el backend sin restricciones de CORS, sin necesidad de configuraciones por entorno.

### 3.9 Preparación para publicación pública: `.env` y pipeline CI/CD

Tras validar el funcionamiento local del stack, se incorporó una capa adicional de preparación orientada a la publicación del repositorio como código abierto y a la automatización del ciclo de release de las imágenes Docker.

#### 3.9.1 Extracción de credenciales a `.env`

Las credenciales sensibles que estaban literales en `docker-compose.yml` y `dump-aiven.sh` se desplazaron a un archivo `.env` (gitignored) en la raíz del repositorio. Los archivos versionados pasaron a referenciar las variables mediante la sintaxis `${VAR}` que Docker Compose resuelve automáticamente desde el `.env` ubicado en su mismo directorio.

Se incorporó un archivo `.env.example` (versionado) con todas las variables documentadas y valores placeholder:

```ini
# --- MySQL (contenedor local del docker-compose) ---
MYSQL_ROOT_PASSWORD=rootchangeme
MYSQL_DATABASE=musicreviews
MYSQL_USER=musicreviews
MYSQL_PASSWORD=changeme

# --- Backend Spring Boot ---
JWT_SECRET=cambia-esto-por-una-cadena-aleatoria-de-min-32-caracteres
JWT_EXPIRATION_MS=86400000
SPOTIFY_CLIENT_ID=tu-spotify-client-id
SPOTIFY_CLIENT_SECRET=tu-spotify-client-secret
LASTFM_API_KEY=tu-lastfm-api-key

# --- Aiven (solo necesario para regenerar el seed con dump-aiven.sh) ---
AIVEN_HOST=tu-instancia.aivencloud.com
AIVEN_PORT=11527
AIVEN_USER=avnadmin
AIVEN_PASSWORD=tu-aiven-password
AIVEN_DATABASE=defaultdb
```

Flujo de uso en una clonación nueva: copiar `.env.example` a `.env` y rellenar con valores reales antes de ejecutar `docker compose up`. Las credenciales originales se rotaron previamente (Aiven password, Spotify Client Secret, JWT secret), garantizando que cualquier valor expuesto en commits previos del historial quede invalidado.

#### 3.9.2 Pipeline CI/CD: publicación automática de imágenes en GHCR

Se incorporaron dos workflows de **GitHub Actions** (uno por repositorio) en `.github/workflows/docker-publish.yml`. Cada workflow se dispara automáticamente en cada push a la rama por defecto (`main`/`master`) y permite ejecución manual mediante `workflow_dispatch`. Las acciones que realiza cada workflow:

1. Checkout del código del repositorio.
2. Configuración de **Docker Buildx** (constructor moderno con soporte para caché y multi-plataforma).
3. Inicio de sesión en **GitHub Container Registry (`ghcr.io`)** con el token `GITHUB_TOKEN` autoinyectado por el runner (no requiere configuración manual de secretos).
4. Construcción de la imagen Docker correspondiente, con caché de capas almacenada en GitHub Actions para acelerar builds subsiguientes.
5. Publicación de la imagen en `ghcr.io/pabloplazx/musicreviews-backend` o `ghcr.io/pabloplazx/musicreviews-frontend`, etiquetada con `latest` y con el SHA corto del commit (para trazabilidad y posibilidad de pinning a versiones específicas).

El `docker-compose.yml` se actualizó incorporando la directiva `image:` además de la `build:` existente. Esta combinación ofrece dos modos de uso al consumidor del proyecto:

| Comando | Comportamiento |
|---|---|
| `docker compose up` | Descarga las imágenes ya construidas desde GHCR (segundos). Recomendado para evaluación. |
| `docker compose up --build` | Construye las imágenes localmente desde el código fuente. Recomendado durante desarrollo activo. |

**Beneficio para la evaluación del proyecto:** un evaluador puede levantar la pila completa en aproximadamente 30 segundos descargando imágenes preconstruidas, frente a los 5–10 minutos requeridos por un build local en la primera ejecución.

---

## 4. Problemas encontrados durante la implementación

Esta sección recoge los obstáculos que surgieron durante la dockerización y las soluciones adoptadas. Constituye material directamente útil para la defensa del proyecto.

### 4.1 Sintaxis `${VAR:default}` incompatible con valores complejos

**Síntoma:** al inicializar Spring Boot, Hibernate falla con el error:

```
Driver com.mysql.cj.jdbc.Driver claims to not accept jdbcUrl, ${MYSQL_URL:jdbc:mysql://...
```

**Diagnóstico:** la URL JDBC contiene los caracteres `:`, `?` y `&`, todos ellos con significado especial en el parser de placeholders de Spring Boot. El parser interpreta el primer `:` interior como separador adicional en lugar de tratarlo como parte del valor por defecto, dejando la cadena literal sin resolver.

**Solución:** abandono de la sintaxis de placeholders en `application.properties` y adopción del **relaxed binding** nativo de Spring Boot. Este mecanismo mapea automáticamente variables de entorno con convención `MAYUSCULAS_CON_GUIONES_BAJOS` a propiedades con convención `minusculas.con.puntos.o-guiones`, sin requerir modificación alguna del archivo de configuración.

### 4.2 Error de parser YAML en URLs con caracteres especiales

**Síntoma:** Docker Compose rechaza el `docker-compose.yml`:

```
Implicit map keys need to be followed by map values at line 43, column 3
```

**Diagnóstico:** YAML interpreta el carácter `:` como separador entre clave y valor. La URL JDBC (`jdbc:mysql://...:3306/...`) contiene varios `:` interiores que confunden al parser, que intenta tratar partes de la URL como nuevas claves implícitas.

**Solución:** envolver el valor entre comillas simples para forzar a YAML a tratarlo como cadena literal:

```yaml
SPRING_DATASOURCE_URL: 'jdbc:mysql://mysql:3306/musicreviews?useSSL=false&...'
```

Las comillas simples (en contraste con las dobles) no interpretan secuencias de escape, lo que las hace especialmente seguras para cadenas con caracteres especiales.

### 4.3 Conflicto de puerto 3306 en el host

**Síntoma:** Docker Compose falla al arrancar:

```
Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:3306:
listen tcp 0.0.0.0:3306: bind: Only one usage of each socket address ... permitted.
```

**Diagnóstico:** mediante `netstat -ano | findstr :3306` se identificó un proceso (PID 5360, `mysqld.exe`) escuchando en el puerto 3306 del host. Probablemente correspondía a una instalación previa de MySQL Server Community, instalada como servicio de Windows junto con MySQL Workbench o MySQL Shell.

**Solución:** modificación del mapeo de puertos en el servicio `mysql` del `docker-compose.yml`:

```yaml
ports:
  - "3307:3306"   # host 3307 → contenedor 3306
```

Esta solución es preferible a parar el servicio MySQL local porque (1) no afecta al servicio existente y (2) la conectividad entre backend y MySQL dentro de Docker no depende del puerto del host, sino de la red interna de Docker (`mysql:3306`). El mapeo del puerto del host solo se utiliza si se desea conectar a la base de datos del contenedor desde una herramienta externa como MySQL Workbench, en cuyo caso se usaría `localhost:3307`.

### 4.4 Flag obsoleta en `mysqldump` de MySQL 8

**Síntoma:** el script `dump-aiven.sh` falla con:

```
mysqldump: [ERROR] unknown variable 'connect-timeout=30'.
```

**Diagnóstico:** la opción `--connect-timeout` no es reconocida por `mysqldump` en MySQL 8.

**Solución:** eliminación de la flag. La conexión a Aiven es suficientemente estable y rápida como para no requerir un timeout extendido respecto al valor por defecto.

### 4.5 Aviso de GTIDs en el dump

**Síntoma:** `mysqldump` emite el siguiente aviso:

```
Warning: A partial dump from a server that has GTIDs will by default include
the GTIDs of all transactions...
```

**Diagnóstico:** Aiven utiliza GTIDs (Global Transaction Identifiers) para gestionar la replicación entre nodos. El dump generado incluye sentencias `SET @@GLOBAL.GTID_PURGED='...'` que fallarían al ejecutarse en una instancia MySQL única (la del contenedor) que no soporta esa configuración.

**Solución:** añadir la flag `--set-gtid-purged=OFF` al comando `mysqldump`, excluyendo las sentencias relacionadas con GTID del dump generado.

### 4.6 Backend no encuentra propiedades personalizadas

**Síntoma:** primer arranque del backend tras el build, falla con:

```
Could not resolve placeholder 'spotify.client-id' in value "${spotify.client-id}"
```

**Diagnóstico:** `application.properties` se excluyó del contexto de build mediante `.dockerignore` (decisión correcta desde el punto de vista de seguridad), por lo que el archivo no existe dentro de la imagen del backend. Las propiedades estándar de Spring Boot (`spring.datasource.*`) se resuelven mediante variables de entorno con relaxed binding, pero las propiedades personalizadas (`spotify.client-id`, `spotify.client-secret`, `lastfm.api-key`) no estaban definidas en ningún sitio.

**Solución:** definición explícita de las variables de entorno faltantes en el bloque `environment` del servicio `backend` del `docker-compose.yml`:

```yaml
SPOTIFY_CLIENT_ID: <tu-spotify-client-id>
SPOTIFY_CLIENT_SECRET: <tu-spotify-client-secret>
LASTFM_API_KEY: <tu-lastfm-api-key>
```

(Posteriormente, en la sección 3.9, estas variables se desplazaron a un archivo `.env` gitignored para permitir la publicación pública del repositorio.)

El relaxed binding de Spring Boot mapea automáticamente estas variables a las propiedades correspondientes, permitiendo que los beans `SpotifyService` y servicios análogos se construyan correctamente.

### 4.7 CORS bloqueando peticiones del frontend dockerizado

**Síntoma:** tras el arranque exitoso de los tres contenedores, el frontend muestra el error genérico *"Failed to fetch"* al intentar cualquier operación contra el backend.

**Diagnóstico:** la configuración de CORS en `SecurityConfig.java` solo permitía como origen `http://localhost:5173`. El frontend dockerizado se sirve desde nginx en el puerto 80, lo que produce el origen `http://localhost` (sin puerto explícito). El navegador detecta la discrepancia y descarta las respuestas, sin que el backend se entere del bloqueo.

**Solución:** ampliación de la lista de orígenes permitidos en `SecurityConfig.java`, sección 3.8 de este documento.

---

## 5. Resultados

### 5.1 Verificación end-to-end

Tras la resolución de los problemas anteriores, se verificó manualmente el funcionamiento completo del stack dockerizado. Operaciones probadas:

| Operación | Resultado |
|---|---|
| Acceso a `http://localhost` | Página de inicio renderizada con estilos y portadas correctas |
| Login con usuario regular (María) | Autenticación correcta, navegación habilitada |
| Login con usuario administrador | Autenticación correcta, panel de administración accesible |
| Listado del catálogo de álbumes | 481 álbumes cargados desde el seed, paginación funcional |
| Visualización de portadas (URLs de Spotify) | Carga correcta desde el dominio externo |
| Crear una reseña nueva | `POST` exitoso, persistencia confirmada en BD |
| Marcar un álbum como favorito | `POST` exitoso, estado persistente |
| Cambio de estado activo/inactivo de usuario (admin) | `PATCH` exitoso, refleja cambio en el listado |

### 5.2 Verificación de persistencia

Se realizó un ciclo completo de **parar + arrancar** los contenedores para validar la persistencia de datos en el volumen `mysql_data`:

1. Tras crear una reseña y marcar un favorito, se ejecutó `docker compose down`.
2. Se reinició el stack con `docker compose up`.
3. **La reseña creada y el favorito persistieron**, confirmando que:
   - El volumen `mysql_data` retiene correctamente los datos entre reinicios.
   - El `seed.sql` no se reejecuta cuando la base de datos ya tiene datos (el script de inicialización de la imagen oficial de MySQL solo se ejecuta cuando detecta una BD vacía).
   - La sesión JWT vuelve a establecerse correctamente tras el reinicio.

### 5.3 Características del stack final

| Aspecto | Valor |
|---|---|
| Comando para levantar el stack | `docker compose up --build` |
| Tiempo de primer arranque (con descarga de imágenes) | 5-10 min |
| Tiempo de arranque subsiguiente (caché caliente) | ~30 s |
| Tamaño aproximado de imágenes finales | Backend ~250 MB, Frontend ~30 MB, MySQL ~600 MB |
| Puerto de acceso al frontend | `http://localhost` |
| Puerto de acceso al backend (API) | `http://localhost:8080/api` |
| Puerto de acceso a MySQL (para inspección) | `localhost:3307` |
| Persistencia de datos | Volumen Docker `mysql_data` |
| Dependencias requeridas en el host | Únicamente Docker Desktop |

---

## 6. Limitaciones conocidas y trabajo no abordado

- **Credenciales viejas presentes en el historial de git.** Tras la rotación de credenciales y la migración a `.env` (sección 3.9), los valores hoy versionados son únicamente placeholders. No obstante, las credenciales originales (anteriores a la rotación) permanecen en commits anteriores del histórico. Como están invalidadas en sus respectivos servicios, no representan un riesgo de seguridad explotable, pero su presencia en el historial es una huella documentada honestamente. La alternativa sería reescribir el historial con `git filter-repo` o BFG Repo-Cleaner; se considera fuera de alcance y poco útil dada la invalidación previa.

- **Configuración CORS estática.** Los orígenes permitidos están hardcodeados en `SecurityConfig.java`. Para un despliegue público sería necesario permitir el dominio real del frontend (por ejemplo, `https://musicreviews.vercel.app`) mediante una propiedad inyectable. Se considera fuera de alcance de la fase 5.

- **Ausencia de HTTPS.** El stack sirve únicamente HTTP. La incorporación de HTTPS requeriría un proxy reverso adicional (nginx delante del nginx del frontend, o Traefik) con gestión de certificados (Let's Encrypt mediante certbot). Es una mejora propia de un despliegue público real, no del entorno de evaluación local.

- **Sin orquestación productiva.** El stack se basa en `docker compose`, adecuado para desarrollo, demos y pequeños despliegues. Una migración a Kubernetes (mediante manifiestos o Helm Charts) permitiría escalado horizontal, rolling updates y gestión declarativa avanzada, pero excede el alcance de un TFG de DAM.

- **Sin separación de entornos.** Existe un único `docker-compose.yml`. Una organización profesional emplearía archivos separados (`docker-compose.yml`, `docker-compose.dev.yml`, `docker-compose.prod.yml`) que se combinarían según el entorno de despliegue.

---

## 7. Comandos de referencia

### 7.1 Ciclo de vida del stack

| Comando | Función |
|---|---|
| `docker compose up --build` | Construye las imágenes y arranca todos los servicios en primer plano |
| `docker compose up -d` | Arranca los servicios en segundo plano (detached mode) |
| `docker compose down` | Detiene y elimina los contenedores. Conserva el volumen `mysql_data` |
| `docker compose down -v` | Detiene, elimina contenedores y borra el volumen (reset completo) |
| `docker compose ps` | Muestra el estado de los servicios |
| `docker compose logs -f backend` | Sigue los logs del servicio backend en tiempo real |
| `docker compose restart backend` | Reinicia un único servicio |

### 7.2 Regeneración del seed

Si la base de datos de Aiven se actualiza y se desea propagar los nuevos datos al contenedor:

```bash
cd "musicreviews/database"
bash dump-aiven.sh         # Regenera seed.sql desde Aiven
docker compose down -v     # Elimina el volumen para forzar reinicialización
docker compose up          # Arranca con el nuevo seed
```

### 7.3 Inspección de la base de datos del contenedor

Desde MySQL Workbench u otra herramienta cliente:

- **Host:** `localhost`
- **Puerto:** `3307`
- **Usuario:** `musicreviews` (o `root` con la contraseña `rootchangeme`)
- **Contraseña:** `changeme`
- **Base de datos:** `musicreviews`

---

## 8. Guía rápida de despliegue para evaluación

Esta sección recoge los pasos exactos para que un evaluador externo (sin conocimiento previo del proyecto) pueda levantar y probar la aplicación completa. Único requisito previo: **Docker Desktop** instalado y en ejecución.

### 8.1 Obtención del código

Clonar los dos repositorios en directorios paralelos:

```bash
git clone https://github.com/pabloplazx/musicreviews.git
git clone https://github.com/pabloplazx/musicreviews-frontend.git
```

La estructura resultante debe ser:

```
.
├── musicreviews/        ← repositorio principal (backend + docs + docker-compose)
└── musicreviews-frontend/   ← repositorio del frontend
```

### 8.2 Levantar el stack

```bash
cd musicreviews
docker compose up --build
```

La primera ejecución descarga las imágenes base e instala todas las dependencias, lo que puede tardar entre 5 y 10 minutos según conexión y hardware. Las ejecuciones subsiguientes con caché caliente toman aproximadamente 30 segundos.

### 8.3 Acceso a la aplicación

Una vez los logs indiquen que los tres servicios están operativos:

| Recurso | URL |
|---|---|
| Aplicación web (frontend) | `http://localhost` |
| API REST (backend) | `http://localhost:8080/api` |
| Base de datos (vía cliente externo) | `localhost:3307` |

### 8.4 Detener el stack

`Ctrl + C` en la terminal del `docker compose up`, seguido opcionalmente de:

```bash
docker compose down
```

para eliminar los contenedores. Los datos generados durante la sesión persisten en el volumen `mysql_data` y estarán disponibles en el siguiente arranque.

---

## 9. Conclusión

La dockerización del proyecto se cierra cumpliendo los objetivos planteados al inicio de la fase 5:

- **Reproducibilidad total del entorno:** un único comando levanta todo el stack en cualquier máquina con Docker Desktop instalado.
- **Self-contained:** la base de datos viaja con el proyecto en forma de seed, sin dependencia de servicios externos como Aiven para la evaluación.
- **Configuración externalizada:** las credenciales no forman parte de las imágenes; se inyectan en runtime mediante variables de entorno.
- **Imágenes optimizadas:** el patrón multi-stage build reduce el tamaño y la superficie de ataque de las imágenes finales.
- **Persistencia validada:** los datos generados durante el uso de la aplicación sobreviven a reinicios del stack.

El proceso de implementación expuso varios obstáculos técnicos típicos de la dockerización (conflictos de puerto, parsers estrictos, acoplamiento entre `application.properties` y `.dockerignore`, configuración CORS dependiente del entorno), todos resueltos mediante decisiones técnicas justificadas y documentadas. La experiencia adquirida en su resolución constituye material directamente aplicable a futuras dockerizaciones y a la defensa del proyecto.

**Tiempo total invertido:** aproximadamente cuatro horas, incluyendo escritura iterativa de los archivos de configuración, diagnóstico y resolución de los problemas descritos en la sección 4, y verificaciones funcionales intermedias.

**Estado al cierre:** los repositorios `musicreviews` y `musicreviews-frontend` están preparados para que cualquier evaluador clone ambos en directorios paralelos y ejecute `docker compose up --build` para disponer de la aplicación completa funcionando con datos reales.
