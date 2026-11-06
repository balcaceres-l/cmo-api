package com.cmo.hospital.features.recepcionista.citas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.BloqueAgenda
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultorioInfo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CitasUiEvent(
    val title: String,
    val message: String,
    val isError: Boolean = false
)

/** Bloques libres de un médico en una fecha, tal como los devuelve la API. */
data class DisponibilidadUi(
    val idMedico: String,
    val fecha: String,
    val bloques: List<BloqueAgenda> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

data class AgendaUiState(
    val medicos: List<User> = emptyList(),
    val consultorios: List<ConsultorioInfo> = emptyList(),
    val catalogosCargando: Boolean = false,
    val catalogosError: String? = null,
    val disponibilidad: DisponibilidadUi? = null,
    val pacientesEncontrados: List<Paciente> = emptyList(),
    val buscandoPacientes: Boolean = false,
    val enviando: Boolean = false
)

/** Dónde se muestra un error de validación: el formulario de la pantalla o el diálogo. */
enum class OrigenProgramacion { FORMULARIO, DIALOGO }

data class ErrorProgramacion(
    val origen: OrigenProgramacion,
    val campo: String?,
    val mensaje: String
)

class CitasViewModel(
    getCitasFlowUseCase: GetCitasFlowUseCase,
    getTurnoEnBoxFlowUseCase: GetTurnoEnBoxFlowUseCase,
    private val refreshCitasUseCase: RefreshCitasUseCase,
    private val cargarCatalogosAgendaUseCase: CargarCatalogosAgendaUseCase,
    private val getDisponibilidadUseCase: GetDisponibilidadUseCase,
    private val buscarPacientesParaCitaUseCase: BuscarPacientesParaCitaUseCase,
    private val agendarCitaUseCase: AgendarCitaUseCase,
    private val confirmarCitaUseCase: ConfirmarCitaUseCase,
    private val reprogramarCitaUseCase: ReprogramarCitaUseCase,
    private val marcarLlegadaCitaUseCase: MarcarLlegadaCitaUseCase,
    private val cancelarCitaUseCase: CancelarCitaUseCase,
    private val registrarTriajeYPasarConsultaUseCase: RegistrarTriajeYPasarConsultaUseCase,
    private val controlarLlamadorSalaUseCase: ControlarLlamadorSalaUseCase
) : ViewModel() {

    val citas: StateFlow<List<Cita>> = getCitasFlowUseCase()
    val turnoEnBox: StateFlow<Cita?> = getTurnoEnBoxFlowUseCase()

    private val _agenda = MutableStateFlow(AgendaUiState())
    val agenda: StateFlow<AgendaUiState> = _agenda.asStateFlow()

    private val _errorProgramacion = MutableStateFlow<ErrorProgramacion?>(null)
    val errorProgramacion: StateFlow<ErrorProgramacion?> = _errorProgramacion.asStateFlow()

    private val _uiEvent = MutableStateFlow<CitasUiEvent?>(null)
    val uiEvent: StateFlow<CitasUiEvent?> = _uiEvent.asStateFlow()

    private var disponibilidadJob: Job? = null
    private var busquedaJob: Job? = null

    init {
        viewModelScope.launch { refreshCitasUseCase() }
        cargarCatalogos()
    }

    fun refrescar() {
        viewModelScope.launch { refreshCitasUseCase() }
    }

    fun cargarCatalogos() {
        viewModelScope.launch {
            _agenda.update { it.copy(catalogosCargando = true, catalogosError = null) }
            cargarCatalogosAgendaUseCase().fold(
                onSuccess = { (medicos, consultorios) ->
                    _agenda.update {
                        it.copy(medicos = medicos, consultorios = consultorios, catalogosCargando = false)
                    }
                },
                onFailure = { err ->
                    _agenda.update {
                        it.copy(catalogosCargando = false, catalogosError = err.message ?: "No se pudo cargar la agenda.")
                    }
                }
            )
        }
    }

    /** Pide los bloques libres; una petición nueva reemplaza a la anterior. */
    fun cargarDisponibilidad(idMedico: String?, fecha: String) {
        disponibilidadJob?.cancel()
        if (idMedico.isNullOrBlank() || !FECHA_ISO.matches(fecha.trim())) {
            _agenda.update { it.copy(disponibilidad = null) }
            return
        }
        val consulta = DisponibilidadUi(idMedico = idMedico, fecha = fecha.trim())
        _agenda.update { it.copy(disponibilidad = consulta) }
        disponibilidadJob = viewModelScope.launch {
            val res = getDisponibilidadUseCase(idMedico, fecha.trim())
            _agenda.update {
                it.copy(
                    disponibilidad = consulta.copy(
                        bloques = res.getOrDefault(emptyList()),
                        cargando = false,
                        error = res.exceptionOrNull()?.message
                    )
                )
            }
        }
    }

    /** Busca pacientes registrados por nombre, DUI o expediente (con pausa al escribir). */
    fun buscarPacientes(texto: String) {
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            delay(350)
            _agenda.update { it.copy(buscandoPacientes = true) }
            val res = buscarPacientesParaCitaUseCase(texto)
            _agenda.update {
                it.copy(pacientesEncontrados = res.getOrDefault(emptyList()), buscandoPacientes = false)
            }
            res.exceptionOrNull()?.let { mostrarError("No se pudo buscar", it) }
        }
    }

    fun limpiarBusquedaPacientes() {
        busquedaJob?.cancel()
        _agenda.update { it.copy(pacientesEncontrados = emptyList(), buscandoPacientes = false) }
    }

    fun limpiarError(origen: OrigenProgramacion) {
        if (_errorProgramacion.value?.origen == origen) _errorProgramacion.value = null
    }

    fun agendarCita(
        idPaciente: String?,
        programacion: ProgramacionCita,
        motivo: String,
        onSuccess: () -> Unit
    ) = enviar(OrigenProgramacion.FORMULARIO, onSuccess, "Cita agendada") {
        agendarCitaUseCase(idPaciente, programacion, motivo)
    }

    fun confirmarSolicitud(citaId: String, programacion: ProgramacionCita, onSuccess: () -> Unit) =
        enviar(OrigenProgramacion.DIALOGO, onSuccess, "Solicitud confirmada") {
            confirmarCitaUseCase(citaId, programacion)
        }

    fun reprogramarCita(citaId: String, programacion: ProgramacionCita, onSuccess: () -> Unit) =
        enviar(OrigenProgramacion.DIALOGO, onSuccess, "Cita reprogramada") {
            reprogramarCitaUseCase(citaId, programacion)
        }

    private fun enviar(
        origen: OrigenProgramacion,
        onSuccess: () -> Unit,
        titulo: String,
        accion: suspend () -> Result<Cita>
    ) {
        if (_agenda.value.enviando) return
        viewModelScope.launch {
            _agenda.update { it.copy(enviando = true) }
            _errorProgramacion.value = null
            accion().fold(
                onSuccess = { cita ->
                    _uiEvent.value = CitasUiEvent(title = titulo, message = resumen(cita))
                    onSuccess()
                },
                onFailure = { err ->
                    _errorProgramacion.value = ErrorProgramacion(
                        origen = origen,
                        campo = (err as? ApiValidationException)?.campo,
                        mensaje = err.message ?: "Revise los datos de la cita."
                    )
                    // La API pudo rechazar el bloque porque otro lo tomó: refrescar disponibilidad.
                    _agenda.value.disponibilidad?.let { cargarDisponibilidad(it.idMedico, it.fecha) }
                }
            )
            _agenda.update { it.copy(enviando = false) }
        }
    }

    fun registrarLlegada(citaId: String) {
        viewModelScope.launch {
            marcarLlegadaCitaUseCase(citaId).fold(
                onSuccess = {
                    _uiEvent.value = CitasUiEvent(
                        title = "Llegada registrada",
                        message = "${it.paciente?.nombreCompleto ?: "El paciente"} pasó a sala de espera."
                    )
                },
                onFailure = { mostrarError("No se registró la llegada", it) }
            )
        }
    }

    fun cancelarCita(citaId: String, motivo: String) {
        viewModelScope.launch {
            cancelarCitaUseCase(citaId, motivo).fold(
                onSuccess = {
                    _uiEvent.value = CitasUiEvent(
                        title = "Cita cancelada",
                        message = "El horario quedó libre en la agenda del médico."
                    )
                },
                onFailure = { mostrarError("No se canceló la cita", it) }
            )
        }
    }

    fun guardarTriajeOPasarConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ) {
        viewModelScope.launch {
            registrarTriajeYPasarConsultaUseCase(citaId, signosVitales, pasarAConsulta).fold(
                onSuccess = { msg ->
                    _uiEvent.value = CitasUiEvent(
                        title = if (pasarAConsulta) "Pase a consulta" else "Triaje registrado",
                        message = msg
                    )
                },
                onFailure = { mostrarError("Error en triaje", it) }
            )
        }
    }

    fun reLlamarEnSala() {
        val actual = controlarLlamadorSalaUseCase.reLlamar()
        if (actual != null) {
            _uiEvent.value = CitasUiEvent(
                title = "Llamador de sala",
                message = "Llamando de nuevo a ${actual.consultorio?.codigo ?: "consultorio"}."
            )
        }
    }

    fun llamarSiguienteEnSala() {
        val sig = controlarLlamadorSalaUseCase.llamarSiguiente()
        _uiEvent.value = if (sig != null) {
            CitasUiEvent(
                title = "Siguiente turno",
                message = "${sig.paciente?.nombreCompleto ?: "Paciente"} pasa a ${sig.consultorio?.codigo ?: "consultorio"}."
            )
        } else {
            CitasUiEvent(
                title = "Sin pacientes en espera",
                message = "No hay citas pendientes en sala de espera.",
                isError = true
            )
        }
    }

    fun consumeEvent() {
        _uiEvent.value = null
    }

    private fun mostrarError(titulo: String, err: Throwable) {
        _uiEvent.value = CitasUiEvent(titulo, err.message ?: "Intente de nuevo.", isError = true)
    }

    private fun resumen(cita: Cita): String =
        "${cita.paciente?.nombreCompleto ?: "Paciente"} con ${cita.medicoNombre ?: "el médico"} " +
            "el ${cita.fecha} a las ${cita.horaInicio}."

    companion object {
        private val FECHA_ISO = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
