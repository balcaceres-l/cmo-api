package com.cmo.hospital.features.doctor.expedientes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.BuscarPacientesTratadosUseCase
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.CargarExpedienteUseCase
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.GuardarAntecedentesUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CancelarOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpedientesUiState(
    val query: String = "",
    val resultados: List<Paciente> = emptyList(),
    val buscando: Boolean = false,
    val idSeleccionado: String? = null,
    val expedienteSeleccionado: ExpedienteDetalle? = null,
    val cargandoDetalle: Boolean = false,
    val error: String? = null
)

class ExpedientesViewModel(
    private val expedientesRepository: ExpedientesRepository,
    private val buscarPacientesTratadosUseCase: BuscarPacientesTratadosUseCase,
    private val cargarExpedienteUseCase: CargarExpedienteUseCase,
    private val guardarAntecedentesUseCase: GuardarAntecedentesUseCase,
    private val emitirOrdenLabUseCase: EmitirOrdenLabUseCase,
    private val cancelarOrdenLabUseCase: CancelarOrdenLabUseCase,
    private val pausaBusquedaMs: Long = 350
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpedientesUiState())
    val uiState: StateFlow<ExpedientesUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    private var busquedaJob: Job? = null
    private var detalleJob: Job? = null

    /** Número de expediente a abrir cuando lleguen los resultados de la búsqueda. */
    private var expedientePendiente: String? = null

    init {
        buscar("", inmediato = true)
        viewModelScope.launch {
            expedientesRepository.expedienteSolicitadoFlow.collect { solicitado ->
                if (!solicitado.isNullOrBlank()) {
                    expedientesRepository.consumirExpedienteSolicitado()
                    expedientePendiente = solicitado
                    buscar(solicitado, inmediato = true)
                }
            }
        }
    }

    fun onQueryChanged(query: String) {
        if (query == _uiState.value.query) return
        buscar(query, inmediato = false)
    }

    private fun buscar(query: String, inmediato: Boolean) {
        _uiState.update { it.copy(query = query) }
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            if (!inmediato) delay(pausaBusquedaMs)
            _uiState.update { it.copy(buscando = true, error = null) }
            buscarPacientesTratadosUseCase(query).fold(
                onSuccess = { lista ->
                    _uiState.update { it.copy(resultados = lista, buscando = false) }
                    val pedido = expedientePendiente?.let { num -> lista.firstOrNull { it.numeroExpediente == num } }
                    expedientePendiente = null
                    val actual = _uiState.value.idSeleccionado
                    when {
                        pedido != null -> seleccionar(pedido.id)
                        actual == null || lista.none { it.id == actual } -> seleccionar(lista.firstOrNull()?.id)
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(buscando = false, error = err.message) }
                }
            )
        }
    }

    fun seleccionar(idPaciente: String?) {
        _uiState.update { it.copy(idSeleccionado = idPaciente) }
        recargarDetalle()
    }

    private fun recargarDetalle() {
        val id = _uiState.value.idSeleccionado
        detalleJob?.cancel()
        if (id == null) {
            _uiState.update { it.copy(expedienteSeleccionado = null, cargandoDetalle = false) }
            return
        }
        detalleJob = viewModelScope.launch {
            _uiState.update { it.copy(cargandoDetalle = true) }
            cargarExpedienteUseCase(id).fold(
                onSuccess = { exp -> _uiState.update { it.copy(expedienteSeleccionado = exp, cargandoDetalle = false, error = null) } },
                onFailure = { err ->
                    _uiState.update { it.copy(expedienteSeleccionado = null, cargandoDetalle = false, error = err.message) }
                }
            )
        }
    }

    fun guardarAntecedentes(personales: String, familiares: String) {
        val id = _uiState.value.idSeleccionado ?: return
        viewModelScope.launch {
            guardarAntecedentesUseCase(id, personales, familiares).fold(
                onSuccess = {
                    _toastEvent.value = Triple("Antecedentes guardados", "El historial clínico quedó actualizado.", false)
                    recargarDetalle()
                },
                onFailure = { err -> _toastEvent.value = Triple("No se guardó", err.message ?: "Intente de nuevo.", true) }
            )
        }
    }

    fun catalogoExamenes(): List<String> = emitirOrdenLabUseCase.catalogo()

    /** La API solo acepta órdenes dentro de una consulta abierta del mismo paciente. */
    fun emitirOrdenDesdeExpediente(
        paciente: Paciente,
        examenes: List<String>,
        prioridad: String,
        obs: String
    ) {
        viewModelScope.launch {
            emitirOrdenLabUseCase(examenes, prioridad, obs, paciente).fold(
                onSuccess = { creadas ->
                    _toastEvent.value = Triple(
                        "Orden de laboratorio emitida",
                        "${creadas.size} examen(es) enviados al laboratorio como Pendiente.",
                        false
                    )
                    recargarDetalle()
                },
                onFailure = { err ->
                    _toastEvent.value = Triple("Orden no emitida", err.message ?: "Seleccione al menos un examen.", true)
                }
            )
        }
    }

    fun cancelarOrdenLab(orden: OrdenLaboratorioItem) {
        viewModelScope.launch {
            cancelarOrdenLabUseCase(orden).fold(
                onSuccess = {
                    _toastEvent.value = Triple("Orden cancelada", "${it.examenNombre} ya no se realizará.", false)
                    recargarDetalle()
                },
                onFailure = { err -> _toastEvent.value = Triple("No se pudo cancelar", err.message ?: "Intente de nuevo.", true) }
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
