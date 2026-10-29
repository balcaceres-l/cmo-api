package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.PersonalGuardia
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AmbulanciaGuardiaViewModel(
    getAmbulanciaFlowUseCase: GetAmbulanciaFlowUseCase,
    private val cambiarEstadoAmbulanciaUseCase: CambiarEstadoAmbulanciaUseCase,
    private val solicitarDespachoAmbulanciaUseCase: SolicitarDespachoAmbulanciaUseCase,
    private val getPersonalGuardiaFiltradoUseCase: GetPersonalGuardiaFiltradoUseCase
) : ViewModel() {

    val ambulancia: StateFlow<AmbulanciaRecurso> = getAmbulanciaFlowUseCase()

    private val _filtroRol = MutableStateFlow("TODOS")
    val filtroRol: StateFlow<String> = _filtroRol.asStateFlow()

    private val _personalFiltrado = MutableStateFlow(getPersonalGuardiaFiltradoUseCase("TODOS"))
    val personalFiltrado: StateFlow<List<PersonalGuardia>> = _personalFiltrado.asStateFlow()

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    fun setFiltroRol(rol: String) {
        _filtroRol.value = rol
        _personalFiltrado.value = getPersonalGuardiaFiltradoUseCase(rol)
    }

    fun cambiarEstadoAmbulancia(nuevoEstado: String) {
        cambiarEstadoAmbulanciaUseCase(nuevoEstado)
        _toastEvent.value = Triple(
            "Estado Operativo Alfa-1",
            "Ambulancia institucional actualizada a $nuevoEstado.",
            false
        )
    }

    fun despacharEmergencia(direccion: String, paciente: String, descripcion: String) {
        viewModelScope.launch {
            val res = solicitarDespachoAmbulanciaUseCase(direccion, paciente, descripcion)
            res.fold(
                onSuccess = {
                    _toastEvent.value = Triple(
                        "Ambulancia Alfa-1 Despachada",
                        "Unidad móvil en Código Rojo rumbo a $direccion (ETA: 15 min).",
                        false
                    )
                },
                onFailure = { err ->
                    _toastEvent.value = Triple(
                        "Dirección Requerida",
                        err.message ?: "Ingrese la dirección de destino.",
                        true
                    )
                }
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
