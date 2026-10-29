# App Android (Kotlin)

App del Hospital Universitario para recepción, enfermería y doctores. Consume la API de [`../backend`](../backend).

## Crear el proyecto

En Android Studio: **New Project → Empty Activity (Compose)** y guárdalo en **esta misma carpeta** (`android/`), con:

- Lenguaje: Kotlin
- Build: Gradle Kotlin DSL
- Minimum SDK: API 26 o superior

## Lineamientos

- Arquitectura MVVM: `ui` (Compose) → `ViewModel` → `repository` → `data/remote` (Retrofit).
- Asincronía con corrutinas y `Flow`.
- Token Bearer guardado en DataStore cifrado; se envía en cada petición con un interceptor de OkHttp.
- URL base de la API y variables de entorno cargadas automáticamente desde `.env` (ubicado en `../.env` en la raíz de `cmo-api`):

  ```env
  API_BASE_URL=http://10.0.2.2:8000/api/v1/
  ```

  `10.0.2.2` es la IP con la que el emulador llega al `localhost` de la computadora (o la IP local si usas un dispositivo físico). Opcionalmente se puede sobreescribir en `local.properties` (`api.baseUrl=...`) para pruebas locales específicas.
- Dependencias y plugins gestionados mediante **Gradle Version Catalog** (`gradle/libs.versions.toml`).
- Tiempo real (estado de la ambulancia y ocupación): cliente Pusher compatible con Reverb, canales públicos `ambulancia` y `ocupacion`. Si no hay conexión, consulta `GET /publico/ambulancia` cada 10 segundos.
