#!/bin/sh
set -e

# Generar env-config.js dinámicamente con las variables de entorno pasadas al contenedor
cat <<EOF > /usr/share/nginx/html/env-config.js
window.__ENV__ = {
  VITE_API_URL: "${VITE_API_URL:-https://cmo-api.marioalabi.com/api/v1}",
  VITE_REVERB_HOST: "${VITE_REVERB_HOST:-cmo-api.marioalabi.com}",
  VITE_REVERB_PORT: "${VITE_REVERB_PORT:-443}",
  VITE_REVERB_APP_KEY: "${VITE_REVERB_APP_KEY:-hospital-universitario-key}",
  VITE_REVERB_SCHEME: "${VITE_REVERB_SCHEME:-https}"
};
EOF

exec "$@"
