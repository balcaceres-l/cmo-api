# API del Hospital Universitario

Referencia de la API REST que consumen la app iOS, la app Android y el panel web. Describe el comportamiento del código en `backend/`; la versión interactiva (OpenAPI) se genera sola en `/docs/api` con el servidor corriendo.

## Contenido

1. [Conceptos generales](#1-conceptos-generales)
2. [Roles](#2-roles)
3. [Autenticación](#3-autenticación)
4. [Información pública](#4-información-pública)
5. [Usuarios y turnos](#5-usuarios-y-turnos)
6. [Pacientes y expediente](#6-pacientes-y-expediente)
7. [Citas](#7-citas)
8. [Consulta y recetas](#8-consulta-y-recetas)
9. [Laboratorio](#9-laboratorio)
10. [Habitaciones e ingresos](#10-habitaciones-e-ingresos)
11. [Ambulancia](#11-ambulancia)
12. [Inventario](#12-inventario)
13. [Avisos y dashboard](#13-avisos-y-dashboard)
14. [Tiempo real](#14-tiempo-real)
15. [Estados y transiciones](#15-estados-y-transiciones)
16. [Privacidad de datos](#16-privacidad-de-datos)
17. [Datos de demostración](#17-datos-de-demostración)

---

## 1. Conceptos generales

| Concepto | Valor |
| --- | --- |
| URL base | `http://<host>:8000/api/v1` (en el emulador de Android: `http://10.0.2.2:8000/api/v1`) |
| Formato | JSON en peticiones y respuestas. Enviar `Accept: application/json`. |
| Autenticación | `Authorization: Bearer <token>` (Laravel Sanctum) |
| Identificadores | UUID en todas las tablas |
| Fechas | `YYYY-MM-DD`; horas `HH:mm` (24 h); marcas de tiempo ISO 8601, zona `America/El_Salvador` |

### Respuesta correcta

```json
{
  "success": true,
  "message": "Operación exitosa.",
  "data": { }
}
```

### Respuesta de error

```json
{
  "success": false,
  "message": "Los datos enviados no son válidos.",
  "errors": { "dui": ["El DUI ya está registrado."] },
  "debug": null
}
```

`errors` solo trae contenido en los errores de validación (422). `debug` solo se llena con `APP_DEBUG=true` y error 500.

### Códigos HTTP

| Código | Cuándo |
| --- | --- |
| 200 / 201 | Correcto / recurso creado |
| 401 | Sin token, token vencido, credenciales incorrectas o usuario inactivo |
| 403 | El rol o la relación con el paciente no permite la acción |
| 404 | El recurso no existe |
| 409 | Regla de negocio: cama ocupada, sin camas, ambulancia no disponible, horario ocupado, turno solapado, estado que no permite la acción, stock negativo |
| 422 | Validación de datos |
| 429 | Demasiados intentos |

### Paginación

Los listados aceptan `?page=1&per_page=15` (máximo 100) y devuelven el paginador de Laravel dentro de `data`:

```json
{
  "success": true,
  "message": "Operación exitosa.",
  "data": {
    "current_page": 1,
    "data": [ { } ],
    "per_page": 15,
    "total": 42,
    "last_page": 3
  }
}
```

### Límites de intentos

| Ruta | Límite |
| --- | --- |
| `POST /auth/login` | 5 por minuto por usuario e IP, y 20 por minuto por IP |
| `POST /auth/registro` | 6 por minuto por IP |
| `PATCH /auth/password` | 6 por minuto |

---

## 2. Roles

| Rol | Quién | App |
| --- | --- | --- |
| `ADMINISTRADOR` | Administración y jefes de operaciones | Web |
| `MEDICO` | Doctores (el tipo va en `cargo`: medicina general, emergencia, alergista…) | iOS y Android |
| `RECEPCIONISTA` | Recepción, asistentes y enfermería (se distinguen por `cargo`) | Android |
| `PACIENTE` | Pacientes con cuenta | iOS |

**Médico tratante:** un médico tiene acceso clínico a un paciente solo si tiene con él una cita en estado `AGENDADA`, `EN_ESPERA`, `EN_ATENCION` o `ATENDIDA`, o un ingreso hospitalario a su cargo. Las citas canceladas, no asistidas o solo solicitadas no cuentan.

---

## 3. Autenticación

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| POST | `/auth/login` | Público | Inicia sesión y devuelve el token |
| POST | `/auth/registro` | Público | Crea la cuenta de un paciente |
| GET | `/auth/me` | Autenticado | Usuario actual |
| POST | `/auth/logout` | Autenticado | Revoca el token actual |
| PATCH | `/auth/password` | Autenticado | Cambia la contraseña |

### POST /auth/login

```json
{ "usuario": "medico.general", "password": "Hospital2026!" }
```

Respuesta 200:

```json
{
  "success": true,
  "message": "Autenticación exitosa.",
  "data": {
    "usuario": { "id": "…", "nombre_completo": "Dra. Ana Martínez", "usuario": "medico.general", "rol": "MEDICO", "cargo": "Medicina general", "estado": "ACTIVO" },
    "id_paciente": null,
    "token": "hu_…",
    "token_type": "Bearer"
  }
}
```

- 401 `Credenciales incorrectas.` si el usuario no existe, la contraseña no coincide o la cuenta está inactiva.
- El token vence a los 480 minutos (`AUTH_TOKEN_EXPIRATION`).
- La app decide qué pantallas mostrar según `usuario.rol`. Para un `PACIENTE`, `id_paciente` trae su expediente.

### POST /auth/registro

| Campo | Reglas |
| --- | --- |
| `nombre_completo` | requerido, máx. 150 |
| `usuario` | requerido, máx. 80, único |
| `email` | opcional, correo válido, único |
| `password` | requerido, mín. 8, con `password_confirmation` |
| `dui` | requerido, máx. 12 |
| `fecha_nacimiento` | requerido, fecha anterior a hoy |
| `sexo` | requerido, `M` o `F` |
| `telefono` | opcional, máx. 25 |
| `direccion` | opcional |
| `codigo_activacion` | 6 dígitos; obligatorio solo si el DUI ya tiene expediente |

- 201 con la misma respuesta que el login.
- 422 si el DUI ya tiene una cuenta.

**Paciente que ya tiene expediente.** Si recepción ya registró al paciente (su DUI existe y no tiene cuenta), conocer el DUI no basta para vincular la cuenta:

1. Recepción genera un código con `POST /pacientes/{id}/codigo-activacion` y se lo entrega en persona.
2. El paciente se registra enviando ese `codigo_activacion`.

El código:
- Vence a las 24 h.
- Sirve una sola vez.
- Se invalida tras 5 intentos fallidos.

Sin código válido, el registro responde 422 en `codigo_activacion`.

### PATCH /auth/password

```json
{ "password_actual": "Hospital2026!", "password": "NuevaClave123", "password_confirmation": "NuevaClave123" }
```

- 422 si la contraseña actual no coincide.
- Revoca **todos** los tokens del usuario, incluido el actual: la app debe volver a iniciar sesión.

### Usuario inactivo

Si una cuenta se desactiva, sus tokens se revocan y cualquier petición posterior responde 401 `El usuario se encuentra inactivo.`

---

## 4. Información pública

No requieren token. Nunca devuelven datos de pacientes.

| Método | Ruta | Respuesta (`data`) |
| --- | --- | --- |
| GET | `/publico/ocupacion` | `[{ "planta": 2, "tipo": "SEMIPRIVADA", "estado": "DISPONIBLE", "total": 7 }, …]` |
| GET | `/publico/medicos-de-turno` | `[{ "nombre": "Dra. Ana Martínez", "cargo": "Medicina general", "area": "Consulta general" }]`: médicos cuyo turno cubre la hora actual |
| GET | `/publico/avisos` | `[{ "titulo", "contenido", "vigente_desde", "vigente_hasta" }]`: avisos públicos vigentes |
| GET | `/publico/ambulancia` | `{ "estado": "DISPONIBLE" }` |

---

## 5. Usuarios y turnos

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/usuarios` | ADMINISTRADOR | Lista paginada |
| POST | `/usuarios` | ADMINISTRADOR | Crea personal |
| GET | `/usuarios/{id}` | ADMINISTRADOR | Detalle |
| PUT | `/usuarios/{id}` | ADMINISTRADOR | Edita personal |
| PATCH | `/usuarios/{id}/estado` | ADMINISTRADOR | `{ "estado": "ACTIVO" \| "INACTIVO" }` |
| GET | `/medicos` | ADMINISTRADOR, RECEPCIONISTA | Médicos activos (para agendar) |
| GET | `/turnos` | ADMINISTRADOR | Todos los turnos |
| POST | `/turnos` | ADMINISTRADOR | Crea un turno |
| GET / PUT / DELETE | `/turnos/{id}` | ADMINISTRADOR | Detalle, edición y borrado |
| GET | `/mi/turnos` | MEDICO, RECEPCIONISTA | Turnos del usuario autenticado |

**Campos de usuario:** `nombre_completo` (req., máx. 150), `usuario` (req., máx. 80, único), `email` (opcional, único), `password` (req. al crear, mín. 8; si se omite o va vacío al editar, se conserva), `rol`, `cargo` (máx. 100), `telefono` (máx. 25), `estado`.

Reglas:
- El administrador solo gestiona personal: crear un `PACIENTE` o editar una cuenta de paciente responde 422.
- Un administrador no puede desactivarse ni cambiar su propio rol (422).
- Desactivar un usuario revoca sus tokens.

**Campos de turno:** `id_usuario` (personal activo, no paciente), `fecha`, `hora_inicio` y `hora_fin` (`HH:mm`), `area` (máx. 100). Un turno que se solapa con otro de la misma persona responde 409.

**Turnos nocturnos.** `POST /turnos` acepta un `hora_fin` menor que `hora_inicio` (por ejemplo, 22:00–06:00) y lo guarda como dos turnos: `22:00–23:59` en la fecha indicada y `00:00–06:00` al día siguiente. Así la agenda y "médicos de turno" funcionan dentro de cada fecha.
- Por eso `POST /turnos` devuelve siempre una **lista** con los turnos creados (uno o dos).
- Al editar (`PUT /turnos/{id}`), el fin debe ser posterior al inicio. Para cambiar un turno a nocturno, se crea uno nuevo.

---

## 6. Pacientes y expediente

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/pacientes?q=` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Búsqueda por nombre, DUI o expediente. El MEDICO solo ve sus pacientes. |
| POST | `/pacientes` | ADMINISTRADOR, RECEPCIONISTA | Registra un paciente |
| GET | `/pacientes/{id}` | ADMINISTRADOR, RECEPCIONISTA, MEDICO tratante | Datos del paciente |
| PUT | `/pacientes/{id}` | ADMINISTRADOR, RECEPCIONISTA | Edición parcial |
| POST | `/pacientes/{id}/codigo-activacion` | ADMINISTRADOR, RECEPCIONISTA | Código para que el paciente vincule su cuenta: `{ "codigo": "482913", "vence_en": "…" }`. 409 si ya tiene cuenta. |
| GET | `/pacientes/{id}/expediente` | ADMINISTRADOR, MEDICO tratante | Expediente completo (403 para recepción y médicos no tratantes) |
| PUT | `/pacientes/{id}/historial` | ADMINISTRADOR, MEDICO tratante | `{ "antecedentes_personales", "antecedentes_familiares" }` |
| GET | `/mi/expediente` | PACIENTE | Expediente propio |

**Campos de paciente:** `nombre_completo` (req., máx. 150), `fecha_nacimiento` (req., anterior a hoy), `sexo` (req., `M`/`F`), `dui` (opcional, máx. 12, único), `telefono`, `direccion`, `tipo_sangre` (máx. 5), `alergias`.

- Al crear un paciente se genera `numero_expediente` con el formato `EXP-2026-000001` y su historial clínico vacío.
- **El expediente** incluye `historial`, `citas[].consulta.recetas`, `ordenes_laboratorio[].examen` e `ingresos[].cama.habitacion`.

---

## 7. Citas

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/citas` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Lista paginada con paciente y médico. El MEDICO solo ve las suyas. |
| POST | `/citas` | MEDICO, RECEPCIONISTA | Agenda una cita |
| GET | `/agenda/disponibilidad?id_medico=&fecha=` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Bloques libres del médico ese día |
| PATCH | `/citas/{id}/llegada` | ADMINISTRADOR, RECEPCIONISTA | Registra la llegada del paciente |
| PATCH | `/citas/{id}/reprogramar` | ADMINISTRADOR, RECEPCIONISTA | Cambia médico, fecha u hora |
| PATCH | `/citas/{id}/cancelar` | ADMINISTRADOR, RECEPCIONISTA | `{ "motivo_cancelacion": "…" }` |
| PATCH | `/citas/{id}/confirmar` | ADMINISTRADOR, RECEPCIONISTA | Convierte una solicitud en cita |
| POST | `/solicitudes-atencion` | PACIENTE | `{ "motivo": "…" }`: crea una cita `SOLICITADA` sin médico ni hora |
| GET | `/mi/citas` | PACIENTE | Citas propias. Del médico solo trae `id`, `nombre_completo` y `cargo`. |

### POST /citas

```json
{ "id_paciente": "…", "id_medico": "…", "fecha": "2026-10-27", "hora_inicio": "08:30", "motivo": "Control" }
```

| Campo | Reglas |
| --- | --- |
| `id_paciente` | requerido, debe existir |
| `id_medico` | requerido para recepción (un médico activo); el MEDICO agenda para sí mismo |
| `fecha` | requerido, hoy o posterior |
| `hora_inicio` | requerido, `HH:mm` |
| `hora_fin` | opcional; por defecto `hora_inicio` + 30 min (`DURACION_CITA_MIN`). La cita debe terminar el mismo día. |
| `motivo` | requerido |

Errores:
- 422 si la fecha ya pasó, si la cita cruzaría la medianoche o si la hora queda fuera de un turno del médico.
- 409 `El horario solicitado ya está ocupado.`
- 403 si un MEDICO intenta agendar a un paciente del que no es tratante. El médico solo agenda seguimientos; la primera cita la agenda recepción.

**Reprogramar:** mismos campos salvo `id_paciente`, que no se permite (422): reprogramar mueve la cita, no la cambia de paciente.

**Confirmar:** `{ "id_medico", "fecha", "hora_inicio" }`, con las mismas reglas de fecha y hora. Solo aplica a citas `SOLICITADA`.

**Disponibilidad:** devuelve `[{ "hora_inicio": "08:30", "hora_fin": "09:00" }, …]`, calculado a partir de los turnos del médico menos sus citas activas.

---

## 8. Consulta y recetas

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| POST | `/citas/{id}/consulta` | MEDICO (el de la cita) | Abre la consulta; la cita pasa a `EN_ATENCION` |
| GET | `/consultas/{id}` | ADMINISTRADOR, MEDICO tratante, PACIENTE (la suya) | Consulta con paciente, recetas y órdenes |
| PUT | `/consultas/{id}` | MEDICO (el de la consulta) | `{ "diagnostico", "motivo", "notas" }` |
| POST | `/consultas/{id}/recetas` | MEDICO | Agrega una receta |
| DELETE | `/recetas/{id}` | MEDICO | Elimina una receta |
| PATCH | `/consultas/{id}/finalizar` | MEDICO | Cierra la consulta; la cita pasa a `ATENDIDA` |

**Receta:** `medicamento`, `dosis`, `frecuencia` y `duracion` (requeridos), más `indicaciones` (opcional).

Reglas:
- La consulta solo se abre sobre una cita `AGENDADA` o `EN_ESPERA`. Una segunda apertura responde 409 `La cita ya tiene una consulta.`
- Una consulta cerrada es de solo lectura: editarla, agregar recetas u órdenes responde 409 `La consulta ya está cerrada.`
- Un médico que no es el de la consulta recibe 403.

---

## 9. Laboratorio

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/examenes` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Catálogo de exámenes activos |
| POST | `/consultas/{id}/ordenes-laboratorio` | MEDICO (de la consulta abierta) | `{ "id_examen", "observaciones" }`; se crea `PENDIENTE` |
| GET | `/ordenes-laboratorio?estado=&paciente=` | ADMINISTRADOR, RECEPCIONISTA, MEDICO | Cola de órdenes; el MEDICO ve las que él solicitó |
| PATCH | `/ordenes-laboratorio/{id}/iniciar` | ADMINISTRADOR, RECEPCIONISTA | `PENDIENTE` → `EN_PROCESO` |
| PATCH | `/ordenes-laboratorio/{id}/completar` | ADMINISTRADOR, RECEPCIONISTA | `{ "resultado": "…" }`; `EN_PROCESO` → `COMPLETADO` |
| GET | `/mi/laboratorio` | PACIENTE | Órdenes propias |

- Las transiciones solo avanzan; cualquier otra responde 409.
- En `/mi/laboratorio`, el campo `resultado` solo aparece cuando la orden está `COMPLETADO`.
- La RECEPCIONISTA procesa la cola y registra el resultado, pero no lo recibe: ni en el listado ni en la respuesta de `/completar`. El resultado es dato clínico y solo lo ven el médico, el paciente y el administrador.

---

## 10. Habitaciones e ingresos

Infraestructura fija, creada por el seeder (no hay endpoints para crear ni borrar habitaciones o camas):

| Planta | Tipo | Habitaciones | Camas |
| --- | --- | --- | --- |
| 3 | `VIP` (individual) | VIP-301 a VIP-305 | 5 (`VIP-301` …) |
| 2 | `SEMIPRIVADA` (compartida) | HAB-201 a HAB-206 | 12 (`HAB-201-A`, `HAB-201-B` …) |

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/habitaciones` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Mapa de habitaciones y camas |
| GET | `/ingresos?activos=1` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Ingresos; el MEDICO ve los suyos |
| POST | `/ingresos` | ADMINISTRADOR, RECEPCIONISTA | Asigna una cama |
| PATCH | `/ingresos/{id}/alta` | ADMINISTRADOR, RECEPCIONISTA | Da de alta y libera la cama |
| PATCH | `/ingresos/{id}/trasladar` | ADMINISTRADOR, RECEPCIONISTA | `{ "id_cama" }`: alta y nuevo ingreso en una sola operación |

### GET /habitaciones

Devuelve una lista por planta (primero la 2, luego la 3); cada planta es una lista de habitaciones:

```json
[
  [
    { "codigo": "HAB-201", "tipo": "SEMIPRIVADA", "capacidad": 2,
      "camas": [
        { "codigo": "HAB-201-A", "estado": "OCUPADA", "paciente": { "id_paciente": "…", "nombre_completo": "…", "numero_expediente": "…" } },
        { "codigo": "HAB-201-B", "estado": "DISPONIBLE", "paciente": null }
      ] }
  ],
  [ { "codigo": "VIP-301", "tipo": "VIP", "capacidad": 1, "camas": [ … ] } ]
]
```

`paciente` solo viene para ADMINISTRADOR, RECEPCIONISTA y el médico tratante del ingreso; para cualquier otro es `null`.

### POST /ingresos

```json
{ "id_paciente": "…", "tipo": "SEMIPRIVADA", "id_cama": null, "id_medico": "…", "motivo": "Observación" }
```

- Sin `id_cama`, el sistema elige una cama libre. En semiprivadas completa primero las habitaciones que ya tienen una cama ocupada.
- 409: cama ocupada, sin camas libres (`No hay camas disponibles en planta 2`) o paciente con un ingreso activo.
- 422: la cama no corresponde al tipo pedido, o `id_medico` no es un médico activo.
- **Concurrencia:** la asignación bloquea las camas del tipo pedido dentro de una transacción, y la base de datos tiene índices únicos que impiden dos ingresos activos en la misma cama o del mismo paciente. Si dos recepciones asignan la última cama a la vez, una recibe 201 y la otra 409.

---

## 11. Ambulancia

El hospital tiene **una sola ambulancia** (creada por el seeder).

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/ambulancia` | ADMINISTRADOR, RECEPCIONISTA, MEDICO | Estado y servicio en curso. El MEDICO recibe solo `{ estado, servicio_activo: { prioridad, hora_salida } }`, sin datos de la persona atendida. |
| POST | `/ambulancia/servicios` | ADMINISTRADOR, RECEPCIONISTA, MEDICO | Despacha la ambulancia |
| PATCH | `/ambulancia/servicios/{id}/finalizar` | ADMINISTRADOR, RECEPCIONISTA | `{ "notas" }`: cierra el servicio y la ambulancia vuelve a `DISPONIBLE` |
| PATCH | `/ambulancia/estado` | ADMINISTRADOR | `{ "estado": "DISPONIBLE" \| "EN_MANTENIMIENTO" }` |
| GET | `/ambulancia/servicios?desde=&hasta=&prioridad=&estado=` | ADMINISTRADOR, RECEPCIONISTA | Bitácora filtrable |

### POST /ambulancia/servicios

```json
{ "paciente_nombre": "Adulto mayor sin registro", "direccion": "Col. San José #14", "descripcion": "Caída", "prioridad": "ALTA" }
```

- `id_paciente` o `paciente_nombre`: uno de los dos es obligatorio.
- `prioridad`: `BAJA`, `MEDIA`, `ALTA` o `CRITICA`.
- 409 si la ambulancia no está `DISPONIBLE`; el mensaje indica su estado actual.
- No se puede pasar a mantenimiento con un servicio en curso (409).

---

## 12. Inventario

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/recursos` | ADMINISTRADOR, RECEPCIONISTA | Lista paginada |
| GET | `/recursos/{id}` | ADMINISTRADOR, RECEPCIONISTA | Detalle |
| POST | `/recursos` | ADMINISTRADOR | Crea un recurso |
| PUT | `/recursos/{id}` | ADMINISTRADOR | Edita datos (no la cantidad) |
| DELETE | `/recursos/{id}` | ADMINISTRADOR | Borra un recurso sin movimientos (409 si los tiene) |
| POST | `/recursos/{id}/movimientos` | ADMINISTRADOR, RECEPCIONISTA | `{ "tipo": "ENTRADA" \| "SALIDA", "cantidad": 3, "motivo": "…" }` |

- **Recurso:** `nombre`, `categoria` (`Medicamento`, `Insumo` o `Equipo`), `unidad`, `stock_minimo`, `ubicacion` y `cantidad` (solo al crear).
- La cantidad solo cambia con movimientos, para que el stock cuadre con el historial.
- Una salida que deje la cantidad en negativo responde 409.
- La RECEPCIONISTA solo registra salidas (403 en entradas).

---

## 13. Avisos y dashboard

| Método | Ruta | Roles | Descripción |
| --- | --- | --- | --- |
| GET | `/avisos` | ADMINISTRADOR, MEDICO, RECEPCIONISTA | Todos los avisos (públicos e internos) |
| POST | `/avisos` | ADMINISTRADOR | `{ "titulo", "contenido", "publico": true, "vigente_desde", "vigente_hasta" }` |
| GET / PUT / DELETE | `/avisos/{id}` | ADMINISTRADOR | Detalle, edición y borrado |
| GET | `/dashboard/metricas` | ADMINISTRADOR | Métricas para el panel web |

`/dashboard/metricas` devuelve `ocupacion` (por planta y estado), `citas_hoy` (por estado), `laboratorio` (órdenes por estado), `recursos_bajo_minimo`, `ambulancia` (`estado` y `servicios_mes`), `pacientes` y `personal_activo`.

---

## 14. Tiempo real

Laravel Reverb (protocolo Pusher), canales **públicos**:

| Canal | Evento | Payload | Cuándo |
| --- | --- | --- | --- |
| `ambulancia` | `App\Events\AmbulanciaActualizada` | `{ "estado": "EN_SERVICIO", "servicio_activo": { "prioridad": "ALTA", "hora_salida": "…" } }` | Despacho, finalización o cambio de estado |
| `ocupacion` | `App\Events\OcupacionActualizada` | `{ "actualizada_en": "…" }` | Ingreso, alta o traslado: la app vuelve a pedir `/habitaciones` o `/publico/ocupacion` |

- **Conexión:** `ws://<REVERB_HOST>:<REVERB_PORT>/app/<REVERB_APP_KEY>`. Sirven los clientes de Pusher para Swift y Kotlin, y `laravel-echo` en web.
- **Cuándo se emiten:** los eventos salen después de guardar en la base de datos. Si Reverb no está disponible, la operación se guarda igual y el error solo queda en el log.
- **Respaldo:** si no hay conexión WebSocket, consulta `GET /publico/ambulancia` cada 10 segundos.
- **Sin datos personales:** los payloads nunca llevan información de pacientes.

---

## 15. Estados y transiciones

**Cita**

```
SOLICITADA ──confirmar──▶ AGENDADA ──llegada──▶ EN_ESPERA ──abrir consulta──▶ EN_ATENCION ──finalizar──▶ ATENDIDA
     │                       │  ▲                   │
     └──────cancelar─────────┴──┴─reprogramar───────┴──cancelar──▶ CANCELADA
```

- `cancelar`: desde `SOLICITADA`, `AGENDADA` o `EN_ESPERA`.
- `reprogramar`: desde `AGENDADA` o `EN_ESPERA` (vuelve a `AGENDADA`).
- `llegada`: solo desde `AGENDADA`.
- Abrir consulta: desde `AGENDADA` o `EN_ESPERA`.
- Cualquier otra acción responde 409 `No se puede <acción> una cita en estado <estado>.`

**Orden de laboratorio:** `PENDIENTE` → `EN_PROCESO` → `COMPLETADO`.

**Ambulancia:** `DISPONIBLE` → `EN_SERVICIO` (despacho) → `DISPONIBLE` (finalizar). `DISPONIBLE` ↔ `EN_MANTENIMIENTO` solo lo hace el administrador y sin servicio en curso.

**Cama:** `DISPONIBLE` ↔ `OCUPADA`, según los ingresos y las altas.

---

## 16. Privacidad de datos

| Dato | Quién lo ve |
| --- | --- |
| Ocupación, médicos de turno, avisos públicos, estado de la ambulancia | Cualquiera, sin token |
| Nombre, fecha de nacimiento, DUI, teléfono, dirección, expediente | Paciente, médico tratante, administrador, recepción |
| Tipo de sangre y alergias | Paciente, médico tratante y administrador. Se ocultan para los demás, también cuando el paciente viene dentro de citas, ingresos u órdenes. |
| Diagnósticos, recetas, antecedentes, resultados de laboratorio | Paciente (resultados al estar completados), médico tratante y administrador. Recepción registra resultados pero no los ve. |
| Usuario de acceso, correo y teléfono del personal | Administrador. El paciente solo ve nombre y cargo de su médico. |
| Habitación y cama de un paciente | Paciente, médico tratante, administrador y recepción |
| Persona atendida por la ambulancia | Administrador y recepción |

Cuando no hay permiso, la API responde 403 sin indicar si el registro existe.

---

## 17. Datos de demostración

`php artisan migrate --seed` crea:

- 11 habitaciones y 17 camas, y 1 ambulancia (`P-123456`).
- Catálogo de exámenes, turnos de la semana actual, recursos de inventario y avisos.
- Los usuarios de abajo, todos con la contraseña `Hospital2026!`:

| Usuario | Rol | Cargo |
| --- | --- | --- |
| `admin` | ADMINISTRADOR | Administración |
| `medico.general` | MEDICO | Medicina general |
| `medico.emergencia` | MEDICO | Emergencia |
| `medico.alergista` | MEDICO | Alergista |
| `recepcion` | RECEPCIONISTA | Recepcionista |
| `enfermeria` | RECEPCIONISTA | Enfermera |
| `paciente.uno`, `paciente.dos` | PACIENTE | — |

---

## Limitaciones conocidas

- `GET /citas` todavía no acepta filtros de `fecha`, `estado` ni búsqueda: las apps filtran del lado del cliente.
- Médicos y recepción solo ven sus propios turnos (`/mi/turnos`) y quién está de turno ahora (`/publico/medicos-de-turno`); la grilla completa es del administrador.
- El paciente no puede editar sus datos de contacto desde la app; lo hace recepción.
- El listado de laboratorio de un médico muestra las órdenes que él solicitó; las del resto de sus pacientes se ven en el expediente.
