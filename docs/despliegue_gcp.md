# Despliegue en Google Cloud (zentimes.es)

Documento del **paso de despliegue público** de la fase 5. Recoge la puesta en producción del stack MusicReviews en una VM de Google Cloud, accesible desde Internet en `https://zentimes.es` con certificado Let's Encrypt y renovación automática.

**Fecha:** 30/04/2026.

**Punto de partida:**
- Stack ya dockerizado (ver `dockerizacion.md`).
- VM `kenetg-light` existente en GCP (proyecto `kenetg-com-backend-dbmysql`, zona `europe-west1-b`), Debian 12, 2 vCPU / 2 GB RAM / 15 GB disco. IP externa fija `34.14.42.119`.
- Dominio `zentimes.es` registrado en Hostinger.
- Registro DNS A `@ → 34.14.42.119` (TTL 14400) creado por el usuario antes de empezar.
- Sin Docker instalado en la VM. Sin firewall configurado para 80/443. Frontend con `VITE_API_URL=http://localhost:8080/api` (incompatible con producción).

---

## 1. Motivación

El despliegue público sustituye al `docker compose up` local como demostración del proyecto:

1. **URL pública estable** para que el tribunal y la tutora puedan probar la aplicación sin instalar nada.
2. **HTTPS obligatorio.** El navegador bloquea muchas APIs (notificaciones, geolocalización, service workers) y marca el sitio como "no seguro" sin TLS. Además, Spotify y otros proveedores OAuth exigen redirect URIs HTTPS.
3. **Validación realista.** Un despliegue real expone problemas que no se ven en local: CORS contra el dominio real, configuración de proxy inverso, renovación de certificados, line-endings tras subir desde Windows, etc.

---

## 2. Arquitectura del despliegue

```
                Internet
                   │
                   ▼
          ┌────────────────────┐
          │  GCP firewall VPC  │  permite 22, 80, 443, 5173
          │  kenetg-light-vpc  │  source 0.0.0.0/0
          └────────┬───────────┘
                   │
          ┌────────▼───────────┐
          │  VM kenetg-light   │  Debian 12, IP 34.14.42.119
          │  Docker + certbot  │
          └────────┬───────────┘
                   │
        ┌──────────┴───────────┐
        │  red Docker interna  │
        │                      │
   ┌────▼──────┐         ┌─────▼──────┐
   │ frontend  │  ──────▶│  backend   │
   │ nginx +   │ /api/*  │ Spring Boot│
   │ React SPA │ proxy   │   :8080    │
   │ :80 :443  │         │ (no expuesto│
   └────┬──────┘         │   a Internet)│
        │                └────┬────────┘
        │                     │
   TLS  │                     │ JDBC + TLS
   Let's│                     │
   Encrypt                    ▼
        │              ┌────────────┐
        │              │   Aiven    │
        │              │   MySQL    │
        │              └────────────┘
        ▼
    Cliente final
   (https://zentimes.es)
```

**Decisiones clave:**

- **Backend NO expuesto a Internet.** Solo escucha en la red Docker interna; el único acceso público es vía nginx en el frontend, que hace `proxy_pass /api/ → backend:8080/api/`.
- **API URL relativa.** El bundle de Vite se construye con `VITE_API_URL=/api`, sin esquema ni host. Las llamadas `fetch("/api/auth/login")` van al mismo origen, evitando CORS y haciendo el frontend portable a cualquier dominio.
- **Certificados montados read-only desde el host** (`/etc/letsencrypt`). Certbot vive en el host (no en un sidecar) porque ya estaba disponible vía apt y no merece la pena meter un contenedor más para un cron.
- **Renovación con `--webroot`** en vez de `--standalone`, para no tener que parar nginx cada 90 días.

---

## 3. Apertura de puertos en GCP

La VPC `kenetg-light-vpc` solo tenía abierto el puerto 22 (SSH). Se añadieron dos reglas:

```bash
# 80 + 443 — tráfico HTTP/HTTPS público (todas las instancias de la VPC)
gcloud compute firewall-rules create kenetg-light-allow-http-https \
  --network=kenetg-light-vpc \
  --direction=INGRESS --priority=1000 \
  --source-ranges=0.0.0.0/0 \
  --allow=tcp:80,tcp:443

# 5173 — Vite dev server (solo desarrollo, abrir y cerrar bajo demanda)
gcloud compute firewall-rules create kenetg-light-allow-vite-dev \
  --network=kenetg-light-vpc \
  --direction=INGRESS --priority=1000 \
  --source-ranges=0.0.0.0/0 \
  --allow=tcp:5173
```

> En la sesión real las reglas se crearon vía API REST con el token del metadata server porque las credenciales gcloud cacheadas no tenían scopes suficientes — el resultado es idéntico.

**Estado final de la VPC:**

| Regla | Puerto | Origen | Tags |
|---|---|---|---|
| `kenetg-light-allow-ssh` | 22 | 0.0.0.0/0 | `kenetg-light-ssh` |
| `kenetg-light-allow-http-https` | 80, 443 | 0.0.0.0/0 | (toda la VPC) |
| `kenetg-light-allow-vite-dev` | 5173 | 0.0.0.0/0 | (toda la VPC) |

---

## 4. Instalación de Docker

Repositorio oficial de Docker para Debian (no la versión `docker.io` de los repos de Debian, que va atrasada):

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker plaza   # requiere logout/login para que aplique
```

Versiones instaladas: Docker `29.4.1`, Compose `v5.1.3`.

---

## 5. Reconfiguración del frontend

El frontend dockerizado original asumía dos cosas que no se cumplen en producción:

1. `VITE_API_URL=http://localhost:8080/api` — funciona en la máquina del desarrollador, pero en producción el navegador del usuario no puede llegar a `localhost:8080`.
2. `nginx.conf` solo escuchaba en `:80` con `server_name _` y sin proxy hacia el backend.

### 5.1 API URL relativa

En `musicreviews-frontend/.env` y como build arg en `docker-compose.yml`:

```env
VITE_API_URL=/api
```

Esto hace que en `src/services/*.js` las llamadas `fetch(\`${API}/auth/login\`)` resuelvan a `/api/auth/login` sobre el mismo origen del navegador. Mismo origen → no hay CORS preflight, no hay dependencia del dominio.

### 5.2 nginx con proxy + SSL

`musicreviews-frontend/nginx.conf` reescrito (resumen):

```nginx
# HTTP — sirve ACME challenge y redirige todo a HTTPS
server {
    listen 80;
    server_name zentimes.es www.zentimes.es;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS — sirve el SPA y proxy a backend en /api
server {
    listen 443 ssl;
    http2 on;
    server_name zentimes.es www.zentimes.es;

    ssl_certificate     /etc/letsencrypt/live/zentimes.es/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/zentimes.es/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    root /usr/share/nginx/html;

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /assets/ { expires 1y; add_header Cache-Control "public, immutable"; }
    location /       { try_files $uri $uri/ /index.html; }
}
```

Puntos clave:

- `/.well-known/acme-challenge/` se sirve desde `/var/www/certbot` (volumen compartido con el host) para que la **renovación** de certbot funcione con `--webroot` sin parar nginx.
- HSTS de 1 año.
- `proxy_pass` con `/` final es importante: sin él, nginx no reescribe el path al hacer proxy.

---

## 6. CORS en el backend

Aunque las llamadas son same-origin (frontend y `/api` viven en `https://zentimes.es`), Spring Security inspecciona el header `Origin` que el navegador envía igualmente. Con la config original (`allowedOrigins = ["http://localhost:5173", "http://localhost"]`) cualquier login devolvía:

```
Invalid CORS request
```

Solución en `SecurityConfig.java`:

```java
config.setAllowedOrigins(List.of(
    "http://localhost:5173",     // Vite dev server (npm run dev)
    "http://localhost",          // nginx local (puerto 80, sin sufijo)
    "https://zentimes.es",       // producción
    "https://www.zentimes.es"
));
```

Tras la rebuild del backend y reinicio del contenedor, el preflight `OPTIONS /api/auth/login` devuelve `Access-Control-Allow-Origin: https://zentimes.es` y la llamada real funciona.

---

## 7. Emisión del certificado Let's Encrypt

Certbot del repo Debian:

```bash
sudo apt-get install -y certbot
```

Primera emisión, antes de levantar el stack (puerto 80 libre → modo `--standalone`):

```bash
sudo certbot certonly --standalone --non-interactive --agree-tos \
  --email ferinazuma@gmail.com \
  -d zentimes.es -d www.zentimes.es
```

Resultado:
- `/etc/letsencrypt/live/zentimes.es/fullchain.pem` (cert + chain)
- `/etc/letsencrypt/live/zentimes.es/privkey.pem` (clave privada ECDSA)
- Cuenta ACME registrada (account ID `48b12795a216571bd207969f005736eb`).
- Caduca a los 90 días; renovación gestionada por `certbot.timer` (sección 9).

---

## 8. docker-compose.yml de producción

Cambios respecto al de desarrollo:

```yaml
services:
  backend:
    image: musicreviews-backend:local
    build: { context: ./backend/backend }
    # ... env vars
    expose: ["8080"]                # solo red interna, NO ports

  frontend:
    image: musicreviews-frontend:local
    build:
      context: ../musicreviews-frontend
      args:
        VITE_API_URL: /api          # URL relativa
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - /var/www/certbot:/var/www/certbot:ro
    depends_on: [backend]
```

Cambios respecto a la versión dockerizada para defensa:

| Antes | Después | Por qué |
|---|---|---|
| `image: ghcr.io/...` | `image: musicreviews-*:local` | El registro privado requiere login; en la VM construimos local |
| backend `ports: 8080:8080` | backend `expose: 8080` | El backend deja de ser accesible desde Internet |
| frontend solo `80:80` | frontend `80:80` + `443:443` + volúmenes de cert | Sirve TLS |
| `VITE_API_URL: http://localhost:8080/api` | `VITE_API_URL: /api` | Funciona contra cualquier dominio |

---

## 9. Renovación automática

Tres piezas:

### 9.1 Cambio de standalone a webroot

Editar `/etc/letsencrypt/renewal/zentimes.es.conf`:

```ini
[renewalparams]
authenticator = webroot
webroot_path = /var/www/certbot,
server = https://acme-v02.api.letsencrypt.org/directory
key_type = ecdsa

[[webroot_map]]
zentimes.es = /var/www/certbot
www.zentimes.es = /var/www/certbot
```

Con `--webroot`, certbot escribe el desafío ACME en `/var/www/certbot/.well-known/acme-challenge/`, nginx lo sirve (volumen compartido al contenedor frontend) y Let's Encrypt valida sin que nadie pare el puerto 80.

### 9.2 Deploy hook que recarga nginx

`/etc/letsencrypt/renewal-hooks/deploy/reload-frontend.sh`:

```sh
#!/bin/sh
docker exec musicreviews-frontend nginx -s reload || true
```

Permisos `0755`. Certbot ejecuta este hook **solo** cuando el cert se renueva con éxito (no en cada `certbot renew`).

### 9.3 systemd timer

El paquete `certbot` instala `certbot.timer` automáticamente. Ejecuta `certbot renew` dos veces al día. Estado:

```
$ systemctl is-enabled certbot.timer   →  enabled
$ systemctl is-active  certbot.timer   →  active
```

### 9.4 Verificación

```bash
sudo certbot renew --dry-run
# → Congratulations, all simulated renewals succeeded
```

---

## 10. Orden de arranque / redeploy

```bash
# 1. (solo primera vez) emitir cert con stack apagado
sudo certbot certonly --standalone -d zentimes.es -d www.zentimes.es ...

# 2. construir imágenes
cd ~/TFG-MusicReviews/musicreviews
sudo docker compose build

# 3. arrancar stack
sudo docker compose up -d

# 4. verificar
curl -sI https://zentimes.es/                # 200
curl -sI http://zentimes.es/                 # 301 → https
curl -s -o /dev/null -w "%{http_code}\n" \
  https://zentimes.es/api/artistas           # 200
```

**Para redeployar tras un cambio:**

```bash
git pull
sudo docker compose build <servicio>         # backend o frontend
sudo docker compose up -d <servicio>
```

---

## 11. Verificación end-to-end

| Comprobación | Comando | Esperado |
|---|---|---|
| DNS | `getent hosts zentimes.es` | `34.14.42.119` |
| Redirect HTTP→HTTPS | `curl -sI http://zentimes.es/` | `301` con `Location: https://...` |
| HTTPS | `curl -sI https://zentimes.es/` | `200`, header `strict-transport-security` |
| Cert | `openssl s_client -connect zentimes.es:443 -servername zentimes.es </dev/null \| openssl x509 -noout -subject -issuer -dates` | `CN=zentimes.es`, issuer `Let's Encrypt`, ~90 días |
| Proxy /api | `curl https://zentimes.es/api/artistas` | JSON con la lista |
| CORS preflight | `curl -i -X OPTIONS https://zentimes.es/api/auth/login -H 'Origin: https://zentimes.es' -H 'Access-Control-Request-Method: POST'` | `200` con `access-control-allow-origin: https://zentimes.es` |
| Renovación | `sudo certbot renew --dry-run` | `simulated renewals succeeded` |

---

## 12. Notas de seguridad

- **Backend no expuesto.** Aunque Spring Boot escucha en `:8080`, ese puerto no está mapeado al host ni abierto en GCP. Solo se accede vía nginx.
- **Secretos.** `application.properties` y los `.env` están gitignored. La config sensible (Aiven, JWT, Spotify, Last.fm) vive solo en `~/TFG-MusicReviews/musicreviews/.env` en la VM.
- **TLS 1.2+ only.** Sin TLS 1.0/1.1, sin cifradores débiles.
- **HSTS.** Tras la primera visita, el navegador rechaza HTTP durante un año.
- **Puerto 5173 abierto pero sin servicio.** El firewall lo permite, pero nada escucha. Si se levanta `vite --host` para desarrollo remoto, hay que considerar cerrarlo después.
- **Autorenovación testada.** El hook recarga nginx automáticamente; no hay riesgo de cert caducado mientras systemd siga vivo.

---

## 13. Anexo: troubleshooting de la sesión

Problemas reales que surgieron, por si vuelven a aparecer:

| Síntoma | Causa | Fix |
|---|---|---|
| `gcloud compute firewall-rules list` → "insufficient authentication scopes" | Credenciales gcloud cacheadas obsoletas, aunque el service account tenía scope `cloud-platform` | Llamar a la API REST de Compute con el token del metadata server (`http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token`) |
| `502 Bad Gateway` en `/api/*` justo después de `up -d` | Spring Boot tarda ~30 s en arrancar (Hibernate + Hikari) | Esperar; `docker logs musicreviews-backend` debe terminar con `Tomcat started on port 8080` |
| `Unexpected token 'I', "Invalid CORS request"` al hacer login | CORS allowlist del backend solo tenía `localhost` | Añadir `https://zentimes.es` y `https://www.zentimes.es` en `SecurityConfig.java`, rebuild backend |
| `git status` muestra ficheros modificados que nadie tocó | Subida desde Windows convirtió LF → CRLF | Añadir `.gitattributes` con `* text=auto eol=lf` y/o normalizar con `dos2unix` |
