# App iOS (Swift + SwiftUI)

App móvil del Centro Médico de Occidente para los roles **Médico** y **Paciente**. Consume la API compartida de [`../backend`](../backend).

## Requisitos

- macOS con Xcode 15 o superior
- Simulador de iOS o dispositivo físico

## Crear y abrir el proyecto en Xcode

1. En una Mac, abre Xcode y selecciona **Create New Project → iOS → App**.
2. Parámetros del proyecto:
   - **Product Name**: `appcmo`
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Storage**: None
3. Guarda el proyecto dentro de **esta misma carpeta** (`ios/`). Queda en `ios/appcmo/appcmo.xcodeproj`.
4. Configura el archivo de entorno:
   - Copia `Config.example.xcconfig` como `Config.xcconfig`:
     ```bash
     cp Config.example.xcconfig Config.xcconfig
     ```
   - `appcmo.xcodeproj` ya asocia `Config.xcconfig` a Debug y Release, y `ios/appcmo/Info.plist` expone `API_BASE_URL`. Si `Config.xcconfig` no existe, la app usa `http://localhost:8000/api/v1`.
   - Simulador: `localhost` funciona. Dispositivo físico: usa la IP de tu Mac en la red local.

## Estado actual

Login por rol (HU-7 y HU-21): usuario y contraseña contra `POST /auth/login`. `MEDICO` entra al inicio del médico y `PACIENTE` al del paciente. `ADMINISTRADOR` y `RECEPCIONISTA` (que usan web y Android) reciben "Credenciales incorrectas.", sin indicar por qué, y no queda sesión guardada. El token vive en el Keychain y se valida con `GET /auth/me` al abrir la app. Para probar con los usuarios demo (contraseña `Hospital2026!`) ver `backend/docs/API.md` §17.

Rol **Médico** (S3): Inicio, Agenda (semana y citas del día), detalle de cita, consulta (diagnóstico, notas, recetas (varios medicamentos a la vez) y exámenes (selección múltiple) con borradores cifrados en el iPhone que se envían al finalizar y se eliminan solos tras una hora sin editarse), Mis pacientes (filtros Todos / Ingresados / Ambulatorios y última consulta), expediente clínico (antecedentes editables; 403 → "Sin acceso"), seguimiento con consultorio, Laboratorio, Mis turnos, Pacientes ingresados, Ambulancia (se actualiza cada 10 s) y Avisos dentro de Perfil. La app impide iniciar una segunda consulta mientras haya una en atención (esa regla solo existe en iOS). Los consultorios (C-101 a C-104) son fijos: ningún rol los gestiona todavía.

Rol **Paciente**: Inicio (próxima cita, estado de sus exámenes, habitación y pedir atención), Citas (filtros Hoy, Próximas, Anteriores y Solicitadas), Expediente (historial y consultas con diagnóstico y recetas, con búsqueda por fecha), Laboratorio (estado de sus exámenes), Perfil y cambio de contraseña. La campana muestra los avisos públicos y desde Inicio se abre la información del hospital (camas, médicos de turno, avisos y ambulancia, sin iniciar sesión). Todo es de solo lectura salvo pedir atención; reprogramar o cancelar lo hace recepción. Un paciente no puede tener dos citas activas a la misma hora y la llegada solo se registra el día de la cita (ambas reglas las valida la API).

## Lineamientos de arquitectura (MVVM)

- **Vistas (`Views/`)**: Construidas 100% en SwiftUI.
  - Vistas y navegación para el rol **Paciente** (información pública, citas, mi expediente, mi habitación).
  - Vistas y navegación para el rol **Médico** (agenda, consultas, prescripciones/órdenes, hospitalizados y emergencias/ambulancia).
- **Modelos de Vista (`ViewModels/`)**: Clases observables con `@Observable` (iOS 17+) o `ObservableObject`.
- **Modelos (`Models/`)**: Estructuras de datos conformes a `Codable` que reflejan los esquemas JSON de la API.
- **Servicios (`Services/`)**:
  - `NetworkClient`: Cliente HTTP con `URLSession` y `async/await`, interceptor del token Bearer y manejo unificado de errores (401, 403, 409, 422).
  - `KeychainManager`: Almacenamiento seguro del token de sesión en el Keychain del sistema.
  - `WebSocketService`: Integración con Pusher Swift compatible con el servidor Laravel Reverb.
