package com.cmo.hospital.ui.doctor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.data.model.*
import com.cmo.hospital.repository.AuthRepository
import com.cmo.hospital.repository.DoctorClinicalRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DoctorUiState(
    val currentUser: User? = null,
    val selectedTab: Int = 0, // 0: Consulta Activa, 1: Expedientes, 2: Laboratorio LIS
    val isCronometroCorriendo: Boolean = true,
    val userMessage: String? = null,
    val errorMessage: String? = null
)

class DoctorViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val clinicalRepository = DoctorClinicalRepository(application)

    private val _uiState = MutableStateFlow(DoctorUiState(currentUser = authRepository.getCurrentUser()))
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    val consultaActiva = clinicalRepository.consultaActiva
    val ordenesLab = clinicalRepository.ordenesLab
    val expedientes = clinicalRepository.expedientes

    init {
        // Ticker de cronómetro de consulta (30 minutos con semáforo)
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.isCronometroCorriendo && !consultaActiva.value.finalizada) {
                    val nuevosSeg = consultaActiva.value.segundosTranscurridos + 1
                    clinicalRepository.actualizarSegundos(nuevosSeg)
                }
            }
        }
    }

    fun cargarDatos() {
        // Datos reactivos inicializados por repositorio clínico
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun toggleCronometro() {
        _uiState.value = _uiState.value.copy(isCronometroCorriendo = !_uiState.value.isCronometroCorriendo)
    }

    /**
     * El Doctor actualiza o registra directamente los Signos Vitales del paciente
     * (esencial cuando el paciente pasa de emergencia sin signos previos de recepción).
     */
    fun actualizarSignosVitales(signos: SignosVitales) {
        clinicalRepository.actualizarSignosVitales(signos)
        _uiState.value = _uiState.value.copy(userMessage = "Signos vitales clínicos actualizados.")
    }

    fun agregarCie10(codigo: String, descripcion: String) {
        clinicalRepository.agregarDiagnostico(codigo, descripcion)
    }

    fun eliminarCie10(codigo: String) {
        clinicalRepository.eliminarDiagnostico(codigo)
    }

    fun guardarAnamnesis(texto: String) {
        clinicalRepository.actualizarAnamnesis(texto)
    }

    fun agregarReceta(receta: RecetaItem) {
        clinicalRepository.agregarReceta(receta)
        _uiState.value = _uiState.value.copy(userMessage = "Medicamento agregado a la receta.")
    }

    fun eliminarReceta(id: String) {
        clinicalRepository.eliminarReceta(id)
    }

    fun finalizarConsulta(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = clinicalRepository.finalizarConsulta()
            _uiState.value = _uiState.value.copy(
                userMessage = "✅ Consulta finalizada y receta digital firmada electrónicamente."
            )
            onSuccess()
        }
    }

    fun emitirOrdenLab(
        examenes: List<String>,
        prioridad: String,
        obs: String,
        paciente: Paciente? = null,
        estadoInicial: String = "En Espera"
    ) {
        viewModelScope.launch {
            val res = clinicalRepository.emitirOrdenLaboratorio(examenes, prioridad, obs, paciente, estadoInicial)
            _uiState.value = _uiState.value.copy(userMessage = "✅ Exámenes indicados guardados en el expediente y enviados al laboratorio.")
        }
    }

    /**
     * El Doctor es el encargado de cambiar el estado de los laboratorios
     * (En Espera, Agendado, En Proceso, Realizado, Cancelado).
     */
    fun cambiarEstadoLaboratorio(ordenId: String, nuevoEstado: String) {
        viewModelScope.launch {
            val res = clinicalRepository.cambiarEstadoLaboratorio(ordenId, nuevoEstado)
            _uiState.value = _uiState.value.copy(userMessage = "Estado actualizado a: $nuevoEstado")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null, errorMessage = null)
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
