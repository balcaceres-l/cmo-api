#!/bin/bash
set -e

# Crear directorios y archivos de log y sqlite con permisos de www-data
mkdir -p /var/www/html/database /var/www/html/storage/logs /var/www/html/storage/framework/cache /var/www/html/storage/framework/sessions /var/www/html/storage/framework/views
touch /var/www/html/database/database.sqlite
touch /var/www/html/storage/logs/laravel.log

chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache /var/www/html/database
chmod -R 775 /var/www/html/storage /var/www/html/bootstrap/cache /var/www/html/database

# Configurar puerto de Apache (por defecto 8000 para soportar network_mode: host sin chocar con el puerto 80 del host)
APACHE_PORT="${PORT:-8000}"
sed -i "s/Listen 80/Listen ${APACHE_PORT}/g" /etc/apache2/ports.conf
sed -i "s/<VirtualHost \*:80>/<VirtualHost \*:${APACHE_PORT}>/g" /etc/apache2/sites-available/*.conf


# Si la variable de entorno RUN_MIGRATIONS es true, correr migraciones
if [ "$RUN_MIGRATIONS" = "true" ]; then
    echo "Ejecutando migraciones..."
    su -s /bin/bash www-data -c "php artisan migrate --force --seed" || true
fi

# Iniciar servidor WebSocket Laravel Reverb como www-data en segundo plano
if [ "${ENABLE_REVERB:-true}" = "true" ]; then
    REVERB_LISTEN_HOST="${REVERB_SERVER_HOST:-0.0.0.0}"
    REVERB_LISTEN_PORT="${REVERB_PORT:-8080}"
    echo "Iniciando Laravel Reverb en ${REVERB_LISTEN_HOST}:${REVERB_LISTEN_PORT}..."
    su -s /bin/bash www-data -c "php artisan reverb:start --host=${REVERB_LISTEN_HOST} --port=${REVERB_LISTEN_PORT}" &
fi


exec "$@"

