package com.cmo.hospital.features.doctor.expedientes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.FiltrarExpedientesUseCase
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.GetExpedientesFlowUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CambiarEstadoOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpedientesUiState(
    val query: String = "",
    val resultados: List<ExpedienteDetalle> = emptyList(),
    val expedienteSeleccionado: ExpedienteDetalle? = null
)

class ExpedientesViewModel(
    getExpedientesFlowUseCase: GetExpedientesFlowUseCase,
    private val expedientesRepository: ExpedientesRepository,
    private val filtrarExpedientesUseCase: FiltrarExpedientesUseCase,
    private val emitirOrdenLabUseCase: EmitirOrdenLabUseCase,
    private val cambiarEstadoOrdenLabUseCase: CambiarEstadoOrdenLabUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedNumeroExp = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            expedientesRepository.expedienteSolicitadoFlow.collect { solicitado ->
                if (!solicitado.isNullOrBlank()) {
                    _searchQuery.value = ""
                    _selectedNumeroExp.value = solicitado
                    expedientesRepository.consumirExpedienteSolicitado()
                }
            }
        }
    }

    val uiState: StateFlow<ExpedientesUiState> = combine(
        getExpedientesFlowUseCase(),
        _searchQuery,
        _selectedNumeroExp
    ) { todos, query, selExp ->
        val filtrados = filtrarExpedientesUseCase(todos, query)
        val seleccionado = filtrados.firstOrNull {
            it.paciente.numeroExpediente == selExp
        } ?: filtrados.firstOrNull()
        ExpedientesUiState(
            query = query,
            resultados = filtrados,
            expedienteSeleccionado = seleccionado
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpedientesUiState())

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectExpediente(numeroExpediente: String?) {
        _selectedNumeroExp.value = numeroExpediente
    }

    fun emitirOrdenDesdeExpediente(
        paciente: Paciente,
        examenes: List<String>,
        prioridad: String,
        obs: String,
        estadoInicial: String
    ) {
        emitirOrdenLabUseCase(examenes, prioridad, obs, paciente, estadoInicial).fold(
            onSuccess = { creadas ->
                _toastEvent.value = Triple(
                    "Examen Vinculado a Expediente",
                    "${creadas.size} orden(es) añadidas a ${paciente.numeroExpediente} y enviadas a LIS.",
                    false
                )
            },
            onFailure = { err ->
                _toastEvent.value = Triple(
                    "Orden Incompleta",
                    err.message ?: "Seleccione al menos un examen.",
                    true
                )
            }
        )
    }

    fun cambiarEstadoOrdenLab(ordenId: String, nuevoEstado: String) {
        cambiarEstadoOrdenLabUseCase(ordenId, nuevoEstado).onSuccess {
            _toastEvent.value = Triple(
                "Estado LIS Actualizado",
                "Orden cambiada a '$nuevoEstado' en expediente y LIS.",
                false
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
