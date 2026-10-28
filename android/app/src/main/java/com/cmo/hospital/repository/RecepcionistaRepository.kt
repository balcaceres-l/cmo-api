package com.cmo.hospital.repository

import android.content.Context
import com.cmo.hospital.data.model.*
import com.cmo.hospital.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecepcionistaRepository(private val context: Context) {
    private val apiService = RetrofitClient.getApiService(context)

    // Estado en memoria de las 17 camas para interacción en tiempo real
    private val _camasState = MutableStateFlow<List<Cama>>(getInitialCamas())
    val camasState: StateFlow<List<Cama>> = _camasState.asStateFlow()

    // Estado en memoria de las citas
    private val _citasState = MutableStateFlow<List<Cita>>(getMockCitas())
    val citasState: StateFlow<List<Cita>> = _citasState.asStateFlow()

    // Turno activo en el llamador de sala
    private val _turnoEnBox = MutableStateFlow<Cita?>(_citasState.value.firstOrNull { it.estado == "EN_ATENCION" } ?: _citasState.value.firstOrNull())
    val turnoEnBox: StateFlow<Cita?> = _turnoEnBox.asStateFlow()

    // Estado de la ambulancia
    private val _ambulanciaState = MutableStateFlow(getMockAmbulancia())
    val ambulanciaState: StateFlow<AmbulanciaRecurso> = _ambulanciaState.asStateFlow()

    suspend fun getCitas(): List<Cita> {
        return try {
            val res = apiService.getCitas()
            if (res.isSuccessful && res.body()?.success == true) {
                val data = res.body()?.data?.data
                if (!data.isNullOrEmpty()) {
                    _citasState.value = data
                    data
                } else _citasState.value
            } else _citasState.value
        } catch (e: Exception) {
            _citasState.value
        }
    }

    suspend fun agendarCita(
        pacienteNombre: String,
        dui: String,
        fechaNacimiento: String?,
        fecha: String,
        horaInicio: String,
        motivo: String,
        especialidad: String,
        box: String
    ): Result<Cita> {
        val horaFin = Cita.calcularFin30Minutos(horaInicio)
        val nuevaCita = Cita(
            id = "c-" + System.currentTimeMillis().toString().takeLast(6),
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            estado = "AGENDADA",
            motivo = motivo,
            paciente = PacienteInfo(
                id = "p-" + System.currentTimeMillis().toString().takeLast(4),
                nombreCompleto = pacienteNombre,
                numeroExpediente = "EXP-2026-" + (100 + (1..899).random()),
                dui = dui,
                sexo = "M",
                fechaNacimiento = fechaNacimiento
            ),
            consultorio = ConsultorioInfo(
                id = "cs-1",
                nombre = "$especialidad ($box)",
                codigo = box
            )
        )

        return try {
            val payload = mapOf(
                "fecha" to fecha,
                "hora_inicio" to horaInicio,
                "hora_fin" to horaFin,
                "motivo" to motivo
            )
            apiService.agendarCita(payload)
            _citasState.value = listOf(nuevaCita) + _citasState.value
            Result.success(nuevaCita)
        } catch (e: Exception) {
            _citasState.value = listOf(nuevaCita) + _citasState.value
            Result.success(nuevaCita)
        }
    }

    suspend fun marcarLlegada(citaId: String): Boolean {
        try {
            apiService.registrarLlegada(citaId)
        } catch (e: Exception) {}

        _citasState.value = _citasState.value.map {
            if (it.id == citaId) it.copy(estado = "EN_ESPERA") else it
        }
        return true
    }

    suspend fun cancelarCita(citaId: String, motivo: String): Boolean {
        try {
            apiService.cancelarCita(citaId, mapOf("motivo_cancelacion" to motivo))
        } catch (e: Exception) {}

        _citasState.value = _citasState.value.map {
            if (it.id == citaId) it.copy(estado = "CANCELADA") else it
        }
        return true
    }

    fun reLlamarTurno() {
        val actual = _turnoEnBox.value ?: return
        _turnoEnBox.value = actual
    }

    fun siguienteTurno() {
        val lista = _citasState.value
        val siguiente = lista.firstOrNull { it.estado == "EN_ESPERA" }
            ?: lista.firstOrNull { it.estado == "AGENDADA" }

        if (siguiente != null) {
            _turnoEnBox.value = siguiente
            _citasState.value = lista.map {
                if (it.id == siguiente.id) it.copy(estado = "EN_ATENCION") else it
            }
        }
    }

    suspend fun registrarEIngresarPaciente(
        paciente: Paciente,
        signosVitales: SignosVitales?,
        codigoCama: String?,
        motivoIngreso: String,
        esEmergenciaDirecta: Boolean
    ): Result<String> {
        // Validación de sobrecupo de camas si no es ambulatorio
        if (!codigoCama.isNullOrBlank()) {
            val cama = _camasState.value.find { it.codigo == codigoCama }
            if (cama == null || cama.estado != "DISPONIBLE") {
                return Result.failure(Exception("La cama $codigoCama ya se encuentra ocupada."))
            }

            // Ocupar cama
            _camasState.value = _camasState.value.map {
                if (it.codigo == codigoCama) {
                    it.copy(
                        estado = "OCUPADA",
                        paciente = PacienteInfo(
                            id = paciente.id,
                            nombreCompleto = paciente.nombreCompleto,
                            numeroExpediente = paciente.numeroExpediente ?: "EXP-2026-00042",
                            dui = paciente.dui,
                            sexo = paciente.sexo,
                            fechaNacimiento = paciente.fechaNacimiento
                        )
                    )
                } else it
            }
        }

        // Si es emergencia con pase directo, añadirlo a la cola de atención inmediata
        if (esEmergenciaDirecta) {
            val citaEmergencia = Cita(
                id = "c-eme-" + System.currentTimeMillis().toString().takeLast(4),
                fecha = java.time.LocalDate.now().toString(),
                horaInicio = "Ahora",
                horaFin = null,
                estado = "EN_ATENCION",
                motivo = "EMERGENCIA CRÍTICA: " + motivoIngreso,
                paciente = PacienteInfo(
                    id = paciente.id,
                    nombreCompleto = paciente.nombreCompleto,
                    numeroExpediente = paciente.numeroExpediente ?: "EXP-2026-EME",
                    dui = paciente.dui,
                    sexo = paciente.sexo,
                    fechaNacimiento = paciente.fechaNacimiento
                ),
                consultorio = ConsultorioInfo("cs-box3", "Box Urgencias 03", "Box 03"),
                signosVitales = signosVitales
            )
            _citasState.value = listOf(citaEmergencia) + _citasState.value
            _turnoEnBox.value = citaEmergencia
        }

        try {
            val req = mapOf(
                "nombre_completo" to paciente.nombreCompleto,
                "dui" to paciente.dui,
                "fecha_nacimiento" to paciente.fechaNacimiento,
                "sexo" to paciente.sexo,
                "telefono" to paciente.telefono,
                "direccion" to paciente.direccion
            )
            apiService.registrarPaciente(req)
        } catch (e: Exception) {}

        return Result.success("Paciente ingresado exitosamente.")
    }

    suspend fun cambiarEstadoAmbulancia(nuevoEstado: String) {
        _ambulanciaState.value = _ambulanciaState.value.copy(estado = nuevoEstado)
    }

    suspend fun solicitarDespachoAmbulancia(direccion: String, paciente: String, descripcion: String, prioridad: String) {
        val servicio = ServicioAmbulanciaDetalle(
            id = "serv-" + System.currentTimeMillis().toString().takeLast(4),
            pacienteNombre = paciente,
            direccion = direccion,
            descripcion = descripcion,
            prioridad = prioridad,
            horaSalida = "12:35",
            estado = "EN_CURSO",
            destino = "Hosp. Universitario - Urgencias",
            minutosLlegada = 15
        )
        _ambulanciaState.value = _ambulanciaState.value.copy(
            estado = "EN_SERVICIO",
            servicioActivo = servicio
        )
    }

    private fun getInitialCamas(): List<Cama> {
        // Planta 3: VIP Individuales (5 camas: VIP-301 a VIP-305)
        val vips = (301..305).map { num ->
            val cod = "VIP-$num"
            val ocupada = num != 302 // Solo VIP-302 disponible
            Cama(
                id = "cama-$cod",
                codigo = cod,
                estado = if (ocupada) "OCUPADA" else "DISPONIBLE",
                paciente = if (ocupada) PacienteInfo("p-$num", "Paciente VIP $num", "EXP-2026-000$num", "0482910$num-1", "M") else null
            )
        }

        // Planta 2: Semiprivadas (6 salas x 2 camas = 12 camas)
        val semis = (201..206).flatMap { num ->
            listOf("A", "B").map { letra ->
                val cod = "HAB-$num-$letra"
                val ocupada = !(num == 202 && letra == "B") && !(num == 205 && letra == "A") // 2 camas disponibles
                Cama(
                    id = "cama-$cod",
                    codigo = cod,
                    estado = if (ocupada) "OCUPADA" else "DISPONIBLE",
                    paciente = if (ocupada) PacienteInfo("p-$num-$letra", "Paciente $cod", "EXP-2026-00$num", "023910$num-4", "F") else null
                )
            }
        }

        return vips + semis
    }

    private fun getMockCitas(): List<Cita> {
        return listOf(
            Cita(
                id = "c-101",
                fecha = "2026-10-28",
                horaInicio = "08:30",
                horaFin = "09:00",
                estado = "EN_ATENCION",
                motivo = "Control de hipertensión y dolor torácico leve",
                paciente = PacienteInfo("p-1", "Carlos Mendoza", "EXP-2026-000104", "14.891.203-K", "M", "1981-05-14"),
                consultorio = ConsultorioInfo("cs-1", "Medicina Interna", "Box 03"),
                signosVitales = SignosVitales(
                    presionSistolica = 120,
                    presionDiastolica = 80,
                    frecuenciaCardiaca = 78,
                    frecuenciaRespiratoria = 18,
                    temperaturaC = 36.6,
                    pesoKg = 74.0,
                    tallaCm = 172.0,
                    saturacionOxigeno = 98,
                    observaciones = "Normotenso en reposo, hemodinámicamente estable"
                )
            ),
            Cita(
                id = "c-102",
                fecha = "2026-10-28",
                horaInicio = "09:15",
                horaFin = "09:45",
                estado = "EN_ESPERA",
                motivo = "Cefalea persistente y mareos matutinos",
                paciente = PacienteInfo("p-2", "Lucía Morales Castillo", "EXP-2026-000108", "16.782.341-8", "F", "1992-08-20"),
                consultorio = ConsultorioInfo("cs-1", "Medicina General", "Box 03"),
                signosVitales = SignosVitales(
                    presionSistolica = 110,
                    presionDiastolica = 70,
                    frecuenciaCardiaca = 82,
                    saturacionOxigeno = 99
                )
            ),
            Cita(
                id = "c-103",
                fecha = "2026-10-28",
                horaInicio = "10:00",
                horaFin = "10:30",
                estado = "AGENDADA",
                motivo = "Evaluación de dermatitis y rinitis alérgica",
                paciente = PacienteInfo("p-3", "Rodrigo Valenzuela", "EXP-2026-000115", "05192834-9", "M", "2001-11-03"),
                consultorio = ConsultorioInfo("cs-1", "Alergología", "Box 02")
            ),
            Cita(
                id = "c-104",
                fecha = "2026-10-28",
                horaInicio = "07:30",
                horaFin = "08:00",
                estado = "ATENDIDA",
                motivo = "Seguimiento post-alta bronquitis aguda",
                paciente = PacienteInfo("p-4", "Elena Beatriz Campos", "EXP-2026-000092", "01928374-1", "F", "1975-02-17"),
                consultorio = ConsultorioInfo("cs-1", "Medicina Interna", "Box 03")
            )
        )
    }

    private fun getMockAmbulancia(): AmbulanciaRecurso {
        return AmbulanciaRecurso(
            id = "amb-1",
            placa = "P-123456",
            estado = "EN_SERVICIO",
            servicioActivo = ServicioAmbulanciaDetalle(
                id = "srv-991",
                pacienteNombre = "Adulto mayor politraumatizado",
                direccion = "Av. Libertador #412",
                descripcion = "Traslado UCI Coronario urgente",
                prioridad = "CRITICA",
                horaSalida = "12:20",
                minutosLlegada = 18,
                tripulacionParamedico = "M. Peralta (Par.)",
                tripulacionChofer = "G. Soto (Chofer)",
                destino = "Hosp. Universitario - Ala B",
                reportes = listOf(
                    ReporteAmbulancia(
                        estadoPaciente = "CRITICO",
                        observaciones = "Paciente con soporte O2 alto flujo y monitoreo ECG activo.",
                        minutosLlegada = 18,
                        creadoEn = "12:24"
                    )
                )
            )
        )
    }

    fun getPersonalGuardia(): List<PersonalGuardia> {
        return listOf(
            PersonalGuardia("1", "Dr. Alejandro Restrepo", "MEDICO", "Urgencias / Med. Interna", "07:00", "15:00", "Box 03", "Activo", "2h 18m"),
            PersonalGuardia("2", "Dra. Sofía Valenzuela", "MEDICO", "Alergista / Ambulatorio", "08:30", "14:30", "Box 02", "En Consulta", "Cierre 14:00"),
            PersonalGuardia("3", "Lic. Mariana Gómez", "RECEPCIONISTA", "Enfermera Jefe 3ª Planta VIP", "07:00", "19:00", "Planta 3", "Ronda Médica", "Turno 12h"),
            PersonalGuardia("4", "Enf. Carlos Duarte", "RECEPCIONISTA", "Asistente 2ª Planta Semiprivada", "07:00", "15:00", "Planta 2", "Activo", "2h 18m"),
            PersonalGuardia("5", "Roberto Morales", "RECEPCIONISTA", "Recepcionista Admisión Central", "07:00", "15:00", "Admisión", "En Mostrador", "2h 18m"),
            PersonalGuardia("6", "Dr. Fernando Castro", "MEDICO", "Médico de Emergencia", "07:00", "19:00", "Ambulancia Alfa-1", "En Traslado", "En ruta")
        )
    }
}
