# App iOS: cómo funciona el código

Guía general de la app del Centro Médico de Occidente (Swift + SwiftUI). Está pensada para que alguien que no escribió el código entienda **qué hace cada parte, cómo se conectan y cómo se protege la sesión**. Las rutas son relativas a `ios/appcmo/appcmo/`.

---

## 1. Qué es la app

Una sola app para dos roles:

| Rol | Pestañas |
|---|---|
| **Médico** | Inicio, Agenda, Pacientes, Perfil (desde Perfil: Mis turnos, Laboratorio, Pacientes ingresados, Ambulancia, Avisos) |
| **Paciente** | Inicio, Citas, Expediente, Laboratorio, Perfil |

`ADMINISTRADOR` y `RECEPCIONISTA` usan la web y Android. Si alguno inicia sesión aquí, la app lo rechaza (ver sección 6).

La app **no tiene base de datos propia**: todo viene de la API Laravel (`/api/v1`). Lo único que guarda en el teléfono es la sesión, los borradores de consulta y una lista de avisos ya vistos (sección 7).

---

## 2. Mapa de carpetas

```
appcmo/
├── appcmoApp.swift        Punto de entrada: crea el SessionStore
├── Models/                Estructuras que representan lo que devuelve la API (Cita, Consulta, Paciente…)
├── Repositories/          Una clase por área de la API: sabe QUÉ endpoint llamar
├── Services/              Piezas técnicas: NetworkClient, KeychainManager, BorradorStore, AppConfig, APIError
├── ViewModels/            La lógica de cada pantalla (@Observable)
└── Views/                 Pantallas en SwiftUI (Auth, Medico, Paciente, Publico, Cuenta, Components)
```

El patrón es **MVVM con repositorios**. Cada capa solo habla con la de al lado:

```
View  ──►  ViewModel  ──►  Repository  ──►  NetworkClient  ──►  API
(pinta)    (decide)        (qué ruta)       (HTTP + token)
```

Ejemplo real, la Agenda del médico:

1. `Views/Medico/AgendaView.swift` muestra la semana y las citas. No hace llamadas de red.
2. `ViewModels/AgendaViewModel.swift` guarda el día elegido, filtra las citas de ese día y las ordena por hora. Llama a `cargar()`.
3. `Repositories/CitasRepository.swift` hace `GET citas?per_page=100`.
4. `Services/NetworkClient.swift` arma la petición, agrega el token y traduce los errores.

Si la API cambia una ruta, se toca el repositorio. Si cambia el diseño, se toca la vista. La lógica no se mezcla con ninguna de las dos.

---

## 3. Cómo arranca la app y qué pantalla muestra

```swift
// appcmoApp.swift
@main struct appcmoApp: App {
    @State private var store = SessionStore()      // un único estado de sesión para toda la app
    var body: some Scene {
        WindowGroup { RootView().environment(store) }
    }
}
```

`RootView` mira `store.estado` y elige la pantalla:

| Estado | Qué se ve |
|---|---|
| `.restaurando` | "Cargando…" mientras se valida la sesión guardada |
| `.noAutenticado` | `LoginView` |
| `.autenticado(sesion)` | `MedicoTabView` o `PacienteTabView`, según `sesion.usuario.rol` |

Al abrirse, `RootView` ejecuta `store.restaurarSesion()` (sección 6). Además escucha la notificación `apiSesionExpirada`: si cualquier petición recibe un 401, `SessionStore` cierra la sesión y manda al login. Por eso ninguna pantalla tiene que manejar la expiración por su cuenta.

`SessionStore` se inyecta con `.environment(store)`, así que cualquier vista lo lee con `@Environment(SessionStore.self)`.

---

## 4. Cliente de red y errores

`Services/NetworkClient.swift` es el único lugar que hace HTTP (`URLSession` con `async/await`):

- Arma la URL: `baseURL + ruta (+ query)`.
- Envía `Accept: application/json` y, si hay cuerpo, `Content-Type: application/json`.
- Si la petición es `autenticada` (por defecto sí), agrega `Authorization: Bearer <token>` leyendo el token del Keychain **en cada petición**.
- Timeout de 20 segundos.
- Las rutas públicas (`publico/...`, login, registro) se llaman con `autenticada: false`: no llevan token.

### La respuesta de la API

La API siempre responde con la misma forma, que la app decodifica en `Models/ApiEnvelope.swift`:

```json
{ "success": true, "message": "...", "data": { ... } }
{ "success": false, "message": "...", "errors": { "campo": ["mensaje"] } }
```

El cliente devuelve solo `data`. Si no viene, lanza `APIError.respuestaInvalida`.

### Traducción de errores

`Services/APIError.swift` convierte el código HTTP en un error con texto legible para el usuario:

| HTTP | `APIError` | Qué pasa en la app |
|---|---|---|
| sin red / timeout | `sinConexion` / `tiempoAgotado` | Mensaje de conexión |
| 401 | `noAutorizado` | Además publica `apiSesionExpirada` y vuelve al login |
| 403 | `prohibido` | Muestra el mensaje de la API (por ejemplo "Sin acceso" en el expediente) |
| 404 / 409 | `noEncontrado` / `conflicto` | Muestra el mensaje de la API |
| 422 | `validacion` | `erroresPorCampo` pone cada mensaje junto a su campo del formulario |
| 429 | `demasiadosIntentos` | Usa el encabezado `Retry-After` para decir cuántos segundos esperar |
| 5xx | `servidor` | "El servidor tuvo un problema" |

Las pantallas usan un tipo común `Carga<Valor>` (`inicial`, `cargando`, `listo`, `fallo`) y un componente `EstadoCarga` que dibuja el indicador, el error con botón "Reintentar" o el contenido. Si ya había datos, una recarga no los borra (no parpadea).

---

## 5. Configuración: de dónde sale la URL de la API

```
ios/Config.xcconfig  ──►  Info.plist (API_BASE_URL = $(API_BASE_URL))  ──►  AppConfig.apiBaseURL
```

- `Config.xcconfig` **no está en git** (está en `ios/.gitignore`). Cada persona lo crea copiando `Config.example.xcconfig`.
- `AppConfig.swift` lee `API_BASE_URL` del `Info.plist`. Si el archivo no existe, el valor queda como el texto literal `$(API_BASE_URL)` y la app usa `http://localhost:8000/api/v1`.
- `NSAllowsLocalNetworking` en el `Info.plist` permite `http://localhost` en el simulador. Para la API desplegada se usa `https`.

Para cambiar de servidor se edita `Config.xcconfig` y se recompila. No hace falta tocar código.

---

## 6. Sesión, login y roles

Todo vive en `ViewModels/SessionStore.swift`.

### Iniciar sesión

```
LoginView ─► SessionStore.iniciarSesion(usuario, password)
              1. POST auth/login        → la API devuelve token + usuario (+ id_paciente si es paciente)
              2. ¿El rol es MEDICO o PACIENTE?
                   no → POST auth/logout con ese token (se revoca) y se devuelve .rolNoPermitido
                         (la pantalla muestra "Credenciales incorrectas.")
                   sí → guarda el token en el Keychain y pasa a .autenticado
```

Un administrador o recepcionista que escriba bien su contraseña **nunca llega a guardar el token**: la app lo revoca de inmediato, y el mensaje es el mismo que si la contraseña fuera incorrecta, para no revelar que la cuenta existe.

### Reabrir la app (`restaurarSesion`)

1. ¿Hay token en el Keychain? Si no → login.
2. Si hay, `GET auth/me` para validarlo:
   - Válido y rol permitido → entra directo (por eso no se pide la contraseña cada vez).
   - 401 → borra el token y muestra "Tu sesión venció".
   - Sin red → no puede confirmar: pide iniciar sesión, pero **conserva** el token.

### Cerrar sesión, cambiar contraseña y 401

| Acción | Qué hace |
|---|---|
| Cerrar sesión | `POST auth/logout` (si la API no responde, igual cierra la sesión local) y borra el token |
| Cambiar contraseña | `PATCH auth/password`. La API revoca **todos** los tokens, así que la app solo borra el suyo y pide iniciar sesión de nuevo |
| Cualquier 401 | `NetworkClient` publica `apiSesionExpirada` → `SessionStore.sesionExpirada()` borra el token y vuelve al login |
| Registro de paciente | `POST auth/registro` responde como un login: se guarda el token y la sesión queda iniciada |

Los datos de la persona (`Sesion`: usuario e `idPaciente`) viven **solo en memoria**. Cada vez que se abre la app se vuelven a pedir con `auth/me`.

---

## 7. Qué se guarda en el teléfono y dónde

| Dato | Dónde | Protección |
|---|---|---|
| Token de sesión | **Keychain** | Cifrado por el sistema, solo accesible con el teléfono desbloqueado |
| Llave de cifrado de borradores | **Keychain** | Igual que el token |
| Borradores de consulta | Archivo en `Application Support/Borradores/` | Cifrado AES-GCM con la llave anterior (sección 8) |
| Avisos ya vistos | `UserDefaults` | Solo ids de avisos, sin datos clínicos |
| Usuario, citas, expedientes, etc. | **Memoria** (se pierde al cerrar la app) | No se guarda en disco |

El token no se imprime en ningún lado (no hay `print` ni logs en el código de la app).

---

## 8. Keychain y borradores cifrados

### Qué es el Keychain

Es el almacén seguro de iOS para secretos pequeños. A diferencia de `UserDefaults` o un archivo, los datos van cifrados por el sistema y otras apps no pueden leerlos. `Services/KeychainManager.swift` es el único archivo que lo toca; el resto de la app le pide cosas por nombre.

### Los dos elementos que guarda

Ambos son de tipo `kSecClassGenericPassword`, con el servicio igual al *bundle identifier* de la app:

| Cuenta (`kSecAttrAccount`) | Contenido | Quién lo usa |
|---|---|---|
| `auth_token` | El token Bearer de la API, como texto UTF-8 | `NetworkClient` (lo lee en cada petición) y `SessionStore` (lo guarda y lo borra) |
| `borradores_key` | Una llave AES de 256 bits | `BorradorStore` (cifra y descifra los borradores) |

### Cómo se guarda

Al crear un elemento se usa `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`:

- **`WhenUnlocked`**: solo se puede leer con el teléfono desbloqueado.
- **`ThisDeviceOnly`**: no viaja en copias de seguridad ni se restaura en otro dispositivo.

`guardar` primero intenta `SecItemUpdate`; si el elemento no existe, hace `SecItemAdd`. `leer` usa `SecItemCopyMatching`. `borrar` usa `SecItemDelete`.

### Ciclo de vida del token

```
login correcto ──► guardarToken ──► (cada petición lo lee) ──► borrarToken
                                                              ▲
            logout · cambio de contraseña · 401 · token inválido en auth/me
```

### La llave de los borradores

`llaveBorradores()` busca la llave en el Keychain. Si no existe (primera vez), genera una nueva con `SymmetricKey(size: .bits256)` y la guarda. **Nunca sale del dispositivo.**

---

## 9. Borradores de consulta (`Services/BorradorStore.swift`)

Mientras el médico atiende, lo que escribe (motivo, diagnóstico, notas, recetas y exámenes) es un **borrador**. Si sale de la pantalla o cierra la app a medias, no pierde nada.

- **Dónde:** un archivo por consulta, `Application Support/Borradores/<idUsuario>_<idConsulta>.borrador`. Cada médico tiene su propio borrador.
- **Cómo se cifra:** se codifica a JSON y se sella con **AES-GCM** (CryptoKit) con la llave del Keychain. En disco solo hay bytes ilegibles.
- **Protección extra:** el archivo usa `completeFileProtection` (iOS lo mantiene cifrado mientras el teléfono está bloqueado) y se excluye de las copias de seguridad.
- **Caducidad:** si pasa **una hora** sin editarse, se borra solo. La limpieza corre al abrir la app y cada vez que vuelve al frente (`MedicoTabView`), y también al leer uno vencido.
- **Cuándo se escribe:** `ConsultaViewModel.persistir()` se llama cada vez que cambia un texto o se agrega o quita una receta o examen. Si ya no hay diferencia con lo que tiene el servidor, borra el archivo.

---

## 10. El flujo de la consulta del médico (`ViewModels/ConsultaViewModel.swift`)

Es la parte con más lógica de la app.

```
Cita "En sala" ─► Iniciar consulta ─► POST citas/{id}/consulta   (la cita pasa a EN_ATENCION)
      │
      ▼
  Se escribe en pantalla ─► todo queda como BORRADOR cifrado en el teléfono
      │
      ▼
  Finalizar ─► envía en este orden:
      1. PUT  consultas/{id}                          (diagnóstico, motivo, notas)
      2. POST consultas/{id}/recetas                  (una por receta)
      3. POST consultas/{id}/ordenes-laboratorio      (una por examen)
      4. PATCH consultas/{id}/finalizar               (la cita pasa a ATENDIDA)
      │
      ▼
  Se borra el borrador y la consulta queda de solo lectura
```

Detalles importantes:

- **Si el envío se corta a medias**, cada receta o examen se quita del borrador apenas la API lo acepta. Lo ya enviado queda en el servidor, el resto sigue como borrador y el nuevo intento **no duplica** nada. El mensaje de error dice en qué paso falló.
- **Abandonar consulta** (`PATCH consultas/{id}/abandonar`): solo se permite mientras ningún examen haya sido trabajado por el laboratorio. Borra también el borrador y la cita vuelve a la agenda como estaba.
- **Cancelar examen** (`PATCH ordenes-laboratorio/{id}/cancelar`): solo si sigue Pendiente. Funciona en la API desplegada desde el 30 de octubre.
- **Segunda consulta:** la app no deja iniciar otra mientras el médico tenga una en atención (regla que valida la app revisando la agenda, no el servidor).

---

## 11. Información pública y avisos

- **Información del hospital** (`Views/Publico/InfoPublicaView.swift`): se abre sin iniciar sesión. Pide `publico/ocupacion`, `publico/medicos-de-turno`, `publico/avisos` y `publico/ambulancia` con `autenticada: false`. El estado de la ambulancia se vuelve a pedir cada 10 segundos mientras la pantalla está abierta (igual que la pantalla de Ambulancia del médico).
- **Campana de avisos** (`AvisosViewModel`): el médico recibe avisos públicos e internos (`GET avisos`); el paciente solo los públicos. El contador muestra los que la persona no ha abierto. Los ids de avisos vistos se guardan en `UserDefaults` bajo la llave `avisosVistos.<idUsuario>`, y solo los que siguen vigentes, para que la lista no crezca.

---

## 12. Cosas que conviene saber

1. **El cierre de sesión no borra los borradores ni su llave.** `descartarSesion` solo borra el token. Los borradores se eliminan solos a la hora, y cada uno es del usuario que lo creó, pero la llave `borradores_key` se conserva.
2. **El Keychain sobrevive a desinstalar la app** (comportamiento normal de iOS). Si se reinstala, la app puede encontrar el token viejo y validarlo con `auth/me`; si ya no es válido, vuelve al login.
3. **Sin red no hay sesión nueva:** al abrir sin conexión, la app no puede validar el token y pide iniciar sesión, aunque lo conserve.
4. **Zona horaria:** `Services/Formato.swift` usa `America/El_Salvador` para calcular "hoy" y las horas. El servidor también debe usar esa zona (`APP_TIMEZONE`); si no, después de las 18:00 hora local la API puede creer que ya es otro día y rechazar iniciar consultas.
5. **Un comentario quedó desactualizado:** en la cabecera de `ConsultaViewModel.swift` dice que la API no permite cancelar una orden de laboratorio. Ya la permite, y la app la usa.
6. **Roles con comportamiento propio:** el médico y el paciente comparten `SessionStore`, `NetworkClient` y los componentes, pero cada uno tiene su `TabView` y sus ViewModels. Un rol no puede abrir las pantallas del otro porque `RootView` decide el árbol completo desde el rol.
