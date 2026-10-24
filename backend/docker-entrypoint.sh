#!/bin/bash
set -e

# Asegurar permisos en directorios escribibles de Laravel
chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache
chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache

# Si la variable de entorno RUN_MIGRATIONS es true, correr migraciones
if [ "$RUN_MIGRATIONS" = "true" ]; then
    echo "Ejecutando migraciones..."
    php artisan migrate --force --seed || true
fi

exec "$@"
