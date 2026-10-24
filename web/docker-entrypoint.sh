#!/bin/sh
set -e

# Generar env-config.js dinámicamente con las variables de entorno pasadas al contenedor
cat <<EOF > /usr/share/nginx/html/env-config.js
window.__ENV__ = {
  VITE_API_URL: "${VITE_API_URL:-http://192.168.110.200:8000/api/v1}",
  VITE_REVERB_HOST: "${VITE_REVERB_HOST:-192.168.110.200}",
  VITE_REVERB_PORT: "${VITE_REVERB_PORT:-8080}",
  VITE_REVERB_APP_KEY: "${VITE_REVERB_APP_KEY:-hospital-universitario-key}",
  VITE_REVERB_SCHEME: "${VITE_REVERB_SCHEME:-http}"
};
EOF

exec "$@"
