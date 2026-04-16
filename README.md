# MusicReviews

Aplicación web de reseñas de álbumes musicales, similar a Letterboxd pero orientada a música.
Los usuarios pueden buscar álbumes, escribir reseñas con puntuación del 1 al 5 y guardar favoritos.

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot + Maven |
| Frontend | React + Vite (en desarrollo) |
| Base de datos | MySQL |

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
│       │   ├── SecurityConfig.java
│       │   └── README.md         ← Documentación completa de la API
│       └── src/main/resources/
│           └── application.properties.example
├── database/
│   └── schema.sql                ← Script SQL para crear la base de datos
└── docs/
    ├── diagrama_arquitectura.png
    ├── diagrama_bd.png
    ├── diagrama_clases.png
    ├── diagrama_casos_uso.png
    ├── pruebas_postman.md        ← Registro de pruebas y bugs resueltos
    ├── importacion/              ← Scripts y documentación del proceso de importación desde Spotify
    └── referencias/              ← Enlaces e inspiración para el diseño del frontend
```

---

## Poner en marcha el proyecto

### Requisitos previos
- Java 21
- Maven
- MySQL

### 1. Base de datos
Ejecutar el script en MySQL:
```sql
source database/schema.sql
```

### 2. Configuración
Copiar el archivo de ejemplo y rellenar con tus credenciales:
```
backend/backend/src/main/resources/application.properties.example → application.properties
```

### 3. Arrancar el backend
```bash
cd backend/backend
./mvnw spring-boot:run
```

La API quedará disponible en `http://localhost:8080`.

---

## Documentación

- **API REST completa:** `backend/backend/src/main/java/com/musicreviews/backend/README.md`
- **Pruebas Postman y bugs resueltos:** `docs/pruebas_postman.md`
- **Proceso de importación desde Spotify:** `docs/importacion/proceso_importacion.md`
- **Referencias de diseño:** `docs/referencias/referencias.md`
- **Diagramas:** carpeta `docs/`
