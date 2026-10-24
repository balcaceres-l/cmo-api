# App iOS (Swift + SwiftUI)

App móvil del Centro Médico de Occidente para los roles **Médico** y **Paciente**. Consume la API compartida de [`../backend`](../backend).

## Requisitos

- macOS con Xcode 15 o superior
- Simulador de iOS o dispositivo físico

## Crear y abrir el proyecto en Xcode

1. En una Mac, abre Xcode y selecciona **Create New Project → iOS → App**.
2. Parámetros del proyecto:
   - **Product Name**: `HospitalCMO`
   - **Interface**: SwiftUI
   - **Language**: Swift
   - **Storage**: None
3. Guarda el proyecto dentro de **esta misma carpeta** (`ios/`).
4. Configura el archivo de entorno:
   - Copia `Config.example.xcconfig` como `Config.xcconfig`:
     ```bash
     cp Config.example.xcconfig Config.xcconfig
     ```
   - En los Project Settings de Xcode, asocia `Config.xcconfig` a tus Configurations (Debug / Release) para leer `API_BASE_URL` en `Info.plist`.

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
