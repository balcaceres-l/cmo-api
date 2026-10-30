package com.cmo.hospital.features.doctor.laboratorio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CancelarOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.GetOrdenesLabFlowUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.RefrescarLaboratorioUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LaboratorioViewModel(
    getOrdenesLabFlowUseCase: GetOrdenesLabFlowUseCase,
    val consultaActivaFlow: StateFlow<ConsultaActiva>,
    private val emitirOrdenLabUseCase: EmitirOrdenLabUseCase,
    private val cancelarOrdenLabUseCase: CancelarOrdenLabUseCase,
    private val refrescarLaboratorioUseCase: RefrescarLaboratorioUseCase
) : ViewModel() {

    private val _filtroEstado = MutableStateFlow("Todos")
    val filtroEstado: StateFlow<String> = _filtroEstado.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    val ordenesFiltradas: StateFlow<List<OrdenLaboratorioItem>> = combine(
        getOrdenesLabFlowUseCase(),
        _filtroEstado
    ) { todas, filtro ->
        if (filtro == "Todos") todas else todas.filter { it.estado == filtro }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    init {
        refrescar()
    }

    /** Trae de la API el estado actual de las órdenes (el laboratorio las avanza desde recepción o la web). */
    fun refrescar() {
        viewModelScope.launch {
            _cargando.value = true
            refrescarLaboratorioUseCase().onFailure { err ->
                _toastEvent.value = Triple(
                    "Laboratorio sin conexión",
                    err.message ?: "No se pudieron cargar las órdenes.",
                    true
                )
            }
            _cargando.value = false
        }
    }

    fun setFiltroEstado(estado: String) {
        _filtroEstado.value = estado
    }

    fun catalogoExamenes(): List<String> = emitirOrdenLabUseCase.catalogo()

    /** Paciente de la consulta abierta, el único para el que la API acepta órdenes. */
    fun consultaAbierta(): ConsultaActiva? =
        consultaActivaFlow.value.takeIf { !it.finalizada && it.esRemota }

    fun emitirOrdenes(examenes: List<String>, prioridad: String, obs: String) {
        val consulta = consultaAbierta()
        viewModelScope.launch {
            emitirOrdenLabUseCase(examenes, prioridad, obs, consulta?.paciente).fold(
                onSuccess = { creadas ->
                    _toastEvent.value = Triple(
                        "Orden de Laboratorio Emitida",
                        "${creadas.size} examen(es) enviados al laboratorio como Pendiente.",
                        false
                    )
                },
                onFailure = { err ->
                    _toastEvent.value = Triple("Orden no emitida", err.message ?: "Seleccione al menos un examen.", true)
                }
            )
        }
    }

    fun cancelarOrden(orden: OrdenLaboratorioItem) {
        viewModelScope.launch {
            cancelarOrdenLabUseCase(orden).fold(
                onSuccess = { _toastEvent.value = Triple("Orden Cancelada", "${it.examenNombre} ya no se realizará.", false) },
                onFailure = { err -> _toastEvent.value = Triple("No se pudo cancelar", err.message ?: "Intente de nuevo.", true) }
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
