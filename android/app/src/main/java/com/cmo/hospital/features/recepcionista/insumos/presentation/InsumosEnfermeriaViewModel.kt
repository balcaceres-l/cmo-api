package com.cmo.hospital.features.recepcionista.insumos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.dto.RecursoDto
import com.cmo.hospital.core.network.llamarApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InsumosUiState(
    val todos: List<RecursoDto> = emptyList(),
    val filtrados: List<RecursoDto> = emptyList(),
    val query: String = "",
    val categoria: String = "Todos",
    val cargando: Boolean = false,
    val error: String? = null
)

class InsumosEnfermeriaViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(InsumosUiState())
    val state: StateFlow<InsumosUiState> = _state.asStateFlow()

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    init {
        recargar()
    }

    fun recargar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true, error = null)
            llamarApi { apiService.getRecursos(perPage = 100) }.fold(
                onSuccess = { page ->
                    val lista = page.data
                    _state.value = _state.value.copy(
                        todos = lista,
                        cargando = false,
                        filtrados = aplicarFiltros(lista, _state.value.query, _state.value.categoria)
                    )
                },
                onFailure = { err ->
                    _state.value = _state.value.copy(
                        cargando = false,
                        error = err.message ?: "No se pudieron cargar los insumos."
                    )
                }
            )
        }
    }

    fun onQueryChanged(query: String) {
        val st = _state.value
        _state.value = st.copy(
            query = query,
            filtrados = aplicarFiltros(st.todos, query, st.categoria)
        )
    }

    fun onCategoriaSelected(categoria: String) {
        val st = _state.value
        _state.value = st.copy(
            categoria = categoria,
            filtrados = aplicarFiltros(st.todos, st.query, categoria)
        )
    }

    fun registrarSalida(recurso: RecursoDto, cantidad: Int, motivo: String) {
        if (cantidad <= 0) {
            _toastEvent.value = Triple("Cantidad inválida", "Debe ser mayor a 0.", true)
            return
        }
        if (cantidad > recurso.cantidad) {
            _toastEvent.value = Triple("Stock insuficiente", "Disponible: ${recurso.cantidad} ${recurso.unidad}.", true)
            return
        }
        if (motivo.isBlank()) {
            _toastEvent.value = Triple("Motivo requerido", "Indica el paciente o cama destino.", true)
            return
        }
        viewModelScope.launch {
            llamarApi {
                apiService.registrarMovimientoRecurso(
                    recurso.id,
                    mapOf(
                        "tipo" to "SALIDA",
                        "cantidad" to cantidad,
                        "motivo" to motivo.trim()
                    )
                )
            }.fold(
                onSuccess = {
                    _toastEvent.value = Triple(
                        "Descargo registrado",
                        "-$cantidad ${recurso.unidad} de ${recurso.nombre}",
                        false
                    )
                    recargar()
                },
                onFailure = { err ->
                    _toastEvent.value = Triple(
                        "No se pudo descargar",
                        err.message ?: "Intenta nuevamente.",
                        true
                    )
                }
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }

    private fun aplicarFiltros(
        lista: List<RecursoDto>,
        query: String,
        categoria: String
    ): List<RecursoDto> {
        val q = query.trim().lowercase()
        return lista.filter { item ->
            val matchCat = categoria == "Todos" || item.categoria.equals(categoria, ignoreCase = true)
            val matchQ = q.isEmpty() ||
                item.nombre.lowercase().contains(q) ||
                (item.ubicacion ?: "").lowercase().contains(q)
            matchCat && matchQ
        }
    }
}

