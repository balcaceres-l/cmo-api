# Hospital Universitario

Ecosistema multiplataforma para la gestión operativa y clínica de un hospital universitario.

| Carpeta | Proyecto | Tecnología |
| --- | --- | --- |
| [`backend/`](backend) | API REST compartida | Laravel 12, Sanctum, MariaDB, Reverb |
| [`android/`](android) | App de recepción, enfermería y doctores | Kotlin, Jetpack Compose, MVVM |

Todas las apps consumen la misma API bajo `/api/v1`. La documentación interactiva queda en `/docs/api` cuando el backend está corriendo.

## Inicio rápido

1. Levanta el backend siguiendo [`backend/README.md`](backend/README.md).
2. Abre la carpeta `android/` en Android Studio y configura la URL de la API según [`android/README.md`](android/README.md).
