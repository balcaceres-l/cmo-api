# Panel Web (React + Vite + TypeScript)

Panel web del Centro Médico de Occidente para el rol **Administrador** (administración y jefes de operaciones). Consume la API de [`../backend`](../backend).

## Requisitos

- Node.js LTS (v20 o v22)
- npm v10 o superior

## Instalación y ejecución

1. Copia el archivo de variables de entorno y ajusta los valores si es necesario:
   ```bash
   cp .env.example .env
   ```

2. Instala las dependencias:
   ```bash
   npm install
   ```

3. Inicia el servidor de desarrollo en el puerto 5173:
   ```bash
   npm run dev
   ```

## Lineamientos de arquitectura

- **Framework**: React con Vite en el puerto `5173` (permitido por CORS en la API).
- **Lenguaje**: TypeScript.
- **Rutas**: React Router.
- **Gestión de estado / API**: TanStack Query o cliente HTTP centralizado con token Bearer guardado en `sessionStorage` o memoria.
- **Tiempo real**: Laravel Echo + cliente compatible con Reverb en WebSockets.
- **Módulos clave**:
  - Dashboard con indicadores operativos.
  - Gestión de usuarios y personal.
  - Grilla semanal de turnos (con validación de solapes).
  - Mapa de camas en plantas 2 y 3.
  - Bitácora y despacho de ambulancia.
  - Inventario de recursos hospitalarios.
  - Expedientes clínicos (modo lectura).
