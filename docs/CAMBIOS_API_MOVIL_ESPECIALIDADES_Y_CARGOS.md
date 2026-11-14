# Guía de Cambios en el Backend para Android e iOS

Resumen de los cambios recientes en la API (`/api/v1`) relacionados con la separación de **Cargos** y **Especialidades Médicas**, el catálogo de especialidades y el flujo de **Citas por Especialidad**.

---

## 1. Cambios en `User` (Autenticación y Personal)

Aplica a los endpoints:
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/usuarios`, `POST /api/v1/usuarios`, `PUT /api/v1/usuarios/{id}`
- `GET /api/v1/medicos`

### Médicos (`rol = "MEDICO"`)
- El campo `cargo` ahora es **`null`** en base de datos.
- La especialidad se expone en los siguientes campos:
  - `id_especialidad`: `string (UUID) | null`
  - `especialidad_nombre`: `string | null` (atributo calculado incluido automáticamente en las respuestas del usuario)
  - `especialidad`: objeto `{ id_especialidad, nombre, descripcion, es_sistema, activo }` cuando se carga la relación.
- **Recomendación en móvil:** para mostrar la especialidad de un médico en UI, leer **`especialidad_nombre`** (o `especialidad.nombre`), **no `cargo`**.
- Al crear un médico (`POST /api/v1/usuarios`), `id_especialidad` es obligatorio (se conserva soporte temporal si se envía `cargo`, pero las apps deben migrar a `id_especialidad`).

### Cargos fijos para personal no médico
- `ADMINISTRADOR`: solo acepta `"Administración"` o `"Jefe de Operaciones"`.
- `RECEPCIONISTA`: solo acepta `"Recepcionista"` o `"Enfermera"`.
- Para estos roles, `id_especialidad` siempre será `null`.

### Otras reglas de usuarios y médicos
- **Inmutabilidad de rol:** `PUT /api/v1/usuarios/{id}` rechaza con `422` cualquier intento de cambiar el `rol` de un usuario existente.
- **Filtro en `GET /api/v1/medicos`:** ahora acepta el parámetro opcional `?id_especialidad=<uuid>` (además de `?fecha=YYYY-MM-DD`).

---

## 2. Nuevo Catálogo de Especialidades (`/api/v1/especialidades`)

### Listar especialidades
- **`GET /api/v1/especialidades`** *(disponible para cualquier usuario autenticado)*
- Query param opcional: `?activas=1` para obtener únicamente especialidades activas (recomendado para selectores de citas).
- Ejemplo de elemento en `data`:
  ```json
  {
    "id_especialidad": "uuid",
    "nombre": "Cardiología",
    "descripcion": "Diagnóstico y tratamiento cardiovascular",
    "es_sistema": false,
    "activo": true,
    "medicos_count": 2
  }
  ```
- Especialidades iniciales sembradas:
  - `Emergencia` (`es_sistema: true`)
  - `Medicina general` (`es_sistema: true`)
  - `Alergista`
  - `Pediatría`
  - `Cardiología`
  - `Ginecología`

### Gestión CRUD (Solo web / Administrador General)
- `POST /api/v1/especialidades`
- `PUT /api/v1/especialidades/{id}`
- `DELETE /api/v1/especialidades/{id}`
- Restringido al **Administrador General** (`Jefe de Operaciones` recibe `403`).
- Las especialidades base (`es_sistema: true`) no se pueden renombrar, desactivar ni eliminar (`422`).
- No se puede eliminar una especialidad que tenga médicos o citas asociadas (`422`).

---

## 3. Citas y Disponibilidad por Especialidad

### Consultar disponibilidad
- **`GET /api/v1/agenda/disponibilidad`**
- Ahora acepta:
  - `?id_especialidad=<uuid>&fecha=YYYY-MM-DD` *(nuevo flujo principal)*
  - `?id_medico=<uuid>&fecha=YYYY-MM-DD` *(mantenido por retrocompatibilidad)*
- Calcula los bloques disponibles de 30 minutos según el número de médicos activos de esa especialidad que tengan turno en ese horario (`capacidad > citas ocupadas`).
- Respuesta en `data`:
  ```json
  [
    { "hora_inicio": "08:00", "hora_fin": "08:30" },
    { "hora_inicio": "08:30", "hora_fin": "09:00" }
  ]
  ```

### Agendar cita
- **`POST /api/v1/citas`**
- Roles permitidos: `ADMINISTRADOR`, `MEDICO`, `RECEPCIONISTA`.
- Para agendar por especialidad (Recepción / Admin):
  - Enviar `id_especialidad` (sin `id_medico`).
  - La cita se crea con `id_medico: null` y `id_especialidad: "<uuid>"`.
- Si un `MEDICO` agenda seguimiento de un paciente suyo, sigue asignando su propio `id_medico`.

### Solicitar, confirmar y reprogramar citas
- **`POST /api/v1/solicitudes-atencion`** *(Paciente)*: acepta el campo opcional `id_especialidad` (`uuid`).
- **`PATCH /api/v1/citas/{id}/confirmar`** y **`PATCH /api/v1/citas/{id}/reprogramar`**: aceptan `id_especialidad` o `id_medico`. Si solo se envía `id_especialidad`, `id_medico` queda en `null`.

### Listado de citas y expediente
Aplica a `GET /api/v1/citas`, `GET /api/v1/mi/citas` y `GET /api/v1/mi/expediente`:
- **`id_medico` y `medico` ahora pueden ser `null`** mientras la cita esté abierta por especialidad y ningún médico la haya tomado aún.
- Cada cita incluye `id_especialidad` y el objeto `especialidad: { id_especialidad, nombre }`.
- Cuando un `MEDICO` consulta `GET /api/v1/citas`, recibe tanto sus citas asignadas (`id_medico == su id`) como las citas abiertas de su especialidad (`id_medico == null` y `id_especialidad == su id_especialidad`).

---

## 4. Inicio de Consulta (`MEDICO`)

- **`POST /api/v1/citas/{id}/consulta`**
- Si la cita tiene `id_medico == null` y su `id_especialidad` coincide con la del médico autenticado, el médico puede iniciar la consulta.
- Al iniciarse, el backend **asigna automáticamente `cita.id_medico = medico.id`** y cambia el estado de la cita a `EN_ATENCION`.
- Si otro médico de la especialidad la tomó antes, responde `409 Conflict` (`"Otro médico ya tomó esta cita."`).

---

## 5. Endpoint Público

- **`GET /api/v1/publico/medicos-de-turno`**
- Cada elemento ahora devuelve:
  ```json
  {
    "nombre": "Dr. Nombre",
    "cargo": "Cardiología",
    "especialidad": "Cardiología",
    "area": "Consulta Externa"
  }
  ```
- Para médicos, `cargo` conserva la especialidad por retrocompatibilidad y además se expone `especialidad` explícitamente.

---

## Checklist rápido para Android / iOS

1. **DTO de `Cita`:** hacer **nullable** `id_medico` y `medico` para evitar errores de deserialización cuando una cita aún no tiene médico asignado.
2. **DTO de `User`:** agregar `id_especialidad`, `especialidad_nombre` y `especialidad` (nullable); usar `especialidad_nombre` para mostrar la especialidad en perfiles `MEDICO`.
3. **Flujo de nueva cita / disponibilidad:** poblar el selector desde `GET /api/v1/especialidades?activas=1` y consultar horarios con `GET /api/v1/agenda/disponibilidad?id_especialidad=...&fecha=...`.
4. **Flujo del médico:** contemplar en la agenda del médico las citas con `id_medico == null` de su especialidad y refrescar la cita al iniciar consulta (`POST /api/v1/citas/{id}/consulta`).
