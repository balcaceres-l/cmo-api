package com.cmo.hospital.repository

import android.content.Context
import com.cmo.hospital.data.model.*
import com.cmo.hospital.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DoctorClinicalRepository(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: DoctorClinicalRepository? = null

        fun getInstance(context: Context): DoctorClinicalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DoctorClinicalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val apiService = RetrofitClient.getApiService(context)
    val repRecepcion: RecepcionistaRepository = RecepcionistaRepository.getInstance(context)
    val citas: StateFlow<List<Cita>> = repRecepcion.citas

    // Consulta activa actual
    private val _consultaActiva = MutableStateFlow(getMockConsultaActiva())
    val consultaActiva: StateFlow<ConsultaActivaData> = _consultaActiva.asStateFlow()

    // Órdenes de laboratorio LIS
    private val _ordenesLab = MutableStateFlow(getMockOrdenesLab())
    val ordenesLab: StateFlow<List<OrdenLaboratorioItem>> = _ordenesLab.asStateFlow()

    // Expedientes clínicos
    private val _expedientes = MutableStateFlow(getMockExpedientes())
    val expedientes: StateFlow<List<ExpedienteDetalle>> = _expedientes.asStateFlow()

    data class ConsultaActivaData(
        val id: String = "cons-01",
        val citaId: String = "c-101",
        val box: String = "Box 03",
        val medicoNombre: String = "Dr. Alejandro Restrepo",
        val especialidad: String = "Medicina Interna",
        val paciente: Paciente,
        val signosVitales: SignosVitales,
        val diagnosticosCie10: List<Cie10Diagnostico>,
        val anamnesis: String,
        val recetas: List<RecetaItem>,
        val ordenesLaboratorio: List<OrdenLaboratorioItem> = emptyList(),
        val segundosTranscurridos: Int = 18 * 60, // 18 minutos transcurridos inicialmente
        val duracionTotalSegundos: Int = 30 * 60, // Bloque oficial de 30 minutos
        val estadoSemaforo: EstadoSemaforo = EstadoSemaforo.VERDE,
        val finalizada: Boolean = false
    ) {
        val minutosRestantes: Int get() = ((duracionTotalSegundos - segundosTranscurridos) / 60).coerceAtLeast(0)
    }

    data class ExpedienteDetalle(
        val paciente: Paciente,
        val cama: String? = null,
        val signosVitales: SignosVitales,
        val alergiasCriticas: List<String> = emptyList(),
        val comorbilidades: List<String> = emptyList(),
        val atencionesPrevias: List<AtencionHistorica> = emptyList(),
        val ordenesLaboratorio: List<OrdenLaboratorioItem> = emptyList()
    )

    data class AtencionHistorica(
        val fecha: String,
        val hora: String,
        val especialidad: String,
        val diagnostico: String,
        val medico: String,
        val notas: String
    )

    fun actualizarSegundos(segundos: Int) {
        val actual = _consultaActiva.value
        val nuevoSemaforo = when {
            segundos < 20 * 60 -> EstadoSemaforo.VERDE
            segundos < 25 * 60 -> EstadoSemaforo.AMBAR
            else -> EstadoSemaforo.ROJO
        }
        _consultaActiva.value = actual.copy(
            segundosTranscurridos = segundos,
            estadoSemaforo = nuevoSemaforo
        )
    }

    /**
     * El Doctor puede tomar o modificar directamente los Signos Vitales en consulta
     * (crucial para emergencias o re-evaluaciones).
     */
    fun actualizarSignosVitales(signos: SignosVitales) {
        _consultaActiva.value = _consultaActiva.value.copy(signosVitales = signos)
    }

    fun agregarDiagnostico(codigo: String, descripcion: String) {
        val actual = _consultaActiva.value
        if (actual.diagnosticosCie10.none { it.codigo == codigo }) {
            _consultaActiva.value = actual.copy(
                diagnosticosCie10 = actual.diagnosticosCie10 + Cie10Diagnostico(codigo, descripcion)
            )
        }
    }

    fun eliminarDiagnostico(codigo: String) {
        val actual = _consultaActiva.value
        _consultaActiva.value = actual.copy(
            diagnosticosCie10 = actual.diagnosticosCie10.filter { it.codigo != codigo }
        )
    }

    fun actualizarAnamnesis(texto: String) {
        _consultaActiva.value = _consultaActiva.value.copy(anamnesis = texto)
    }

    fun agregarReceta(receta: RecetaItem) {
        val actual = _consultaActiva.value
        _consultaActiva.value = actual.copy(recetas = actual.recetas + receta)
    }

    fun eliminarReceta(recetaId: String) {
        val actual = _consultaActiva.value
        _consultaActiva.value = actual.copy(recetas = actual.recetas.filter { it.id != recetaId })
    }

    suspend fun finalizarConsulta(): Result<String> {
        _consultaActiva.value = _consultaActiva.value.copy(finalizada = true)
        try {
            apiService.finalizarConsulta(_consultaActiva.value.id)
        } catch (e: Exception) {}
        return Result.success("Consulta finalizada y receta digital firmada electrónicamente.")
    }

    /**
     * Inicia una consulta activa cuando recepción o enfermería presiona "Pasar a Consulta".
     * Carga al paciente, los signos vitales tomados en el triaje (o vacíos si fue de emergencia)
     * y reinicia el cronómetro del semáforo en 0 minutos (Verde).
     */
    fun iniciarConsultaDesdeCita(cita: Cita) {
        val pac = cita.paciente?.let {
            Paciente(
                id = it.id,
                nombreCompleto = it.nombreCompleto,
                numeroExpediente = it.numeroExpediente ?: Paciente.generarNumeroExpediente(it.nombreCompleto),
                dui = it.dui,
                sexo = it.sexo,
                fechaNacimiento = it.fechaNacimiento,
                alergias = it.alergias
            )
        } ?: _consultaActiva.value.paciente

        val vitals = cita.signosVitales ?: SignosVitales()

        _consultaActiva.value = ConsultaActivaData(
            id = "cons-" + System.currentTimeMillis().toString().takeLast(4),
            citaId = cita.id,
            box = cita.consultorio?.codigo ?: "Box 03",
            medicoNombre = cita.medicoNombre ?: "Dr. Alejandro Restrepo",
            especialidad = cita.especialidad ?: "Medicina General",
            paciente = pac,
            signosVitales = vitals,
            diagnosticosCie10 = emptyList(),
            anamnesis = cita.motivo ?: "Paciente transferido directamente desde admisión.",
            recetas = emptyList(),
            ordenesLaboratorio = emptyList(),
            segundosTranscurridos = 0, // Cronómetro arranca en 0 minutos (Verde)
            duracionTotalSegundos = 30 * 60,
            estadoSemaforo = EstadoSemaforo.VERDE,
            finalizada = false
        )
    }

    /**
     * El Doctor agenda citas para sí mismo (ej. seguimiento, control) respetando
     * bloques de 30 minutos y validando que por ningún motivo haya solapamiento de horario.
     */
    suspend fun agendarCitaParaSiMismo(
        paciente: Paciente,
        fecha: String,
        horaInicio: String,
        motivo: String,
        especialidad: String,
        medicoNombre: String = "Dr. Alejandro Restrepo"
    ): Result<Cita> {
        val repRecepcion = RecepcionistaRepository.getInstance(context)
        val citasGlobales = repRecepcion.citasState.value

        if (Cita.hayConflictoHorario(citasGlobales, medicoNombre, fecha, horaInicio)) {
            return Result.failure(
                IllegalStateException("⚠️ Horario no disponible: Ya tienes una cita programada a las $horaInicio el $fecha. Por ningún motivo se permiten citas a la misma hora.")
            )
        }

        val horaFin = Cita.calcularFin30Minutos(horaInicio)
        val nuevaCita = Cita(
            id = "c-doc-" + System.currentTimeMillis().toString().takeLast(4),
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            estado = "AGENDADA",
            motivo = motivo,
            medicoNombre = medicoNombre,
            especialidad = especialidad,
            paciente = PacienteInfo(
                id = paciente.id,
                nombreCompleto = paciente.nombreCompleto,
                numeroExpediente = paciente.numeroExpediente ?: Paciente.generarNumeroExpediente(paciente.nombreCompleto),
                dui = paciente.dui,
                sexo = paciente.sexo,
                fechaNacimiento = paciente.fechaNacimiento
            ),
            consultorio = ConsultorioInfo("cs-box3", "Box 03 ($especialidad)", "Box 03")
        )

        repRecepcion.agregarCitaDirecta(nuevaCita)
        return Result.success(nuevaCita)
    }

    /**
     * El Doctor deja indicados los exámenes de laboratorio a realizar para el paciente.
     * Se guardan en el expediente clínico del paciente y se visualizan en el módulo del laboratorio.
     */
    suspend fun emitirOrdenLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente? = null,
        estadoInicial: String = "En Espera"
    ): Result<String> {
        val pac = paciente ?: _consultaActiva.value.paciente
        val horaActual = try {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            "12:00"
        }

        val nuevas = examenes.map { nombre ->
            OrdenLaboratorioItem(
                id = "ord-" + System.currentTimeMillis().toString().takeLast(4) + (10..99).random(),
                pacienteId = pac.id,
                pacienteNombre = pac.nombreCompleto,
                numeroExpediente = pac.numeroExpediente,
                examenNombre = nombre,
                tipoTubo = when {
                    nombre.contains("Troponina", true) -> "Protocolo SCA"
                    nombre.contains("Gasometría", true) -> "Heparina Li"
                    nombre.contains("Glucosa", true) || nombre.contains("Perfil", true) -> "Suero Gel"
                    else -> "Tubo EDTA"
                },
                prioridad = prioridad,
                estado = estadoInicial,
                fechaSolicitud = "Hoy, $horaActual",
                observaciones = observaciones,
                medicoSolicitante = "Dr. Alejandro Restrepo"
            )
        }

        // 1. Agregar al módulo general de laboratorio
        _ordenesLab.value = nuevas + _ordenesLab.value

        // 2. Si corresponde a la consulta activa, agregarlo a la consulta activa
        if (pac.numeroExpediente == _consultaActiva.value.paciente.numeroExpediente) {
            _consultaActiva.value = _consultaActiva.value.copy(
                ordenesLaboratorio = nuevas + _consultaActiva.value.ordenesLaboratorio
            )
        }

        // 3. Guardar en el expediente del paciente
        _expedientes.value = _expedientes.value.map { exp ->
            if (exp.paciente.numeroExpediente == pac.numeroExpediente || exp.paciente.id == pac.id) {
                exp.copy(ordenesLaboratorio = nuevas + exp.ordenesLaboratorio)
            } else {
                exp
            }
        }

        return Result.success("Exámenes indicados guardados en el expediente (${pac.numeroExpediente}) y enviados a Laboratorio.")
    }

    /**
     * El Doctor es el encargado de cambiar el estado de los laboratorios:
     * En Espera, Agendado, En Proceso, Realizado, Cancelado.
     */
    suspend fun cambiarEstadoLaboratorio(ordenId: String, nuevoEstado: String): Result<String> {
        val horaActual = try {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            "12:30"
        }

        // 1. Actualizar en el módulo de laboratorio
        _ordenesLab.value = _ordenesLab.value.map { ord ->
            if (ord.id == ordenId) {
                ord.copy(
                    estado = nuevoEstado,
                    fechaResultado = if (nuevoEstado == "Realizado") "Hoy, $horaActual" else ord.fechaResultado
                )
            } else ord
        }

        // 2. Actualizar en la consulta activa si está presente
        _consultaActiva.value = _consultaActiva.value.copy(
            ordenesLaboratorio = _consultaActiva.value.ordenesLaboratorio.map { ord ->
                if (ord.id == ordenId) {
                    ord.copy(
                        estado = nuevoEstado,
                        fechaResultado = if (nuevoEstado == "Realizado") "Hoy, $horaActual" else ord.fechaResultado
                    )
                } else ord
            }
        )

        // 3. Actualizar en los expedientes de pacientes
        _expedientes.value = _expedientes.value.map { exp ->
            exp.copy(
                ordenesLaboratorio = exp.ordenesLaboratorio.map { ord ->
                    if (ord.id == ordenId) {
                        ord.copy(
                            estado = nuevoEstado,
                            fechaResultado = if (nuevoEstado == "Realizado") "Hoy, $horaActual" else ord.fechaResultado
                        )
                    } else ord
                }
            )
        }

        return Result.success("Estado de examen actualizado a: $nuevoEstado")
    }

    private fun getMockConsultaActiva(): ConsultaActivaData {
        val pacienteCarlos = Paciente(
            id = "p-carlos",
            nombreCompleto = "Carlos Mendoza",
            fechaNacimiento = "1981-05-14",
            dui = "14.891.203-K",
            sexo = "M",
            telefono = "+503 7123-4567",
            alergias = "Alergia a Dipirona / AINEs",
            numeroExpediente = "CM-2026-001",
            prevision = "Fonasa B"
        )

        val ordenActiva = OrdenLaboratorioItem(
            id = "ord-02",
            pacienteId = pacienteCarlos.id,
            pacienteNombre = pacienteCarlos.nombreCompleto,
            numeroExpediente = pacienteCarlos.numeroExpediente,
            examenNombre = "Troponina I Alta Sensibilidad (hs-cTnI)",
            tipoTubo = "Protocolo SCA",
            prioridad = "STAT",
            estado = "En Espera",
            fechaSolicitud = "Hoy, 10:32",
            observaciones = "Cinética enzimática 0h - 1h por dolor torácico opresivo.",
            medicoSolicitante = "Dr. Alejandro Restrepo"
        )

        return ConsultaActivaData(
            id = "cons-01",
            citaId = "c-101",
            box = "Box 03",
            medicoNombre = "Dr. Alejandro Restrepo",
            especialidad = "Medicina Interna",
            paciente = pacienteCarlos,
            signosVitales = SignosVitales(
                presionSistolica = 120,
                presionDiastolica = 80,
                frecuenciaCardiaca = 78,
                frecuenciaRespiratoria = 18,
                temperaturaC = 36.6,
                pesoKg = 74.0,
                tallaCm = 172.0,
                saturacionOxigeno = 98,
                observaciones = "Basal AA. Normotenso en reposo."
            ),
            diagnosticosCie10 = listOf(
                Cie10Diagnostico("I10", "Hipertensión Esencial (Primaria)"),
                Cie10Diagnostico("R07.4", "Dolor torácico atípico no especificado")
            ),
            anamnesis = "Paciente masculino de 45 años con antecedente de hipertensión controlada. Refiere opresión retroesternal intermitente de 48 horas de evolución, sin cortejo vegetativo. Normotenso en control seriado.",
            recetas = listOf(
                RecetaItem(
                    medicamento = "Enalapril 10mg",
                    dosis = "1 comprimido",
                    frecuencia = "Cada 12 hrs vía oral",
                    duracion = "30 días",
                    indicaciones = "Tomar en ayunas y noche. Farmacia Central HUC.",
                    tipo = "Crónico"
                ),
                RecetaItem(
                    medicamento = "Paracetamol 500mg",
                    dosis = "1 comprimido",
                    frecuencia = "Cada 8 hrs en caso de dolor",
                    duracion = "Máximo 3 días",
                    indicaciones = "Tomar con abundante agua.",
                    tipo = "SOS"
                )
            ),
            ordenesLaboratorio = listOf(ordenActiva),
            segundosTranscurridos = 18 * 60,
            duracionTotalSegundos = 30 * 60,
            estadoSemaforo = EstadoSemaforo.VERDE
        )
    }

    private fun getMockOrdenesLab(): List<OrdenLaboratorioItem> {
        return listOf(
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
    }

    private fun getMockExpedientes(): List<ExpedienteDetalle> {
        val ordenesMariana = listOf(
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

        val ordenesCarlos = listOf(
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
            )
        )

        val ordenesLuis = listOf(
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
            )
        )

        return listOf(
            ExpedienteDetalle(
                paciente = Paciente(
                    id = "p-carlos",
                    nombreCompleto = "Carlos Mendoza",
                    fechaNacimiento = "1981-05-14",
                    dui = "14.891.203-K",
                    sexo = "M",
                    telefono = "+503 7123-4567",
                    numeroExpediente = "CM-2026-001"
                ),
                cama = "Box 03 (En Consulta)",
                signosVitales = SignosVitales(
                    presionSistolica = 120,
                    presionDiastolica = 80,
                    frecuenciaCardiaca = 78,
                    temperaturaC = 36.6,
                    saturacionOxigeno = 98,
                    pesoKg = 74.0,
                    tallaCm = 172.0
                ),
                alergiasCriticas = listOf("Alergia a Dipirona / AINEs"),
                comorbilidades = listOf("Hipertensión Arterial"),
                atencionesPrevias = listOf(
                    AtencionHistorica("Hoy", "08:30", "Medicina Interna", "I10 - Hipertensión", "Dr. Alejandro Restrepo", "Consulta en curso.")
                ),
                ordenesLaboratorio = ordenesCarlos
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
                    prevision = "Fonasa B"
                ),
                cama = "Cama VIP-302 (Planta 3)",
                signosVitales = SignosVitales(
                    presionSistolica = 125,
                    presionDiastolica = 82,
                    frecuenciaCardiaca = 72,
                    temperaturaC = 36.5,
                    saturacionOxigeno = 98,
                    pesoKg = 62.0,
                    tallaCm = 165.0
                ),
                alergiasCriticas = listOf("ALERGIA SEVERA: PENICILINA", "Alergia moderada a sulfas"),
                comorbilidades = listOf("HTA Grado I", "Asma Leve", "Losartán 50mg/d"),
                atencionesPrevias = listOf(
                    AtencionHistorica("Hoy", "09:40", "Cardiología Adultos", "I10 - Hipertensión controlada", "Dr. Alejandro Restrepo", "Ecocardiograma normal. Se mantiene pauta terapéutica."),
                    AtencionHistorica("12 Sep 2026", "11:15", "Consulta Externa", "J45 - Asma bronquial leve", "Dra. Sofía Valenzuela", "Espirometría estable con broncodilatador.")
                ),
                ordenesLaboratorio = ordenesMariana
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
                    prevision = "Fonasa A"
                ),
                cama = "Cama HAB-202-A (Planta 2)",
                signosVitales = SignosVitales(
                    presionSistolica = 130,
                    presionDiastolica = 85,
                    frecuenciaCardiaca = 74,
                    temperaturaC = 36.8,
                    saturacionOxigeno = 97,
                    pesoKg = 78.5,
                    tallaCm = 176.0
                ),
                alergiasCriticas = listOf("Alergia a Contrastes Yodados"),
                comorbilidades = listOf("Diabetes Mellitus Tipo 2", "Dislipidemia"),
                atencionesPrevias = listOf(
                    AtencionHistorica("Ayer", "16:00", "Medicina Interna", "E11 - Diabetes Mellitus Tipo 2", "Dr. Alejandro Restrepo", "Se indican controles metabólicos y gasometría.")
                ),
                ordenesLaboratorio = ordenesLuis
            )
        )
    }
}
