package com.cmo.hospital.core.data.datasource

import com.cmo.hospital.core.data.mapper.aOrdenLaboratorio
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.dto.ConsultaDto
import com.cmo.hospital.core.network.dto.ExamenDto
import com.cmo.hospital.core.network.dto.OrdenLaboratorioDto
import com.cmo.hospital.core.network.dto.RecetaDto
import com.cmo.hospital.core.network.llamarApi
import com.cmo.hospital.core.network.llamarApiSinDatos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Fuente de datos reactiva compartida en memoria y sincronizada con ApiService.
 * Permite que los eventos clínicos entre Recepcionista (Citas, Admisión, Emergencia Directa)
 * y Doctor (Consulta Activa 30m, Expedientes, Laboratorio LIS, Seguimiento) se reflejen en tiempo real.
 */
class HospitalClinicalDataSource(
    private val apiService: ApiService? = null
) {
    private val _camasState = MutableStateFlow(seedInitialCamas())
    val camasState: StateFlow<List<Cama>> = _camasState.asStateFlow()

    private val _citasState = MutableStateFlow(seedInitialCitas())
    val citasState: StateFlow<List<Cita>> = _citasState.asStateFlow()

    private val _turnoEnBox = MutableStateFlow(
        _citasState.value.firstOrNull { it.estado == "EN_ATENCION" } ?: _citasState.value.firstOrNull()
    )
    val turnoEnBox: StateFlow<Cita?> = _turnoEnBox.asStateFlow()

    private val _ambulanciaState = MutableStateFlow(seedInitialAmbulancia())
    val ambulanciaState: StateFlow<AmbulanciaRecurso> = _ambulanciaState.asStateFlow()

    // Sin consulta de ejemplo: la consulta real se abre o retoma desde una cita de la API.
    private val _consultaActiva = MutableStateFlow(ConsultaActiva.vacia())
    val consultaActiva: StateFlow<ConsultaActiva> = _consultaActiva.asStateFlow()

    private val _ordenesLab = MutableStateFlow(seedInitialOrdenesLab())
    val ordenesLab: StateFlow<List<OrdenLaboratorioItem>> = _ordenesLab.asStateFlow()

    private val _examenesCatalogo = MutableStateFlow<List<ExamenDto>>(emptyList())
    val examenesCatalogo: StateFlow<List<ExamenDto>> = _examenesCatalogo.asStateFlow()

    private val _expedientes = MutableStateFlow(seedInitialExpedientes())
    val expedientes: StateFlow<List<ExpedienteDetalle>> = _expedientes.asStateFlow()

    private val _expedienteSolicitado = MutableStateFlow<String?>(null)
    val expedienteSolicitado: StateFlow<String?> = _expedienteSolicitado.asStateFlow()

    private val _personalGuardia = seedPersonalGuardia()

    init {
        _citasState.value.forEach { registrarPacienteDeCitaEnExpedientes(it) }
    }

    fun getPersonalGuardia(): List<PersonalGuardia> = _personalGuardia

    private fun registrarPacienteDeCitaEnExpedientes(cita: Cita) {
        val pInfo = cita.paciente ?: return
        val pac = Paciente(
            id = pInfo.id,
            nombreCompleto = pInfo.nombreCompleto,
            numeroExpediente = pInfo.numeroExpediente
                ?: Paciente.generarNumeroExpediente(pInfo.nombreCompleto),
            dui = pInfo.dui,
            sexo = pInfo.sexo,
            fechaNacimiento = pInfo.fechaNacimiento,
            telefono = pInfo.telefono,
            alergias = pInfo.alergias,
            tipoSangre = pInfo.tipoSangre,
            protegido = pInfo.protegido
        )
        ensurePacienteEnExpedientes(
            paciente = pac,
            ubicacion = cita.consultorio?.codigo ?: "Consulta Externa",
            motivoReciente = cita.motivo,
            signos = cita.signosVitales
        )
    }

    fun solicitarAperturaExpediente(numeroExpediente: String?) {
        if (!numeroExpediente.isNullOrBlank()) {
            _citasState.value.firstOrNull { cita ->
                val exp = cita.paciente?.numeroExpediente
                    ?: cita.paciente?.nombreCompleto?.let { Paciente.generarNumeroExpediente(it) }
                exp.equals(numeroExpediente, ignoreCase = true)
            }?.let { registrarPacienteDeCitaEnExpedientes(it) }
        }
        _expedienteSolicitado.value = numeroExpediente
    }

    fun consumirExpedienteSolicitado() {
        _expedienteSolicitado.value = null
    }

    private fun ensurePacienteEnExpedientes(
        paciente: Paciente,
        ubicacion: String? = null,
        motivoReciente: String? = null,
        signos: SignosVitales? = null
    ) {
        val expKey = paciente.numeroExpediente
        val yaExiste = _expedientes.value.any {
            (!expKey.isNullOrBlank() && it.paciente.numeroExpediente == expKey) ||
                (!paciente.id.isNullOrBlank() && it.paciente.id == paciente.id)
        }
        if (!yaExiste) {
            val nuevo = ExpedienteDetalle(
                paciente = paciente,
                cama = ubicacion ?: "Consulta Externa HUC",
                signosVitales = signos ?: SignosVitales(),
                alergiasCriticas = paciente.alergias?.takeIf { it.isNotBlank() }?.let { listOf(it) }
                    ?: emptyList(),
                comorbilidades = emptyList(),
                atencionesPrevias = if (!motivoReciente.isNullOrBlank()) {
                    listOf(
                        AtencionHistorica(
                            fecha = LocalDate.now().toString(),
                            hora = obtenerHoraActual(),
                            especialidad = "Consulta Externa",
                            diagnostico = motivoReciente,
                            medico = "Cuerpo Médico HUC",
                            notas = "Cita registrada en agenda hospitalaria."
                        )
                    )
                } else emptyList(),
                ordenesLaboratorio = _ordenesLab.value.filter {
                    (!expKey.isNullOrBlank() && it.numeroExpediente == expKey) ||
                        (!paciente.id.isNullOrBlank() && it.pacienteId == paciente.id)
                }
            )
            _expedientes.value = listOf(nuevo) + _expedientes.value
        }
    }

    suspend fun syncPacientesFromRemote(): List<ExpedienteDetalle> {
        try {
            val res = apiService?.getPacientes()
            if (res != null && res.isSuccessful && res.body()?.success == true) {
                val remotos = res.body()?.data?.data.orEmpty()
                remotos.forEach { pac ->
                    val conExp = pac.copy(
                        numeroExpediente = pac.numeroExpediente
                            ?: Paciente.generarNumeroExpediente(pac.nombreCompleto)
                    )
                    ensurePacienteEnExpedientes(conExp)
                }
            }
        } catch (_: Exception) {
        }
        return _expedientes.value
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Citas & Llamador de Sala
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun syncCitasFromRemote(): List<Cita> {
        return try {
            val res = apiService?.getCitas()
            if (res != null && res.isSuccessful && res.body()?.success == true) {
                // Con respuesta válida la API manda, aunque venga vacía: se descartan las citas de ejemplo.
                val remoteList = res.body()?.data?.data.orEmpty()
                run {
                    val normalized = remoteList.mapIndexed { idx, raw ->
                        val safeId: String = (raw.id as String?) ?: "c-rem-$idx"
                        val hi = raw.horaInicio.take(5)
                        val hf = raw.horaFin?.take(5) ?: Cita.calcularFin30Minutos(hi)
                        val citaNorm = raw.copy(
                            id = safeId,
                            horaInicio = hi,
                            horaFin = hf,
                            medicoNombre = raw.medico?.nombreCompleto ?: raw.medicoNombre ?: "Dr. Alejandro Restrepo",
                            especialidad = raw.medico?.cargo ?: raw.especialidad ?: "Medicina General",
                            consultorio = raw.consultorio ?: ConsultorioInfo("cs-1", "Consultorio General", "Box 03")
                        )
                        citaNorm.paciente?.let { pInfo ->
                            val pac = Paciente(
                                id = pInfo.id,
                                nombreCompleto = pInfo.nombreCompleto,
                                numeroExpediente = pInfo.numeroExpediente
                                    ?: Paciente.generarNumeroExpediente(pInfo.nombreCompleto),
                                dui = pInfo.dui,
                                sexo = pInfo.sexo,
                                fechaNacimiento = pInfo.fechaNacimiento,
                                telefono = pInfo.telefono,
                                alergias = pInfo.alergias,
                                tipoSangre = pInfo.tipoSangre,
                                protegido = pInfo.protegido
                            )
                            ensurePacienteEnExpedientes(
                                paciente = pac,
                                ubicacion = citaNorm.consultorio?.codigo ?: "Consulta Externa",
                                motivoReciente = citaNorm.motivo,
                                signos = citaNorm.signosVitales
                            )
                        }
                        citaNorm
                    }
                    // Conservar también citas locales creadas en sesión actual (ej. emergencias o controles recién agendados)
                    val remoteIds = normalized.map { it.id }.toSet()
                    val localesEnSesion = _citasState.value.filter {
                        (it.id.startsWith("c-eme-") || it.id.startsWith("c-seg-")) && it.id !in remoteIds
                    }
                    val combinadas = localesEnSesion + normalized
                    _citasState.value = combinadas

                    // Retoma la consulta que el médico dejó abierta en la API (p. ej. tras reiniciar la app).
                    val activaRemota = combinadas.firstOrNull {
                        it.estado == "EN_ATENCION" && it.consultaRef?.abierta == true
                    }
                    if (activaRemota != null) {
                        _turnoEnBox.value = activaRemota
                        val actual = _consultaActiva.value
                        if (actual.citaId != activaRemota.id || actual.finalizada || !actual.esRemota) {
                            cargarConsultaRemota(activaRemota.consultaRef!!.id, activaRemota)
                        }
                    } else {
                        _turnoEnBox.value = combinadas.firstOrNull()
                        if (_consultaActiva.value.citaId !in combinadas.map { it.id }) {
                            _consultaActiva.value = _consultaActiva.value.copy(finalizada = true)
                        }
                    }
                }
            }
            _citasState.value
        } catch (_: Exception) {
            _citasState.value
        }
    }

    fun prependCita(cita: Cita) {
        registrarPacienteDeCitaEnExpedientes(cita)
        _citasState.value = listOf(cita) + _citasState.value
    }

    suspend fun agendarCitaRemotaOLocal(nuevaCita: Cita) {
        try {
            apiService?.agendarCita(
                mapOf(
                    "fecha" to nuevaCita.fecha,
                    "hora_inicio" to nuevaCita.horaInicio,
                    "hora_fin" to nuevaCita.horaFin,
                    "motivo" to nuevaCita.motivo
                )
            )
        } catch (_: Exception) {
        }
        prependCita(nuevaCita)
    }

    suspend fun marcarLlegada(citaId: String) {
        try {
            apiService?.registrarLlegada(citaId)
        } catch (_: Exception) {
        }
        _citasState.value = _citasState.value.map {
            if (it.id == citaId) it.copy(estado = "EN_ESPERA") else it
        }
    }

    suspend fun cancelarCita(citaId: String, motivo: String) {
        try {
            apiService?.cancelarCita(citaId, mapOf("motivo_cancelacion" to motivo))
        } catch (_: Exception) {
        }
        _citasState.value = _citasState.value.map {
            if (it.id == citaId) it.copy(estado = "CANCELADA") else it
        }
    }

    fun actualizarTriajeOPasarAConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ): Cita? {
        var citaTransferida: Cita? = null
        _citasState.value = _citasState.value.map { cita ->
            if (cita.id == citaId) {
                val nuevosSignos = if (signosVitales?.hasAnyData == true) signosVitales else cita.signosVitales
                val actualizada = cita.copy(
                    signosVitales = nuevosSignos,
                    estado = if (pasarAConsulta) "EN_ATENCION" else cita.estado
                )
                if (pasarAConsulta) {
                    citaTransferida = actualizada
                }
                actualizada
            } else cita
        }

        if (pasarAConsulta && citaTransferida != null) {
            _turnoEnBox.value = citaTransferida
            iniciarConsultaDesdeCita(citaTransferida!!)
        }
        return citaTransferida
    }

    fun reLlamarTurno(): Cita? {
        val actual = _turnoEnBox.value ?: return null
        _turnoEnBox.value = actual.copy()
        return actual
    }

    fun siguienteTurno(): Cita? {
        val lista = _citasState.value
        val siguiente = lista.firstOrNull { it.estado == "EN_ESPERA" }
            ?: lista.firstOrNull { it.estado == "AGENDADA" }
        if (siguiente != null) {
            val enAtencion = siguiente.copy(estado = "EN_ATENCION")
            _turnoEnBox.value = enAtencion
            _citasState.value = lista.map { if (it.id == siguiente.id) enAtencion else it }
            iniciarConsultaDesdeCita(enAtencion)
            return enAtencion
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admisión, 17 Camas & Emergencia Directa
    // ─────────────────────────────────────────────────────────────────────────

    fun construirCapacidadHospitalaria(): CapacidadHospitalaria {
        val todas = _camasState.value
        val vips = todas.filter { it.esVip }
        val semis = todas.filter { !it.esVip }
        val salasMap = semis.groupBy { cama ->
            // "HAB-201-A" -> "HAB-201"
            cama.codigo.substringBeforeLast("-")
        }
        val salasUi = salasMap.entries.sortedBy { it.key }.mapNotNull { (codigoSala, camasSala) ->
            val camaA = camasSala.firstOrNull { it.codigo.endsWith("-A") }
            val camaB = camasSala.firstOrNull { it.codigo.endsWith("-B") }
            if (camaA != null && camaB != null) {
                SalaSemiprivadaUi(codigoSala = codigoSala, camaA = camaA, camaB = camaB)
            } else null
        }
        val ocupadas = todas.count { !it.estaDisponible }
        val disponibles = todas.count { it.estaDisponible }
        return CapacidadHospitalaria(
            totalCamas = todas.size,
            ocupadas = ocupadas,
            disponibles = disponibles,
            vipCamas = vips,
            salasSemiprivadas = salasUi
        )
    }

    suspend fun registrarEIngresarPaciente(
        paciente: Paciente,
        signosVitales: SignosVitales?,
        codigoCama: String?,
        motivoIngreso: String,
        esEmergenciaDirecta: Boolean
    ): Result<Paciente> {
        val expedienteGenerado = paciente.numeroExpediente
            ?: Paciente.generarNumeroExpediente(paciente.nombreCompleto, 2026, (_expedientes.value.size + 5))
        val pacienteConExp = paciente.copy(
            id = paciente.id ?: ("p-" + System.currentTimeMillis().toString().takeLast(5)),
            numeroExpediente = expedienteGenerado
        )

        if (!codigoCama.isNullOrBlank()) {
            val cama = _camasState.value.find { it.codigo == codigoCama }
            if (cama == null || !cama.estaDisponible) {
                return Result.failure(
                    IllegalStateException("Sobrecupo bloqueado: La cama $codigoCama no está disponible.")
                )
            }
            _camasState.value = _camasState.value.map {
                if (it.codigo == codigoCama) {
                    it.copy(
                        estado = "OCUPADA",
                        paciente = PacienteInfo(
                            id = pacienteConExp.id,
                            nombreCompleto = pacienteConExp.nombreCompleto,
                            numeroExpediente = pacienteConExp.numeroExpediente,
                            dui = pacienteConExp.dui,
                            sexo = pacienteConExp.sexo,
                            fechaNacimiento = pacienteConExp.fechaNacimiento,
                            protegido = pacienteConExp.protegido
                        )
                    )
                } else it
            }
        }

        // Registrar o actualizar ficha en Expedientes Clínicos
        val nuevoExp = ExpedienteDetalle(
            paciente = pacienteConExp,
            cama = when {
                esEmergenciaDirecta -> "Box 03 (Urgencia Directa • ${pacienteConExp.nivelTriage.titulo})"
                !codigoCama.isNullOrBlank() -> "Cama $codigoCama (${if (codigoCama.startsWith("VIP")) "Planta 3 VIP" else "Planta 2 Semiprivada"})"
                else -> "Admisión Ambulatoria"
            },
            signosVitales = signosVitales ?: SignosVitales(),
            alergiasCriticas = pacienteConExp.alergias?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList(),
            comorbilidades = listOf("Triage: ${pacienteConExp.nivelTriage.titulo}"),
            atencionesPrevias = listOf(
                AtencionHistorica(
                    fecha = "Hoy",
                    hora = obtenerHoraActual(),
                    especialidad = if (esEmergenciaDirecta) "Urgencias Adultos" else "Admisión Hospitalaria",
                    diagnostico = motivoIngreso,
                    medico = "Dr. Alejandro Restrepo",
                    notas = "Previsión: ${pacienteConExp.prevision ?: "Fonasa Tramo D"}"
                )
            )
        )
        _expedientes.value = listOf(nuevoExp) + _expedientes.value

        if (esEmergenciaDirecta) {
            val citaEmergencia = Cita(
                id = "c-eme-" + System.currentTimeMillis().toString().takeLast(4),
                fecha = LocalDate.now().toString(),
                horaInicio = "Ahora",
                horaFin = null,
                estado = "EN_ATENCION",
                motivo = "EMERGENCIA (${pacienteConExp.nivelTriage.titulo}): $motivoIngreso",
                paciente = PacienteInfo(
                    id = pacienteConExp.id,
                    nombreCompleto = pacienteConExp.nombreCompleto,
                    numeroExpediente = pacienteConExp.numeroExpediente,
                    dui = pacienteConExp.dui,
                    sexo = pacienteConExp.sexo,
                    fechaNacimiento = pacienteConExp.fechaNacimiento,
                    protegido = pacienteConExp.protegido
                ),
                consultorio = ConsultorioInfo("cs-box3", "Box Urgencias 03", "Box 03"),
                signosVitales = signosVitales
            )
            _citasState.value = listOf(citaEmergencia) + _citasState.value
            _turnoEnBox.value = citaEmergencia
            iniciarConsultaDesdeCita(citaEmergencia, pacienteConExp)
        }

        try {
            apiService?.registrarPaciente(
                mapOf(
                    "nombre_completo" to pacienteConExp.nombreCompleto,
                    "dui" to pacienteConExp.dui,
                    "fecha_nacimiento" to pacienteConExp.fechaNacimiento,
                    "sexo" to pacienteConExp.sexo,
                    "telefono" to pacienteConExp.telefono,
                    "direccion" to pacienteConExp.direccion
                )
            )
        } catch (_: Exception) {
        }

        return Result.success(pacienteConExp)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ambulancia Alfa-1
    // ─────────────────────────────────────────────────────────────────────────

    fun cambiarEstadoAmbulancia(nuevoEstado: String) {
        _ambulanciaState.value = _ambulanciaState.value.copy(estado = nuevoEstado)
    }

    suspend fun despacharAmbulancia(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String
    ) {
        val servicio = ServicioAmbulanciaDetalle(
            id = "serv-" + System.currentTimeMillis().toString().takeLast(4),
            pacienteNombre = paciente.ifBlank { "Emergencia Prehospitalaria" },
            direccion = direccion,
            descripcion = descripcion.ifBlank { "Despacho Código Rojo" },
            prioridad = prioridad,
            horaSalida = obtenerHoraActual(),
            estado = "EN_CURSO",
            destino = "Hosp. Universitario - Urgencias",
            minutosLlegada = 15
        )
        try {
            apiService?.despacharAmbulancia(
                mapOf(
                    "direccion" to direccion,
                    "paciente_nombre" to servicio.pacienteNombre,
                    "descripcion" to servicio.descripcion,
                    "prioridad" to prioridad
                )
            )
        } catch (_: Exception) {
        }
        _ambulanciaState.value = _ambulanciaState.value.copy(
            estado = "EN_SERVICIO",
            servicioActivo = servicio
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Doctor: Consulta Activa (Semáforo 30m), CIE-10, Recetas y Laboratorio LIS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Abre la consulta de una cita en la API, o la retoma si ya estaba abierta.
     * Si la API la rechaza (otra fecha, cita cancelada, etc.) el motivo llega a la pantalla.
     */
    suspend fun iniciarORetomarConsultaDoctor(citaId: String): Result<ConsultaActiva> {
        val cita = _citasState.value.firstOrNull { it.id == citaId }
            ?: return Result.failure(IllegalArgumentException("No se encontró la cita seleccionada."))
        if (!cita.esRemota) {
            return Result.failure(
                IllegalStateException("Esta cita no está registrada en el sistema; solo las citas de la agenda del hospital admiten consulta.")
            )
        }

        val actual = _consultaActiva.value
        if (actual.citaId == cita.id && !actual.finalizada && actual.esRemota) {
            return Result.success(actual)
        }

        val api = apiService ?: return Result.failure(IllegalStateException("Sin conexión con la API."))
        val abierta = cita.consultaRef?.takeIf { it.abierta }
        val res = if (abierta != null) {
            llamarApi { api.getConsulta(abierta.id) }
        } else {
            llamarApi { api.abrirConsulta(cita.id) }
        }
        return res.map { dto -> aplicarConsultaRemota(dto, cita) }
    }

    /** Usado al sincronizar: retoma la consulta abierta sin bloquear la lista de citas. */
    private suspend fun cargarConsultaRemota(consultaId: String, cita: Cita) {
        val api = apiService ?: return
        llamarApi { api.getConsulta(consultaId) }.onSuccess { aplicarConsultaRemota(it, cita) }
    }

    /** Carga en pantalla lo que la API tiene guardado de la consulta (diagnóstico, notas y recetas). */
    private fun aplicarConsultaRemota(dto: ConsultaDto, cita: Cita): ConsultaActiva {
        val enAtencion = cita.copy(
            estado = if (dto.fechaHoraFin == null) "EN_ATENCION" else "ATENDIDA",
            consultaRef = ConsultaRef(dto.id, dto.fechaHoraFin)
        )
        _citasState.value = _citasState.value.map { if (it.id == cita.id) enAtencion else it }
        _turnoEnBox.value = enAtencion
        iniciarConsultaDesdeCita(enAtencion, remoteConsultaId = dto.id)

        val transcurridos = segundosDesde(dto.fechaHoraInicio)
        val base = _consultaActiva.value
        _consultaActiva.value = base.copy(
            diagnosticosCie10 = ConsultaActiva.diagnosticoDesdeTexto(dto.diagnostico),
            anamnesis = dto.notas ?: base.anamnesis,
            recetas = dto.recetas.orEmpty().map { it.aReceta() },
            ordenesLaboratorio = dto.ordenesLaboratorio?.map { it.aOrden(base.paciente) }
                ?: _ordenesLab.value.filter { it.idConsulta == dto.id },
            segundosTranscurridos = transcurridos,
            estadoSemaforo = EstadoSemaforo.calcularPorSegundos(transcurridos),
            finalizada = dto.fechaHoraFin != null
        )
        return _consultaActiva.value
    }

    private fun RecetaDto.aReceta() = RecetaItem(
        id = id,
        medicamento = medicamento,
        dosis = dosis,
        frecuencia = frecuencia,
        duracion = duracion,
        indicaciones = indicaciones
    )

    /** El cronómetro continúa desde la hora real de inicio guardada en la API. */
    private fun segundosDesde(iso: String?): Int = try {
        val inicio = OffsetDateTime.parse(iso).toInstant()
        Duration.between(inicio, Instant.now()).seconds.toInt().coerceAtLeast(0)
    } catch (_: Exception) {
        0
    }

    fun iniciarConsultaDesdeCita(
        cita: Cita,
        pacienteOverride: Paciente? = null,
        remoteConsultaId: String? = null
    ) {
        val expExistente = _expedientes.value.firstOrNull { exp ->
            (!cita.paciente?.numeroExpediente.isNullOrBlank() &&
                exp.paciente.numeroExpediente == cita.paciente?.numeroExpediente) ||
                (!cita.paciente?.id.isNullOrBlank() && exp.paciente.id == cita.paciente?.id)
        }

        val pac = pacienteOverride ?: expExistente?.paciente ?: cita.paciente?.let {
            Paciente(
                id = it.id,
                nombreCompleto = it.nombreCompleto,
                numeroExpediente = it.numeroExpediente
                    ?: Paciente.generarNumeroExpediente(it.nombreCompleto),
                dui = it.dui,
                sexo = it.sexo,
                fechaNacimiento = it.fechaNacimiento,
                telefono = it.telefono,
                alergias = it.alergias,
                tipoSangre = it.tipoSangre,
                protegido = it.protegido
            )
        } ?: _consultaActiva.value.paciente

        ensurePacienteEnExpedientes(
            paciente = pac,
            ubicacion = "${cita.consultorio?.codigo ?: "Box 03"} (En Consulta)",
            motivoReciente = cita.motivo,
            signos = cita.signosVitales
        )

        val signosIniciales = when {
            cita.signosVitales?.hasAnyData == true -> cita.signosVitales
            expExistente?.signosVitales?.hasAnyData == true -> expExistente.signosVitales
            else -> SignosVitales()
        }

        val ordenesExistentes = _ordenesLab.value.filter {
            (!pac.numeroExpediente.isNullOrBlank() && it.numeroExpediente == pac.numeroExpediente) ||
                (!pac.id.isNullOrBlank() && it.pacienteId == pac.id)
        }

        _consultaActiva.value = ConsultaActiva(
            id = remoteConsultaId ?: ("cons-" + System.currentTimeMillis().toString().takeLast(4)),
            citaId = cita.id,
            box = cita.consultorio?.codigo ?: "Box 03",
            medicoNombre = cita.medicoNombre ?: "Dr. Alejandro Restrepo",
            especialidad = cita.especialidad ?: "Medicina General",
            paciente = pac,
            signosVitales = signosIniciales,
            diagnosticosCie10 = emptyList(),
            anamnesis = cita.motivo ?: "Paciente ingresa a consulta médica.",
            recetas = emptyList(),
            ordenesLaboratorio = ordenesExistentes,
            segundosTranscurridos = 0,
            duracionTotalSegundos = 30 * 60,
            estadoSemaforo = EstadoSemaforo.VERDE,
            finalizada = false
        )
    }

    fun actualizarSegundosConsulta(segundos: Int) {
        val actual = _consultaActiva.value
        _consultaActiva.value = actual.copy(
            segundosTranscurridos = segundos,
            estadoSemaforo = EstadoSemaforo.calcularPorSegundos(segundos)
        )
    }

    fun actualizarSignosConsulta(signos: SignosVitales) {
        val actual = _consultaActiva.value
        _consultaActiva.value = actual.copy(signosVitales = signos)
        // Sincronizar también en el expediente del paciente activo
        _expedientes.value = _expedientes.value.map { exp ->
            if (exp.paciente.numeroExpediente == actual.paciente.numeroExpediente) {
                exp.copy(signosVitales = signos)
            } else exp
        }
    }

    fun agregarDiagnosticoCie10(codigo: String, descripcion: String) {
        val actual = _consultaActiva.value
        if (actual.diagnosticosCie10.none { it.codigo == codigo }) {
            _consultaActiva.value = actual.copy(
                diagnosticosCie10 = actual.diagnosticosCie10 + Cie10Diagnostico(codigo, descripcion)
            )
        }
    }

    fun eliminarDiagnosticoCie10(codigo: String) {
        val actual = _consultaActiva.value
        _consultaActiva.value = actual.copy(
            diagnosticosCie10 = actual.diagnosticosCie10.filter { it.codigo != codigo }
        )
    }

    fun actualizarAnamnesis(texto: String) {
        _consultaActiva.value = _consultaActiva.value.copy(anamnesis = texto)
    }

    private fun sinConsultaRemota() =
        IllegalStateException("No hay una consulta abierta en el sistema. Iníciela desde Mis Citas.")

    suspend fun agregarReceta(receta: RecetaItem): Result<RecetaItem> {
        val actual = _consultaActiva.value
        val api = apiService
        if (actual.finalizada || !actual.esRemota || api == null) return Result.failure(sinConsultaRemota())
        return llamarApi {
            api.agregarReceta(
                actual.id,
                mapOf(
                    "medicamento" to receta.medicamento,
                    "dosis" to receta.dosis,
                    "frecuencia" to receta.frecuencia,
                    "duracion" to receta.duracion,
                    "indicaciones" to receta.indicaciones
                )
            )
        }.map { dto ->
            // El tipo (crónico, SOS…) es solo de la pantalla; la API no lo guarda.
            val guardada = dto.aReceta().copy(tipo = receta.tipo)
            val a = _consultaActiva.value
            _consultaActiva.value = a.copy(recetas = a.recetas + guardada)
            guardada
        }
    }

    suspend fun eliminarReceta(recetaId: String): Result<Unit> {
        val api = apiService ?: return Result.failure(sinConsultaRemota())
        return llamarApiSinDatos { api.eliminarReceta(recetaId) }.onSuccess {
            val a = _consultaActiva.value
            _consultaActiva.value = a.copy(recetas = a.recetas.filter { it.id != recetaId })
        }
    }

    /** Guarda en la consulta abierta el diagnóstico (códigos CIE-10) y la anamnesis (notas). */
    suspend fun guardarConsultaRemota(): Result<Unit> {
        val actual = _consultaActiva.value
        val api = apiService
        if (actual.finalizada) return Result.success(Unit)
        if (!actual.esRemota || api == null) return Result.failure(sinConsultaRemota())
        return llamarApi {
            api.actualizarConsulta(
                actual.id,
                mapOf(
                    "diagnostico" to ConsultaActiva.diagnosticoComoTexto(actual.diagnosticosCie10),
                    "notas" to actual.anamnesis.ifBlank { null }
                )
            )
        }.map { }
    }

    /** Guarda los últimos cambios y cierra la consulta en la API; solo entonces se marca atendida. */
    suspend fun finalizarConsulta(): Result<String> {
        val actual = _consultaActiva.value
        val api = apiService
        if (actual.finalizada) return Result.failure(IllegalStateException("La consulta ya está cerrada."))
        if (!actual.esRemota || api == null) return Result.failure(sinConsultaRemota())
        guardarConsultaRemota().onFailure { return Result.failure(it) }
        val cierre = llamarApi { api.finalizarConsulta(actual.id) }
        cierre.onFailure { return Result.failure(it) }

        _consultaActiva.value = actual.copy(finalizada = true)
        _citasState.value = _citasState.value.map {
            if (it.id == actual.citaId) {
                it.copy(
                    estado = "ATENDIDA",
                    consultaRef = ConsultaRef(actual.id, cierre.getOrNull()?.fechaHoraFin ?: "cerrada")
                )
            } else it
        }

        // Registrar la atención recién firmada en el historial del expediente del paciente
        val resumenCie = actual.diagnosticosCie10.joinToString(", ") { "${it.codigo} ${it.descripcion}" }
            .ifBlank { "Consulta general evaluada" }
        val resumenRecetas = if (actual.recetas.isNotEmpty()) {
            "Rx: " + actual.recetas.joinToString(", ") { it.medicamento }
        } else {
            "Sin prescripción farmacológica."
        }
        val nuevaAtencion = AtencionHistorica(
            fecha = "Hoy",
            hora = obtenerHoraActual(),
            especialidad = actual.especialidad,
            diagnostico = resumenCie,
            medico = actual.medicoNombre,
            notas = "${actual.anamnesis.take(140)} • $resumenRecetas"
        )
        _expedientes.value = _expedientes.value.map { exp ->
            if (exp.paciente.numeroExpediente == actual.paciente.numeroExpediente ||
                (!actual.paciente.id.isNullOrBlank() && exp.paciente.id == actual.paciente.id)
            ) {
                exp.copy(
                    cama = "Alta de Box (${actual.box})",
                    signosVitales = if (actual.signosVitales.hasAnyData) actual.signosVitales else exp.signosVitales,
                    atencionesPrevias = listOf(nuevaAtencion) + exp.atencionesPrevias
                )
            } else exp
        }

        return Result.success("Consulta guardada y cerrada en el expediente del paciente.")
    }

    // ─── Laboratorio: catálogo y órdenes reales de la API ──────────────────────

    suspend fun syncExamenesCatalogo(): Result<List<ExamenDto>> {
        val api = apiService ?: return Result.failure(IllegalStateException("Sin conexión con la API."))
        return llamarApi { api.getExamenes() }.map { it.data }.onSuccess { _examenesCatalogo.value = it }
    }

    /** Reemplaza las órdenes de ejemplo por las del médico en la API (solo estados, sin resultados). */
    suspend fun syncOrdenesLabFromRemote(): Result<List<OrdenLaboratorioItem>> {
        val api = apiService ?: return Result.failure(IllegalStateException("Sin conexión con la API."))
        return llamarApi { api.getOrdenesLaboratorio() }.map { pagina ->
            val ordenes = pagina.data.map { it.aOrden() }
            _ordenesLab.value = ordenes
            val consulta = _consultaActiva.value
            if (consulta.esRemota) {
                _consultaActiva.value = consulta.copy(
                    ordenesLaboratorio = ordenes.filter { it.idConsulta == consulta.id }
                )
            }
            ordenes
        }
    }

    private fun OrdenLaboratorioDto.aOrden(pacienteConsulta: Paciente? = null): OrdenLaboratorioItem =
        aOrdenLaboratorio(
            nombreExamen = _examenesCatalogo.value.firstOrNull { it.id == idExamen }?.nombre,
            pacienteConsulta = pacienteConsulta
        )

    /**
     * Crea una orden por examen en la consulta abierta. La API exige consulta abierta,
     * así que no se pueden pedir exámenes para un paciente que no está en consulta.
     */
    suspend fun emitirOrdenesLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente?
    ): Result<List<OrdenLaboratorioItem>> {
        val consulta = _consultaActiva.value
        val api = apiService
        if (consulta.finalizada || !consulta.esRemota || api == null) {
            return Result.failure(
                IllegalStateException("Las órdenes de laboratorio se emiten desde una consulta abierta. Inicie la consulta del paciente en Mis Citas.")
            )
        }
        if (paciente?.id != null && paciente.id != consulta.paciente.id) {
            return Result.failure(
                IllegalStateException("Solo puede ordenar exámenes para el paciente en consulta (${consulta.paciente.nombreCompleto}).")
            )
        }
        if (_examenesCatalogo.value.isEmpty()) {
            syncExamenesCatalogo().onFailure { return Result.failure(it) }
        }
        val seleccion = examenes.map { nombre ->
            _examenesCatalogo.value.firstOrNull { it.nombre == nombre }
                ?: return Result.failure(IllegalArgumentException("El examen \"$nombre\" no está en el catálogo del laboratorio."))
        }
        val obs = listOfNotNull(
            OrdenLaboratorioItem.MARCA_STAT.takeIf { prioridad.equals("STAT", ignoreCase = true) },
            observaciones.trim().ifBlank { null }
        ).joinToString(" ").ifBlank { null }

        val creadas = mutableListOf<OrdenLaboratorioItem>()
        var error: Throwable? = null
        for (examen in seleccion) {
            llamarApi {
                api.crearOrdenLaboratorio(consulta.id, mapOf("id_examen" to examen.id, "observaciones" to obs))
            }.fold(
                onSuccess = { creadas += it.aOrden(consulta.paciente).copy(examenNombre = examen.nombre) },
                onFailure = { error = it }
            )
            if (error != null) break
        }

        if (creadas.isNotEmpty()) {
            _ordenesLab.value = creadas + _ordenesLab.value
            val actual = _consultaActiva.value
            _consultaActiva.value = actual.copy(ordenesLaboratorio = creadas + actual.ordenesLaboratorio)
        }
        error?.let { err ->
            val parcial = if (creadas.isEmpty()) "" else "Se emitieron ${creadas.size} de ${seleccion.size}. "
            return Result.failure(IllegalStateException(parcial + (err.message ?: "No se pudo emitir la orden.")))
        }
        return Result.success(creadas)
    }

    /** El médico solo puede cancelar una orden PENDIENTE; iniciar y completar son de recepción/laboratorio. */
    suspend fun cancelarOrdenLaboratorio(ordenId: String): Result<OrdenLaboratorioItem> {
        val api = apiService ?: return Result.failure(IllegalStateException("Sin conexión con la API."))
        return llamarApi { api.cancelarOrdenLaboratorio(ordenId, mapOf("motivo_cancelacion" to null)) }.map { dto ->
            val previa = _ordenesLab.value.firstOrNull { it.id == ordenId }
                ?: _consultaActiva.value.ordenesLaboratorio.firstOrNull { it.id == ordenId }
            val cancelada = (previa?.copy(estado = OrdenLaboratorioItem.estadoDesdeApi(dto.estado)) ?: dto.aOrden())
            val reemplazar: (OrdenLaboratorioItem) -> OrdenLaboratorioItem = { if (it.id == ordenId) cancelada else it }
            _ordenesLab.value = _ordenesLab.value.map(reemplazar)
            val actual = _consultaActiva.value
            _consultaActiva.value = actual.copy(ordenesLaboratorio = actual.ordenesLaboratorio.map(reemplazar))
            cancelada
        }
    }

    private fun obtenerHoraActual(): String {
        return try {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            "12:00"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Datos Semilla Clínicos (17 camas estrictas, citas, consulta y LIS)
    // ─────────────────────────────────────────────────────────────────────────

    private fun seedInitialCamas(): List<Cama> {
        val vips = (301..305).map { num ->
            val cod = "VIP-$num"
            val ocupada = num != 302
            Cama(
                id = "cama-$cod",
                codigo = cod,
                estado = if (ocupada) "OCUPADA" else "DISPONIBLE",
                paciente = if (ocupada) {
                    PacienteInfo("p-$num", "Paciente VIP $num", "PV-2026-0$num", "0482910$num-1", "M")
                } else null
            )
        }

        val semis = (201..206).flatMap { num ->
            listOf("A", "B").map { letra ->
                val cod = "HAB-$num-$letra"
                val ocupada = !(num == 202 && letra == "B") && !(num == 205 && letra == "A")
                Cama(
                    id = "cama-$cod",
                    codigo = cod,
                    estado = if (ocupada) "OCUPADA" else "DISPONIBLE",
                    paciente = if (ocupada) {
                        PacienteInfo("p-$num-$letra", "Paciente $cod", "PH-2026-$num", "023910$num-4", "F")
                    } else null
                )
            }
        }
        return vips + semis
    }

    private fun seedInitialCitas(): List<Cita> = listOf(
        Cita(
            id = "c-101",
            fecha = "2026-10-29",
            horaInicio = "08:30",
            horaFin = "09:00",
            estado = "EN_ATENCION",
            motivo = "Control de hipertensión y dolor torácico leve",
            paciente = PacienteInfo("p-carlos", "Carlos Mendoza", "CM-2026-001", "14.891.203-K", "M", "1981-05-14", protegido = true),
            consultorio = ConsultorioInfo("cs-1", "Medicina Interna", "Box 03"),
            medicoNombre = "Dr. Alejandro Restrepo",
            especialidad = "Medicina Interna",
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
            fecha = "2026-10-29",
            horaInicio = "09:00",
            horaFin = "09:30",
            estado = "EN_ESPERA",
            motivo = "Cefalea persistente y mareos matutinos",
            paciente = PacienteInfo("p-2", "Lucía Morales Castillo", "LM-2026-002", "16.782.341-8", "F", "1992-08-20"),
            consultorio = ConsultorioInfo("cs-1", "Medicina General", "Box 03"),
            medicoNombre = "Dr. Alejandro Restrepo",
            especialidad = "Medicina General",
            signosVitales = SignosVitales(
                presionSistolica = 110,
                presionDiastolica = 70,
                frecuenciaCardiaca = 82,
                saturacionOxigeno = 99
            )
        ),
        Cita(
            id = "c-103",
            fecha = "2026-10-29",
            horaInicio = "10:00",
            horaFin = "10:30",
            estado = "AGENDADA",
            motivo = "Evaluación de dermatitis y rinitis alérgica",
            paciente = PacienteInfo("p-3", "Rodrigo Valenzuela", "RV-2026-003", "05192834-9", "M", "2001-11-03"),
            consultorio = ConsultorioInfo("cs-2", "Alergología", "Box 02"),
            medicoNombre = "Dra. Sofía Valenzuela",
            especialidad = "Medicina General"
        ),
        Cita(
            id = "c-104",
            fecha = "2026-10-29",
            horaInicio = "08:00",
            horaFin = "08:30",
            estado = "ATENDIDA",
            motivo = "Seguimiento post-alta bronquitis aguda",
            paciente = PacienteInfo("p-4", "Elena Beatriz Campos", "EC-2026-004", "01928374-1", "F", "1975-02-17"),
            consultorio = ConsultorioInfo("cs-1", "Medicina Interna", "Box 03"),
            medicoNombre = "Dr. Alejandro Restrepo",
            especialidad = "Medicina Interna"
        )
    )

    private fun seedInitialAmbulancia(): AmbulanciaRecurso = AmbulanciaRecurso(
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

    private fun seedPersonalGuardia(): List<PersonalGuardia> = listOf(
        PersonalGuardia("1", "Dr. Alejandro Restrepo", "MEDICO", "Urgencias / Med. Interna", "07:00", "15:00", "Box 03", "Activo", "2h 18m"),
        PersonalGuardia("2", "Dra. Sofía Valenzuela", "MEDICO", "Alergista / Ambulatorio", "08:30", "14:30", "Box 02", "En Consulta", "Cierre 14:00"),
        PersonalGuardia("3", "Lic. Mariana Gómez", "RECEPCIONISTA", "Enfermera Jefe 3ª Planta VIP", "07:00", "19:00", "Planta 3", "Ronda Médica", "Turno 12h"),
        PersonalGuardia("4", "Enf. Carlos Duarte", "RECEPCIONISTA", "Asistente 2ª Planta Semiprivada", "07:00", "15:00", "Planta 2", "Activo", "2h 18m"),
        PersonalGuardia("5", "Roberto Morales", "RECEPCIONISTA", "Recepcionista Admisión Central", "07:00", "15:00", "Admisión", "En Mostrador", "2h 18m"),
        PersonalGuardia("6", "Dr. Fernando Castro", "MEDICO", "Médico de Emergencia", "07:00", "19:00", "Ambulancia Alfa-1", "En Traslado", "En ruta")
    )

    private fun seedInitialOrdenesLab(): List<OrdenLaboratorioItem> = listOf(
        OrdenLaboratorioItem(
            id = "ord-01",
            pacienteId = "p-mariana",
            pacienteNombre = "Mariana Silva Rivas",
            numeroExpediente = "MS-2026-002",
            examenNombre = "Hemograma Completo + Plaquetas",
            tipoTubo = "Tubo EDTA",
            prioridad = "STAT",
            estado = "En Proceso",
            fechaSolicitud = "Hoy, 10:30",
            observaciones = "Control urgente de hematocrito y leucocitos.",
            medicoSolicitante = "Dr. Alejandro Restrepo"
        ),
        OrdenLaboratorioItem(
            id = "ord-02",
            pacienteId = "p-carlos",
            pacienteNombre = "Carlos Mendoza",
            numeroExpediente = "CM-2026-001",
            examenNombre = "Troponina I Alta Sensibilidad (hs-cTnI)",
            tipoTubo = "Protocolo SCA",
            prioridad = "STAT",
            estado = "En Espera",
            fechaSolicitud = "Hoy, 10:32",
            observaciones = "Cinética enzimática 0h - 1h por dolor torácico.",
            medicoSolicitante = "Dr. Alejandro Restrepo"
        ),
        OrdenLaboratorioItem(
            id = "ord-03",
            pacienteId = "p-luis",
            pacienteNombre = "Luis Barillas",
            numeroExpediente = "LB-2026-003",
            examenNombre = "Gasometría Arterial y Electrolitos",
            tipoTubo = "Heparina Li",
            prioridad = "STAT",
            estado = "Realizado",
            fechaSolicitud = "Hoy, 09:15",
            fechaResultado = "Hoy, 09:42",
            observaciones = "pH: 7.39, pO2: 92, pCO2: 38, Lactato 1.1 mmol/L.",
            medicoSolicitante = "Dr. Alejandro Restrepo"
        ),
        OrdenLaboratorioItem(
            id = "ord-04",
            pacienteId = "p-luis",
            pacienteNombre = "Luis Barillas",
            numeroExpediente = "LB-2026-003",
            examenNombre = "Perfil Lipídico & Glucemia en Ayunas",
            tipoTubo = "Suero Gel",
            prioridad = "RUTINA",
            estado = "Agendado",
            fechaSolicitud = "Hoy, 08:00",
            observaciones = "Colesterol total, HDL, LDL, Triglicéridos y Glucosa basal.",
            medicoSolicitante = "Dr. Alejandro Restrepo"
        ),
        OrdenLaboratorioItem(
            id = "ord-05",
            pacienteId = "p-mariana",
            pacienteNombre = "Mariana Silva Rivas",
            numeroExpediente = "MS-2026-002",
            examenNombre = "Urocultivo & Antibiograma Automatizado",
            tipoTubo = "Frasco Estéril",
            prioridad = "RUTINA",
            estado = "Cancelado",
            fechaSolicitud = "Ayer, 16:40",
            observaciones = "Cancelado a solicitud médica: muestra contaminada, se repetirá.",
            medicoSolicitante = "Dr. Alejandro Restrepo"
        )
    )

    private fun seedInitialExpedientes(): List<ExpedienteDetalle> {
        val todasOrdenes = seedInitialOrdenesLab()
        return listOf(
            ExpedienteDetalle(
                paciente = Paciente(
                    id = "p-carlos",
                    nombreCompleto = "Carlos Mendoza",
                    fechaNacimiento = "1981-05-14",
                    dui = "14.891.203-K",
                    sexo = "M",
                    telefono = "+503 7123-4567",
                    numeroExpediente = "CM-2026-001",
                    prevision = "Fonasa Tramo D"
                ),
                cama = "Box 03 (En Consulta)",
                signosVitales = SignosVitales(120, 80, 78, 18, 36.6, 74.0, 172.0, 98),
                alergiasCriticas = listOf("Alergia a Dipirona / AINEs"),
                comorbilidades = listOf("Hipertensión Arterial"),
                atencionesPrevias = listOf(
                    AtencionHistorica("Hoy", "08:30", "Medicina Interna", "I10 - Hipertensión", "Dr. Alejandro Restrepo", "Consulta en curso.")
                ),
                ordenesLaboratorio = todasOrdenes.filter { it.numeroExpediente == "CM-2026-001" }
            ),
            ExpedienteDetalle(
                paciente = Paciente(
                    id = "p-mariana",
                    nombreCompleto = "Mariana Silva Rivas",
                    fechaNacimiento = "1988-03-24",
                    dui = "15.902.341-2",
                    sexo = "F",
                    telefono = "+503 7890-1234",
                    numeroExpediente = "MS-2026-002",
                    prevision = "Isapre Colmena"
                ),
                cama = "Cama VIP-301 (Planta 3)",
                signosVitales = SignosVitales(125, 82, 72, 16, 36.5, 62.0, 165.0, 98),
                alergiasCriticas = listOf("ALERGIA SEVERA: PENICILINA", "Alergia moderada a sulfas"),
                comorbilidades = listOf("HTA Grado I", "Asma Leve", "Losartán 50mg/d"),
                atencionesPrevias = listOf(
                    AtencionHistorica("Hoy", "09:40", "Cardiología Adultos", "I10 - Hipertensión controlada", "Dr. Alejandro Restrepo", "Ecocardiograma normal. Se mantiene pauta terapéutica."),
                    AtencionHistorica("12 Sep 2026", "11:15", "Consulta Externa", "J45 - Asma bronquial leve", "Dra. Sofía Valenzuela", "Espirometría estable con broncodilatador.")
                ),
                ordenesLaboratorio = todasOrdenes.filter { it.numeroExpediente == "MS-2026-002" }
            ),
            ExpedienteDetalle(
                paciente = Paciente(
                    id = "p-luis",
                    nombreCompleto = "Luis Barillas",
                    fechaNacimiento = "1975-09-12",
                    dui = "03456789-1",
                    sexo = "M",
                    telefono = "+503 7654-9870",
                    numeroExpediente = "LB-2026-003",
                    prevision = "Fonasa Tramo D"
                ),
                cama = "Cama HAB-202-A (Planta 2)",
                signosVitales = SignosVitales(130, 85, 74, 17, 36.8, 78.5, 176.0, 97),
                alergiasCriticas = listOf("Alergia a Contrastes Yodados"),
                comorbilidades = listOf("Diabetes Mellitus Tipo 2", "Dislipidemia"),
                atencionesPrevias = listOf(
                    AtencionHistorica("Ayer", "16:00", "Medicina Interna", "E11 - Diabetes Mellitus Tipo 2", "Dr. Alejandro Restrepo", "Se indican controles metabólicos y gasometría.")
                ),
                ordenesLaboratorio = todasOrdenes.filter { it.numeroExpediente == "LB-2026-003" }
            )
        )
    }
}
