#!/usr/bin/env sh
# Respaldo de PostgreSQL (producción). Uso, desde la carpeta del proyecto en la EC2: ./backup-db.sh
# Guarda un .sql.gz en ~/backups y conserva los últimos 14.
set -eu

cd "$(dirname "$0")"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
KEEP="${KEEP:-14}"
mkdir -p "$BACKUP_DIR"

# Lee DB_NAME y DB_USERNAME del .env (con valores por defecto).
DB_NAME=$(grep -E '^DB_NAME=' .env 2>/dev/null | cut -d= -f2- || true)
DB_USERNAME=$(grep -E '^DB_USERNAME=' .env 2>/dev/null | cut -d= -f2- || true)
DB_NAME=${DB_NAME:-sabor_db}
DB_USERNAME=${DB_USERNAME:-sabor_user}

FILE="$BACKUP_DIR/${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql.gz"
docker compose -f compose.prod.yaml exec -T db pg_dump -U "$DB_USERNAME" -d "$DB_NAME" --clean --if-exists \
  | gzip > "$FILE"

# Un respaldo vacío indica un error (por ejemplo, el contenedor db no está corriendo).
if [ ! -s "$FILE" ] || [ "$(gzip -cd "$FILE" | head -c 1 | wc -c)" -eq 0 ]; then
  echo "ERROR: el respaldo quedó vacío" >&2
  rm -f "$FILE"
  exit 1
fi

ls -1t "$BACKUP_DIR"/"${DB_NAME}"_*.sql.gz | tail -n +"$((KEEP + 1))" | xargs -r rm -f
echo "Respaldo creado: $FILE"
