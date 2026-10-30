package com.cmo.hospital.features.recepcionista.pacientes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteInvalidoException
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.BuscarPacientesUseCase
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GenerarCodigoActivacionUseCase
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GuardarPacienteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PacientesUiState(
    val pacientes: List<Paciente> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

/** [campo] es el nombre del campo en la API; null si el error no es de un campo concreto. */
data class ErrorFormulario(val campo: String?, val mensaje: String)

data class PacientesUiEvent(
    val title: String,
    val message: String,
    val isError: Boolean = false
)

class PacientesViewModel(
    private val buscarPacientesUseCase: BuscarPacientesUseCase,
    private val guardarPacienteUseCase: GuardarPacienteUseCase,
    private val generarCodigoActivacionUseCase: GenerarCodigoActivacionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PacientesUiState())
    val state: StateFlow<PacientesUiState> = _state.asStateFlow()

    private val _uiEvent = MutableStateFlow<PacientesUiEvent?>(null)
    val uiEvent: StateFlow<PacientesUiEvent?> = _uiEvent.asStateFlow()

    private var query: String = ""
    private var busquedaJob: Job? = null

    init {
        recargar()
    }

    /** Espera a que el usuario deje de escribir para no consultar la API en cada tecla. */
    fun onQueryChanged(texto: String) {
        if (texto.trim() == query) return
        query = texto.trim()
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            delay(350)
            cargar()
        }
    }

    fun recargar() {
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        _state.value = _state.value.copy(cargando = true, error = null)
        buscarPacientesUseCase(query).fold(
            onSuccess = { lista -> _state.value = PacientesUiState(pacientes = lista) },
            onFailure = { err ->
                _state.value = _state.value.copy(cargando = false, error = err.message)
            }
        )
    }

    /**
     * [onTerminado] recibe null si se guardó, o el error para mostrarlo dentro del
     * formulario: el toast del shell queda detrás del diálogo y no se vería.
     */
    fun guardar(paciente: Paciente?, form: PacienteForm, onTerminado: (ErrorFormulario?) -> Unit) {
        viewModelScope.launch {
            guardarPacienteUseCase(paciente?.id, form).fold(
                onSuccess = { guardado ->
                    _uiEvent.value = if (paciente == null) {
                        PacientesUiEvent("Paciente registrado", "${guardado.nombreCompleto} • Exp. ${guardado.numeroExpediente ?: "asignado"}")
                    } else {
                        PacientesUiEvent("Datos actualizados", guardado.nombreCompleto)
                    }
                    onTerminado(null)
                    cargar()
                },
                onFailure = { err ->
                    onTerminado(
                        ErrorFormulario(
                            campo = (err as? PacienteInvalidoException)?.campo,
                            mensaje = err.message ?: "Revise los datos del formulario."
                        )
                    )
                }
            )
        }
    }

    fun generarCodigoActivacion(paciente: Paciente, onCodigo: (String) -> Unit) {
        viewModelScope.launch {
            generarCodigoActivacionUseCase(paciente).fold(
                onSuccess = { onCodigo(it.codigo) },
                onFailure = { err ->
                    _uiEvent.value = PacientesUiEvent(
                        title = "Código no generado",
                        message = err.message ?: "No se pudo generar el código.",
                        isError = true
                    )
                }
            )
        }
    }

    fun consumeEvent() {
        _uiEvent.value = null
    }
}
