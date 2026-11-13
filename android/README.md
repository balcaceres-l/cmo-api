# App Android (Kotlin)

App del Hospital Universitario para **recepción** y **médicos**. Consume la API de [`../backend`](../backend). Los roles `ADMINISTRADOR` y `PACIENTE` no entran a esta app (usan la web y la app iOS).

## Tecnología

- Kotlin, vistas **XML con ViewBinding** (no Jetpack Compose), Material Components.
- Arquitectura **MVVM** por módulo: `Fragment` → `ViewModel` (StateFlow) → casos de uso → repositorio → `ApiService` (Retrofit + OkHttp).
- Corrutinas y `Flow`. Inyección manual en `core/di/AppContainer.kt`.
- Minimum SDK 26, target SDK 34.

## Estructura

```
app/src/main/java/com/cmo/hospital/
├── core/        red (Retrofit, interceptores), sesión, tiempo real (Reverb), modelos, utilidades de UI
├── components/  barra superior y avisos reutilizables
└── features/
    ├── auth/           login y cierre de sesión
    ├── recepcionista/  citas, pacientes, camas (admisión), laboratorio, ambulancia
    └── doctor/         inicio (citas, turnos, pacientes, ambulancia), consulta, expedientes, laboratorio
```

Los layouts de cada módulo están en `app/src/main/res-features/<módulo>`.

## Configuración

La URL de la API y los datos de Reverb se leen del `.env` de la raíz del repositorio (`../.env`) al compilar; se pueden sobrescribir en `local.properties` (`api.baseUrl=...`). Con el teléfono por USB:

```bash
adb reverse tcp:8000 tcp:8000
adb reverse tcp:8080 tcp:8080
```

## Sesión

- El token Bearer de Sanctum se guarda en `SharedPreferences` privadas de la app y se envía en cada petición desde `AuthInterceptor`.
- Si la API responde **401** (token vencido o revocado), la app borra la sesión y vuelve al login.
- Al cerrar sesión se revoca el token en el servidor y se borran los datos cargados del usuario.

## Tiempo real

Cliente propio del protocolo Pusher sobre el WebSocket de OkHttp (`core/realtime/ReverbClient.kt`):

- `ambulancia` (público): estado de la ambulancia.
- `private-personal` (privado, autorizado con `POST /api/v1/broadcasting/auth`): reportes del traslado.

Requiere `php artisan reverb:start` en el backend. Si se cae la conexión, se reintenta cada 5 segundos.

## Compilar y probar

```bash
./gradlew testDebugUnitTest assembleDebug
```

Las pruebas unitarias usan una API simulada con las respuestas del backend. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
