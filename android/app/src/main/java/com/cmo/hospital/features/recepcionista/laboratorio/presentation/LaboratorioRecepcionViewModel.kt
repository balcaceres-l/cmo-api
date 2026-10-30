package com.cmo.hospital.features.recepcionista.laboratorio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.AvanzarOrdenLaboratorioUseCase
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.FiltroLaboratorio
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.ListarOrdenesLaboratorioUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LaboratorioRecepcionUiState(
    val filtro: FiltroLaboratorio = FiltroLaboratorio.PENDIENTES,
    val ordenes: List<OrdenLaboratorioItem> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    /** Órdenes con una petición en curso, para no enviar el mismo paso dos veces. */
    val procesando: Set<String> = emptySet()
)

class LaboratorioRecepcionViewModel(
    private val listarOrdenesLaboratorioUseCase: ListarOrdenesLaboratorioUseCase,
    private val avanzarOrdenLaboratorioUseCase: AvanzarOrdenLaboratorioUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LaboratorioRecepcionUiState())
    val state: StateFlow<LaboratorioRecepcionUiState> = _state.asStateFlow()

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    init {
        recargar()
    }

    fun seleccionarFiltro(filtro: FiltroLaboratorio) {
        if (filtro == _state.value.filtro) return
        _state.value = _state.value.copy(filtro = filtro)
        recargar()
    }

    fun recargar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true, error = null)
            listarOrdenesLaboratorioUseCase(_state.value.filtro).fold(
                onSuccess = { _state.value = _state.value.copy(ordenes = it, cargando = false) },
                onFailure = { _state.value = _state.value.copy(cargando = false, error = it.message) }
            )
        }
    }

    fun avanzar(orden: OrdenLaboratorioItem) {
        if (orden.id in _state.value.procesando) return
        _state.value = _state.value.copy(procesando = _state.value.procesando + orden.id)
        viewModelScope.launch {
            avanzarOrdenLaboratorioUseCase(orden).fold(
                onSuccess = { actualizada ->
                    // La API devuelve la orden sin paciente ni examen: se conservan los de la lista.
                    val nueva = orden.copy(estado = actualizada.estado, fechaResultado = actualizada.fechaResultado)
                    val filtro = _state.value.filtro
                    val siguePorFiltro = filtro.estadoApi == null || nueva.estado == orden.estado
                    _state.value = _state.value.copy(
                        ordenes = _state.value.ordenes.mapNotNull {
                            when {
                                it.id != orden.id -> it
                                siguePorFiltro -> nueva
                                else -> null
                            }
                        }
                    )
                    _toastEvent.value = Triple(
                        if (nueva.estado == OrdenLaboratorioItem.COMPLETADO) "Orden Completada" else "Orden en Proceso",
                        "${orden.examenNombre} • ${orden.pacienteNombre ?: "Paciente"}",
                        false
                    )
                },
                onFailure = { err ->
                    _toastEvent.value = Triple("No se pudo actualizar", err.message ?: "Intente de nuevo.", true)
                    // Otro usuario pudo haberla movido: se vuelve a leer el estado real.
                    recargar()
                }
            )
            _state.value = _state.value.copy(procesando = _state.value.procesando - orden.id)
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
