package com.cmo.hospital.features.recepcionista.citas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CitasUiEvent(
    val title: String,
    val message: String,
    val isError: Boolean = false
)

class CitasViewModel(
    getCitasFlowUseCase: GetCitasFlowUseCase,
    getTurnoEnBoxFlowUseCase: GetTurnoEnBoxFlowUseCase,
    private val refreshCitasUseCase: RefreshCitasUseCase,
    private val agendarCitaCascadaUseCase: AgendarCitaCascadaUseCase,
    private val marcarLlegadaCitaUseCase: MarcarLlegadaCitaUseCase,
    private val cancelarCitaUseCase: CancelarCitaUseCase,
    private val registrarTriajeYPasarConsultaUseCase: RegistrarTriajeYPasarConsultaUseCase,
    private val controlarLlamadorSalaUseCase: ControlarLlamadorSalaUseCase
) : ViewModel() {

    val citas: StateFlow<List<Cita>> = getCitasFlowUseCase()
    val turnoEnBox: StateFlow<Cita?> = getTurnoEnBoxFlowUseCase()

    private val _uiEvent = MutableStateFlow<CitasUiEvent?>(null)
    val uiEvent: StateFlow<CitasUiEvent?> = _uiEvent.asStateFlow()

    init {
        viewModelScope.launch {
            refreshCitasUseCase()
        }
    }

    fun agendarCita(
        especialidad: String,
        medicoNombre: String,
        pacienteNombre: String,
        dui: String,
        fechaNacimiento: String?,
        fecha: String,
        horaInicio: String,
        motivo: String,
        box: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val res = agendarCitaCascadaUseCase(
                especialidad = especialidad,
                medicoNombre = medicoNombre,
                pacienteNombre = pacienteNombre,
                dui = dui,
                fechaNacimiento = fechaNacimiento,
                fecha = fecha,
                horaInicio = horaInicio,
                motivo = motivo,
                box = box
            )
            res.fold(
                onSuccess = { cita ->
                    _uiEvent.value = CitasUiEvent(
                        title = "Cita Agendada (30 min)",
                        message = "${cita.paciente?.nombreCompleto} con ${cita.medicoNombre} (${cita.horaInicio} - ${cita.horaFin})."
                    )
                    onSuccess()
                },
                onFailure = { err ->
                    _uiEvent.value = CitasUiEvent(
                        title = "No se pudo agendar",
                        message = err.message ?: "Verifique los campos obligatorios.",
                        isError = true
                    )
                }
            )
        }
    }

    fun registrarLlegada(citaId: String) {
        viewModelScope.launch {
            marcarLlegadaCitaUseCase(citaId)
            _uiEvent.value = CitasUiEvent(
                title = "Llegada Confirmada",
                message = "El paciente pasó a Sala de Espera para triaje opcional o pase a Box."
            )
        }
    }

    fun cancelarCita(citaId: String) {
        viewModelScope.launch {
            cancelarCitaUseCase(citaId)
            _uiEvent.value = CitasUiEvent(
                title = "Cita Cancelada",
                message = "El bloque de 30 minutos quedó liberado en la agenda médica."
            )
        }
    }

    fun guardarTriajeOPasarConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ) {
        viewModelScope.launch {
            val res = registrarTriajeYPasarConsultaUseCase(citaId, signosVitales, pasarAConsulta)
            res.fold(
                onSuccess = { msg ->
                    _uiEvent.value = CitasUiEvent(
                        title = if (pasarAConsulta) "Pase a Consulta Activa" else "Triaje Registrado",
                        message = msg
                    )
                },
                onFailure = { err ->
                    _uiEvent.value = CitasUiEvent(
                        title = "Error en Triaje",
                        message = err.message ?: "Error al procesar triaje.",
                        isError = true
                    )
                }
            )
        }
    }

    fun reLlamarEnSala() {
        val actual = controlarLlamadorSalaUseCase.reLlamar()
        if (actual != null) {
            _uiEvent.value = CitasUiEvent(
                title = "Llamador de Sala",
                message = "Re-emitiendo llamado sonoro y visual para ${actual.consultorio?.codigo ?: "Box 03"}."
            )
        }
    }

    fun llamarSiguienteEnSala() {
        val sig = controlarLlamadorSalaUseCase.llamarSiguiente()
        if (sig != null) {
            _uiEvent.value = CitasUiEvent(
                title = "Siguiente Turno en Box",
                message = "Paciente transferido a ${sig.consultorio?.codigo ?: "Box 03"} y cargado en Consulta Activa."
            )
        } else {
            _uiEvent.value = CitasUiEvent(
                title = "Sin Pacientes en Espera",
                message = "No hay citas pendientes en sala de espera.",
                isError = true
            )
        }
    }

    fun consumeEvent() {
        _uiEvent.value = null
    }
}
