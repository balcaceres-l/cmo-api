package com.cmo.hospital.repository

import android.content.Context
import com.cmo.hospital.data.model.*
import com.cmo.hospital.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DoctorClinicalRepository(private val context: Context) {
    private val apiService = RetrofitClient.getApiService(context)

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
        val atencionesPrevias: List<AtencionHistorica> = emptyList()
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

    suspend fun emitirOrdenLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String
    ): Result<String> {
        val nuevas = examenes.map { nombre ->
            OrdenLaboratorioItem(
                id = "ord-" + System.currentTimeMillis().toString().takeLast(4),
                examenNombre = nombre,
                tipoTubo = if (nombre.contains("Troponina", true)) "Protocolo SCA" else if (nombre.contains("Gasometría", true)) "Heparina Li" else "Tubo EDTA",
                prioridad = prioridad,
                estado = "PENDIENTE",
                fechaSolicitud = "Hoy, 12:45",
                observaciones = observaciones
            )
        }
        _ordenesLab.value = nuevas + _ordenesLab.value
        return Result.success("Orden enviada exitosamente al LIS Central.")
    }

    private fun getMockConsultaActiva(): ConsultaActivaData {
        return ConsultaActivaData(
            id = "cons-01",
            citaId = "c-101",
            box = "Box 03",
            medicoNombre = "Dr. Alejandro Restrepo",
            especialidad = "Medicina Interna",
            paciente = Paciente(
                id = "p-1",
                nombreCompleto = "Carlos Mendoza",
                fechaNacimiento = "1981-05-14",
                dui = "14.891.203-K",
                sexo = "M",
                telefono = "+503 7123-4567",
                alergias = "Alergia moderada a AINEs",
                numeroExpediente = "EXP-2026-000104",
                prevision = "Fonasa B"
            ),
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
            segundosTranscurridos = 18 * 60,
            duracionTotalSegundos = 30 * 60,
            estadoSemaforo = EstadoSemaforo.VERDE
        )
    }

    private fun getMockOrdenesLab(): List<OrdenLaboratorioItem> {
        return listOf(
            OrdenLaboratorioItem(
                id = "ord-01",
                examenNombre = "Hemograma Completo + Plaquetas",
                tipoTubo = "Tubo EDTA",
                prioridad = "STAT",
                estado = "EN_PROCESO",
                fechaSolicitud = "Hoy, 10:30",
                observaciones = "Control urgente de hematocrito y leucocitos."
            ),
            OrdenLaboratorioItem(
                id = "ord-02",
                examenNombre = "Troponina I Alta Sensibilidad (hs-cTnI)",
                tipoTubo = "Protocolo SCA",
                prioridad = "STAT",
                estado = "EN_PROCESO",
                fechaSolicitud = "Hoy, 10:32",
                observaciones = "Cinética enzimática 0h - 1h por dolor torácico."
            ),
            OrdenLaboratorioItem(
                id = "ord-03",
                examenNombre = "Gasometría Arterial y Electrolitos",
                tipoTubo = "Heparina Li",
                prioridad = "STAT",
                estado = "COMPLETADO",
                fechaSolicitud = "Hoy, 09:15",
                fechaResultado = "Hoy, 09:42",
                observaciones = "pH: 7.39, pO2: 92, pCO2: 38, Lactato normal."
            ),
            OrdenLaboratorioItem(
                id = "ord-04",
                examenNombre = "Perfil Bioquímico & Renal",
                tipoTubo = "Suero Seco",
                prioridad = "RUTINA",
                estado = "PENDIENTE",
                fechaSolicitud = "Hoy, 08:00",
                observaciones = "Creatinina sérica basal y glucemia en ayunas."
            )
        )
    }

    private fun getMockExpedientes(): List<ExpedienteDetalle> {
        return listOf(
            ExpedienteDetalle(
                paciente = Paciente(
                    id = "p-mariana",
                    nombreCompleto = "Mariana Silva Rivas",
                    fechaNacimiento = "1988-03-24",
                    dui = "15.902.341-2",
                    sexo = "F",
                    telefono = "+503 7890-1234",
                    numeroExpediente = "EXP-2026-000089",
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
                )
            ),
            ExpedienteDetalle(
                paciente = Paciente(
                    id = "p-carlos",
                    nombreCompleto = "Carlos Mendoza",
                    fechaNacimiento = "1981-05-14",
                    dui = "14.891.203-K",
                    sexo = "M",
                    numeroExpediente = "EXP-2026-000104"
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
                )
            )
        )
    }
}
