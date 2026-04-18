# Migración de la base de datos a Aiven

## Por qué Aiven

La base de datos se migró de MySQL local a **Aiven** (servicio MySQL en la nube, plan gratuito permanente) para:

- No depender de tener MySQL corriendo en local
- Acceder a la BD desde cualquier máquina
- Dar un aspecto más profesional al proyecto (entorno cloud real)

---

## Datos de conexión

| Parámetro | Valor |
|---|---|
| Host | `mysql-1056637b-plaza953pablo-f92f.e.aivencloud.com` |
| Puerto | `11527` |
| Usuario | `avnadmin` |
| Base de datos | `defaultdb` |
| SSL | Requerido |

La contraseña no se incluye aquí — está en `application.properties` (gitignoreado).

---

## Proceso de migración

### 1. Exportar la BD local

Se exportó la base de datos local `musicreviews` desde MySQL Workbench:

- **Server → Data Export**
- Seleccionar la BD `musicreviews`
- Exportar como un único archivo SQL
- Archivo resultante: `exportar_musicreviews.sql`

### 2. Crear el servicio en Aiven

1. Registro en [aiven.io](https://aiven.io)
2. Crear un nuevo servicio: **MySQL → Plan gratuito**
3. Esperar a que el estado pase de `Rebuilding` a `Running` (puede tardar 1-3 minutos)

> El plan gratuito hiberna el servicio tras un tiempo de inactividad. Al reconectarse, hay que esperar a que vuelva a estado `Running` antes de usarlo.

### 3. Configurar la conexión en MySQL Workbench

En Workbench, crear una nueva conexión con los datos de Aiven (ver tabla anterior). SSL debe estar activado.

### 4. Importar el dump SQL

Con la conexión abierta en Workbench:

1. **File → Open SQL Script** → seleccionar `exportar_musicreviews.sql`
2. Ejecutar el script completo (rayo ⚡)

> Alternativa por terminal (si Workbench da problemas con la redirección en CMD):
> ```bash
> "C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" \
>   --host=<host> --port=<port> --user=avnadmin \
>   --password=<password> --ssl-mode=REQUIRED defaultdb \
>   < exportar_musicreviews.sql
> ```

### 5. Verificar la importación

```sql
SELECT COUNT(*) FROM artista;   -- debe dar ~99
SELECT COUNT(*) FROM album;     -- debe dar ~469
```

### 6. Configurar el backend

Actualizar `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://<host>:<port>/defaultdb?useSSL=true&requireSSL=true
spring.datasource.username=avnadmin
spring.datasource.password=<password>
```

Arrancar el backend y verificar con Postman: `GET /api/artistas`.

---

## Estado final

- ~99 artistas y ~469 álbumes migrados correctamente
- Backend conectado a Aiven y funcionando
- MySQL local ya no es necesario para el desarrollo
