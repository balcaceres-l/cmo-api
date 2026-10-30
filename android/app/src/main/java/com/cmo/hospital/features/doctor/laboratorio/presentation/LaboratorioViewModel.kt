package com.cmo.hospital.features.doctor.laboratorio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CambiarEstadoOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.GetOrdenesLabFlowUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LaboratorioViewModel(
    getOrdenesLabFlowUseCase: GetOrdenesLabFlowUseCase,
    val expedientesFlow: StateFlow<List<ExpedienteDetalle>>,
    private val emitirOrdenLabUseCase: EmitirOrdenLabUseCase,
    private val cambiarEstadoOrdenLabUseCase: CambiarEstadoOrdenLabUseCase
) : ViewModel() {

    private val _filtroEstado = MutableStateFlow("Todos")
    val filtroEstado: StateFlow<String> = _filtroEstado.asStateFlow()

    val ordenesFiltradas: StateFlow<List<OrdenLaboratorioItem>> = combine(
        getOrdenesLabFlowUseCase(),
        _filtroEstado
    ) { todas, filtro ->
        if (filtro == "Todos") todas else todas.filter { it.estado == filtro }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    fun setFiltroEstado(estado: String) {
        _filtroEstado.value = estado
    }

    fun emitirOrdenes(
        examenes: List<String>,
        prioridad: String,
        obs: String,
        estadoInicial: String,
        paciente: Paciente?
    ) {
        emitirOrdenLabUseCase(examenes, prioridad, obs, paciente, estadoInicial).fold(
            onSuccess = { creadas ->
                _toastEvent.value = Triple(
                    "Orden LIS Emitida",
                    "${creadas.size} examen(es) registrados en expediente y enviados a Laboratorio.",
                    false
                )
            },
            onFailure = { err ->
                _toastEvent.value = Triple(
                    "Datos Incompletos",
                    err.message ?: "Seleccione al menos un examen.",
                    true
                )
            }
        )
    }

    fun cambiarEstado(ordenId: String, nuevoEstado: String) {
        cambiarEstadoOrdenLabUseCase(ordenId, nuevoEstado).onSuccess {
            _toastEvent.value = Triple(
                "Estado LIS Actualizado",
                "Orden actualizada a '$nuevoEstado' en LIS, Consulta y Expediente.",
                false
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
