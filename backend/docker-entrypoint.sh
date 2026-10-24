#!/bin/bash
set -e

# Crear database.sqlite de respaldo si no existe
mkdir -p /var/www/html/database
touch /var/www/html/database/database.sqlite

# Asegurar permisos en directorios escribibles de Laravel
chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache /var/www/html/database
chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache /var/www/html/database


# Si la variable de entorno RUN_MIGRATIONS es true, correr migraciones
if [ "$RUN_MIGRATIONS" = "true" ]; then
    echo "Ejecutando migraciones..."
    php artisan migrate --force --seed || true
fi

# Iniciar servidor WebSocket Laravel Reverb en segundo plano si ENABLE_REVERB está activo (por defecto true)
if [ "${ENABLE_REVERB:-true}" = "true" ]; then
    REVERB_LISTEN_HOST="${REVERB_SERVER_HOST:-0.0.0.0}"
    REVERB_LISTEN_PORT="${REVERB_PORT:-8080}"
    echo "Iniciando Laravel Reverb en ${REVERB_LISTEN_HOST}:${REVERB_LISTEN_PORT}..."
    php artisan reverb:start --host="${REVERB_LISTEN_HOST}" --port="${REVERB_LISTEN_PORT}" &
fi

exec "$@"

