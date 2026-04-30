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
# shellcheck disable=SC1090
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
