package com.cmo.hospital.features.doctor.inicio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.features.doctor.inicio.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SegmentoInicioMedico {
    MIS_CITAS,
    MIS_PACIENTES
}

data class InicioDoctorUiState(
    val segmentoActivo: SegmentoInicioMedico = SegmentoInicioMedico.MIS_CITAS,
    val filtroCitas: String = "Todas",
    val queryPacientes: String = "",
    val citasFiltradas: List<Cita> = emptyList(),
    val pacientesFiltrados: List<PacienteMedicoResumen> = emptyList(),
    val totalCitas: Int = 0,
    val totalPacientes: Int = 0,
    val kpiPendientes: Int = 0,
    val kpiEnBox: Int = 0,
    val kpiAtendidas: Int = 0,
    val consultaEnCurso: ConsultaActiva? = null
)

class InicioDoctorViewModel(
    observeCitasMedicoUseCase: ObserveCitasMedicoUseCase,
    observeConsultaEnCursoUseCase: ObserveConsultaEnCursoUseCase,
    observeExpedientesMedicoUseCase: ObserveExpedientesMedicoUseCase,
    private val sincronizarPanelMedicoUseCase: SincronizarPanelMedicoUseCase,
    private val iniciarORetomarConsultaMedicoUseCase: IniciarORetomarConsultaMedicoUseCase,
    private val abrirExpedientePacienteUseCase: AbrirExpedientePacienteUseCase,
    private val construirPacientesMedicoUseCase: ConstruirPacientesMedicoUseCase
) : ViewModel() {

    private val _segmento = MutableStateFlow(SegmentoInicioMedico.MIS_CITAS)
    private val _filtroCitas = MutableStateFlow("Todas")
    private val _queryPacientes = MutableStateFlow("")

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    val uiState: StateFlow<InicioDoctorUiState> = combine(
        observeCitasMedicoUseCase(),
        observeConsultaEnCursoUseCase(),
        observeExpedientesMedicoUseCase(),
        combine(_segmento, _filtroCitas, _queryPacientes) { seg, fil, q -> Triple(seg, fil, q) }
    ) { todasCitas, consultaActiva, expedientes, (seg, filtro, queryPac) ->
        // Ordenar priorizando EN_ATENCION -> EN_ESPERA -> AGENDADA -> ATENDIDA -> CANCELADA
        val ordenPrioridad = mapOf(
            "EN_ATENCION" to 0,
            "EN_ESPERA" to 1,
            "AGENDADA" to 2,
            "ATENDIDA" to 3,
            "CANCELADA" to 4
        )
        val citasOrdenadas = todasCitas.sortedWith(
            compareBy<Cita> { ordenPrioridad[it.estado] ?: 5 }
                .thenBy { it.horaInicio }
        )

        val citasVisibles = when (filtro) {
            "Pendientes" -> citasOrdenadas.filter { it.estado == "EN_ESPERA" || it.estado == "AGENDADA" }
            "En Atención" -> citasOrdenadas.filter { it.estado == "EN_ATENCION" }
            "Atendidas" -> citasOrdenadas.filter { it.estado == "ATENDIDA" }
            else -> citasOrdenadas
        }

        val todosPacientes = construirPacientesMedicoUseCase(citasOrdenadas, expedientes, "")
        val pacientesVisibles = construirPacientesMedicoUseCase(citasOrdenadas, expedientes, queryPac)

        val pendientes = todasCitas.count { it.estado == "EN_ESPERA" || it.estado == "AGENDADA" }
        val enBox = todasCitas.count { it.estado == "EN_ATENCION" }
        val atendidas = todasCitas.count { it.estado == "ATENDIDA" }

        val activaValida = consultaActiva.takeIf { !it.finalizada && enBox > 0 }

        InicioDoctorUiState(
            segmentoActivo = seg,
            filtroCitas = filtro,
            queryPacientes = queryPac,
            citasFiltradas = citasVisibles,
            pacientesFiltrados = pacientesVisibles,
            totalCitas = todasCitas.size,
            totalPacientes = todosPacientes.size,
            kpiPendientes = pendientes,
            kpiEnBox = enBox,
            kpiAtendidas = atendidas,
            consultaEnCurso = activaValida
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InicioDoctorUiState())

    init {
        refrescarDatos()
    }

    fun refrescarDatos() {
        viewModelScope.launch {
            sincronizarPanelMedicoUseCase()
        }
    }

    fun seleccionarSegmento(segmento: SegmentoInicioMedico) {
        _segmento.value = segmento
    }

    fun seleccionarFiltroCitas(filtro: String) {
        _filtroCitas.value = filtro
    }

    fun actualizarBusquedaPacientes(query: String) {
        _queryPacientes.value = query
    }

    fun iniciarORetomarConsulta(cita: Cita, onReadyToNavigate: () -> Unit) {
        viewModelScope.launch {
            val eraEnAtencion = cita.estado == "EN_ATENCION"
            iniciarORetomarConsultaMedicoUseCase(cita.id).fold(
                onSuccess = { consulta ->
                    if (!eraEnAtencion) {
                        _toastEvent.value = Triple(
                            "Consulta Médica Iniciada",
                            "${consulta.paciente.nombreCompleto} en ${consulta.box} • Cronómetro 30m activo.",
                            false
                        )
                    }
                    onReadyToNavigate()
                },
                onFailure = { err ->
                    _toastEvent.value = Triple(
                        "No se pudo abrir consulta",
                        err.message ?: "Error al iniciar la consulta.",
                        true
                    )
                }
            )
        }
    }

    fun prepararAperturaExpediente(numeroExpediente: String?) {
        abrirExpedientePacienteUseCase(numeroExpediente)
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
