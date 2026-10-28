# Hospital Universitario

Ecosistema multiplataforma para la gestión operativa y clínica de un hospital universitario.

| Carpeta | Proyecto | Tecnología |
| --- | --- | --- |
| [`backend/`](backend) | API REST compartida | Laravel 12, Sanctum, MariaDB, Reverb |
| [`android/`](android) | App de recepción, enfermería y doctores | Kotlin, Jetpack Compose, MVVM |
| [`ios/`](ios) | App de doctores y pacientes | Swift, SwiftUI, MVVM |
| [`web/`](web) | Panel web de administración y operaciones | React 18, Vite, TypeScript |
| [`docs/`](docs) | Plan de trabajo y documentación | HTML interactivo, Backlog |

Todas las apps consumen la misma API bajo `/api/v1`. La documentación interactiva queda en `/docs/api` cuando el backend está corriendo.

## Inicio rápido

> 📖 **Guía Completa**: Consulta [`INSTRUCCIONES.md`](INSTRUCCIONES.md) para la guía paso a paso desde `composer install` para el backend hasta los comandos de compilación, pruebas y ejecución de Android.

1. **Backend**: Levanta el backend siguiendo [`backend/README.md`](backend/README.md) o [`INSTRUCCIONES.md`](INSTRUCCIONES.md).
2. **Web**: Abre [`web/README.md`](web/README.md) y ejecuta `npm install && npm run dev` (puerto 5173).
3. **Android**: Sigue [`INSTRUCCIONES.md`](INSTRUCCIONES.md) o abre `android/` en Android Studio.
4. **iOS**: Abre `ios/` en Xcode en una Mac y configura `Config.xcconfig` según [`ios/README.md`](ios/README.md).

## Convenciones de trabajo

- **Ramas**: `<plataforma>-<persona>-s<N>` (por ejemplo `ios-kath-s2`, `web-carlos-s2`, `android-mario-s2`).
- **Commits**: Iniciar con el número de la HU (por ejemplo `HU-8: Configurar proyecto base web`).
- **Pull Requests**: Todo cambio entra a `development` mediante PR revisado por al menos un compañero.

