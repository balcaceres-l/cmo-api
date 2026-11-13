package com.cmo.hospital.core.data.datasource

import com.cmo.hospital.core.data.mapper.aOrdenLaboratorio
import com.cmo.hospital.core.data.mapper.agruparHabitaciones
import com.cmo.hospital.core.data.mapper.parsearHabitacionesJson
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
    // Sin camas de ejemplo: el mapa se carga desde GET /habitaciones.
    private val _camasState = MutableStateFlow<List<Cama>>(emptyList())
    val camasState: StateFlow<List<Cama>> = _camasState.asStateFlow()

    private val _citasState = MutableStateFlow<List<Cita>>(emptyList())
    val citasState: StateFlow<List<Cita>> = _citasState.asStateFlow()

    private val _turnoEnBox = MutableStateFlow(
        _citasState.value.firstOrNull { it.estado == "EN_ATENCION" } ?: _citasState.value.firstOrNull()
    )
    val turnoEnBox: StateFlow<Cita?> = _turnoEnBox.asStateFlow()

    // Sin consulta de ejemplo: la consulta real se abre o retoma desde una cita de la API.
    private val _consultaActiva = MutableStateFlow(ConsultaActiva.vacia())
    val consultaActiva: StateFlow<ConsultaActiva> = _consultaActiva.asStateFlow()

    private val _ordenesLab = MutableStateFlow<List<OrdenLaboratorioItem>>(emptyList())
    val ordenesLab: StateFlow<List<OrdenLaboratorioItem>> = _ordenesLab.asStateFlow()

    private val _examenesCatalogo = MutableStateFlow<List<ExamenDto>>(emptyList())
    val examenesCatalogo: StateFlow<List<ExamenDto>> = _examenesCatalogo.asStateFlow()

    private val _expedientes = MutableStateFlow<List<ExpedienteDetalle>>(emptyList())
    val expedientes: StateFlow<List<ExpedienteDetalle>> = _expedientes.asStateFlow()

    private val _expedienteSolicitado = MutableStateFlow<String?>(null)
    val expedienteSolicitado: StateFlow<String?> = _expedienteSolicitado.asStateFlow()

    init {
        _citasState.value.forEach { registrarPacienteDeCitaEnExpedientes(it) }
    }

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
                cama = ubicacion ?: "Consulta Externa CMO",
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
                            medico = "Cuerpo Médico CMO",
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
                // La lista se reemplaza: solo quedan los pacientes que devuelve la API.
                val previos = _expedientes.value.associateBy { it.paciente.id }
                _expedientes.value = remotos.map { pac ->
                    previos[pac.id]?.copy(paciente = pac) ?: ExpedienteDetalle(
                        paciente = pac,
                        signosVitales = SignosVitales(),
                        alergiasCriticas = listOfNotNull(pac.alergias?.trim()?.ifBlank { null })
                    )
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
                        val hi = (raw.horaInicio as String?)?.take(5).orEmpty()
                        val hf = raw.horaFin?.take(5)
                            ?: hi.takeIf { it.isNotEmpty() }?.let { Cita.calcularFin30Minutos(it) }
                        val estadoRaw = (raw.estado as String?)?.uppercase()?.trim().orEmpty()
                        // SOLICITADA se conserva: es la solicitud del paciente que recepción debe confirmar.
                        val estadoNorm = estadoRaw.ifBlank { EstadoCita.AGENDADA }
                        val citaNorm = raw.copy(
                            id = safeId,
                            fecha = (raw.fecha as String?)?.take(10).orEmpty(),
                            horaInicio = hi,
                            horaFin = hf,
                            estado = estadoNorm,
                            idMedico = raw.idMedico ?: raw.medico?.id,
                            medicoNombre = raw.medico?.nombreCompleto ?: "Médico por asignar",
                            especialidad = raw.medico?.cargo ?: "Consulta externa",
                            consultorio = raw.consultorio
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
        val citaAgendada = if (cita.estado.isBlank() || cita.estado == EstadoCita.SOLICITADA) {
            cita.copy(estado = EstadoCita.AGENDADA)
        } else {
            cita
        }
        registrarPacienteDeCitaEnExpedientes(citaAgendada)
        _citasState.value = listOf(citaAgendada) + _citasState.value
    }

    suspend fun agendarCitaRemotaOLocal(nuevaCita: Cita) {
        val citaAgendada = nuevaCita.copy(
            estado = if (nuevaCita.estado.isBlank() || nuevaCita.estado == EstadoCita.SOLICITADA) {
                EstadoCita.AGENDADA
            } else {
                nuevaCita.estado
            }
        )
        try {
            apiService?.agendarCita(
                mapOf(
                    "fecha" to citaAgendada.fecha,
                    "hora_inicio" to citaAgendada.horaInicio,
                    "hora_fin" to citaAgendada.horaFin,
                    "motivo" to citaAgendada.motivo,
                    "estado" to EstadoCita.AGENDADA
                )
            )
        } catch (_: Exception) {
        }
        prependCita(citaAgendada)
    }

    // Agenda de recepción: todo pasa por la API. Si falla, la lista no cambia y
    // el error llega a la pantalla; tras cada cambio se recarga la lista para
    // traer paciente, médico y consultorio ya resueltos por el servidor.

    private fun sinApi(): Result<Nothing> =
        Result.failure(IllegalStateException("La API no está configurada en esta compilación."))

    private suspend fun alCambiarCita(resultado: Result<Cita>): Result<Cita> {
        val cita = resultado.getOrElse { return Result.failure(it) }
        syncCitasFromRemote()
        return Result.success(_citasState.value.firstOrNull { it.id == cita.id } ?: cita)
    }

    suspend fun getMedicosAgenda(): Result<List<User>> {
        val api = apiService ?: return sinApi()
        return llamarApi { api.getMedicos() }.map { it.data }
    }

    suspend fun getConsultorios(): Result<List<ConsultorioInfo>> {
        val api = apiService ?: return sinApi()
        return llamarApi { api.getConsultorios() }
    }

    suspend fun getDisponibilidad(idMedico: String, fecha: String): Result<List<BloqueAgenda>> {
        val api = apiService ?: return sinApi()
        return llamarApi { api.getDisponibilidad(idMedico, fecha) }
    }

    suspend fun buscarPacientes(texto: String): Result<List<Paciente>> {
        val api = apiService ?: return sinApi()
        val limpio = texto.trim()
        val perPage = if (limpio.isEmpty()) 50 else 8
        return llamarApi { api.getPacientes(query = limpio.ifBlank { null }, perPage = perPage) }.map { it.data }
    }

    suspend fun agendarCita(
        idPaciente: String,
        programacion: ProgramacionCita,
        motivo: String
    ): Result<Cita> {
        val api = apiService
        if (api == null) {
            val pacExp = _expedientes.value.firstOrNull { it.paciente.id == idPaciente }?.paciente
            val nuevaLocal = Cita(
                id = "c-loc-${System.currentTimeMillis()}",
                fecha = programacion.fecha,
                horaInicio = programacion.horaInicio,
                horaFin = Cita.calcularFin30Minutos(programacion.horaInicio),
                estado = EstadoCita.AGENDADA,
                motivo = motivo,
                idMedico = programacion.idMedico,
                paciente = pacExp?.let {
                    PacienteInfo(
                        id = it.id,
                        nombreCompleto = it.nombreCompleto,
                        numeroExpediente = it.numeroExpediente,
                        dui = it.dui,
                        sexo = it.sexo,
                        fechaNacimiento = it.fechaNacimiento,
                        telefono = it.telefono
                    )
                }
            )
            prependCita(nuevaLocal)
            return Result.success(nuevaLocal)
        }
        val cuerpo = programacion.aMapa() + mapOf(
            "id_paciente" to idPaciente,
            "motivo" to motivo
        )
        return alCambiarCita(llamarApi { api.agendarCita(cuerpo) })
    }

    suspend fun reprogramarCita(citaId: String, programacion: ProgramacionCita): Result<Cita> {
        val api = apiService ?: return sinApi()
        return alCambiarCita(llamarApi { api.reprogramarCita(citaId, programacion.aMapa()) })
    }

    suspend fun confirmarCita(citaId: String, programacion: ProgramacionCita): Result<Cita> {
        val api = apiService ?: return sinApi()
        return alCambiarCita(llamarApi { api.confirmarCita(citaId, programacion.aMapa()) })
    }

    suspend fun marcarLlegada(citaId: String): Result<Cita> {
        val api = apiService ?: return sinApi()
        return alCambiarCita(llamarApi { api.registrarLlegada(citaId) })
    }

    suspend fun cancelarCita(citaId: String, motivo: String): Result<Cita> {
        val api = apiService ?: return sinApi()
        return alCambiarCita(
            llamarApi { api.cancelarCita(citaId, mapOf("motivo_cancelacion" to motivo)) }
        )
    }

    fun actualizarTriajeOPasarAConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean,
        hoyIso: String = LocalDate.now().toString()
    ): Cita? {
        val objetivo = _citasState.value.firstOrNull { it.id == citaId } ?: return null
        if (!objetivo.esDelDia(hoyIso)) {
            return null
        }
        var citaTransferida: Cita? = null
        _citasState.value = _citasState.value.map { cita ->
            if (cita.id == citaId) {
                val nuevosSignos = if (signosVitales?.hasAnyData == true) signosVitales else cita.signosVitales
                val nuevoEstado = when {
                    pasarAConsulta -> EstadoCita.EN_ATENCION
                    cita.estado == EstadoCita.AGENDADA && nuevosSignos?.hasAnyData == true -> EstadoCita.EN_ESPERA
                    else -> cita.estado
                }
                val actualizada = cita.copy(
                    signosVitales = nuevosSignos,
                    estado = nuevoEstado
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
    // Camas e ingresos hospitalarios (17 camas: 5 VIP en planta 3, 12 semiprivadas en planta 2)
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

    /** Camas agrupadas por habitación para la pestaña Habitaciones (VIP individual o semiprivada A/B). */
    fun construirHabitacionesDetalladas(): List<HabitacionDetalleUi> =
        agruparHabitaciones(_camasState.value)

    /** Mapa de camas real: GET /habitaciones devuelve las habitaciones agrupadas por planta. */
    suspend fun syncCamasFromRemote(): Result<List<Cama>> {
        val api = apiService ?: return sinApi()
        return llamarApi { api.getHabitaciones() }.map { elemento ->
            val camas = parsearHabitacionesJson(elemento)
                .flatMap { it.camas }
                .sortedBy { it.codigo }
            // Sin camas de ejemplo: si la API falla, la pantalla muestra el error. Unas camas
            // inventadas (ids "seed-…") harían que la API rechace cualquier ingreso sobre ellas.
            _camasState.value = camas
            camas
        }
    }

    private suspend fun alCambiarCamas(resultado: Result<*>): Result<Unit> {
        resultado.exceptionOrNull()?.let { return Result.failure(it) }
        syncCamasFromRemote()
        return Result.success(Unit)
    }

    suspend fun ingresarPaciente(
        idPaciente: String,
        cama: Cama,
        idMedico: String,
        motivo: String
    ): Result<Unit> {
        val api = apiService ?: return sinApi()
        val cuerpo = mapOf(
            "id_paciente" to idPaciente,
            "tipo" to if (cama.esVip) "VIP" else "SEMIPRIVADA",
            "id_cama" to cama.id,
            "id_medico" to idMedico,
            "motivo" to motivo
        )
        return alCambiarCamas(llamarApi { api.ingresarPaciente(cuerpo) })
    }

    suspend fun darAlta(idIngreso: String): Result<Unit> {
        val api = apiService ?: return sinApi()
        return alCambiarCamas(llamarApi { api.altaPaciente(idIngreso) })
    }

    suspend fun trasladarPaciente(idIngreso: String, idCamaDestino: String): Result<Unit> {
        val api = apiService ?: return sinApi()
        return alCambiarCamas(llamarApi { api.trasladarPaciente(idIngreso, mapOf("id_cama" to idCamaDestino)) })
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

    /**
     * PUT /citas/{id}/signos-vitales. Al guardarse, la cita (y la consulta abierta
     * de esa cita, si la hay) muestran los signos que devolvió la API.
     */
    suspend fun guardarSignosVitales(citaId: String, signos: SignosVitales): Result<SignosVitales> {
        val api = apiService ?: return sinApi()
        if (!signos.hasAnyData) {
            return Result.failure(IllegalArgumentException("Ingrese al menos un signo vital."))
        }
        return llamarApi { api.guardarSignosVitales(citaId, signos) }.onSuccess { guardados ->
            _citasState.value = _citasState.value.map { if (it.id == citaId) it.copy(signosVitales = guardados) else it }
            if (_consultaActiva.value.citaId == citaId) actualizarSignosConsulta(guardados)
        }
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

}
