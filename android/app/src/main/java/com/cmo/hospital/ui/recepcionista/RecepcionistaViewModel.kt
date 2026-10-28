package com.cmo.hospital.ui.recepcionista

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.data.model.*
import com.cmo.hospital.repository.AuthRepository
import com.cmo.hospital.repository.RecepcionistaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecepcionistaUiState(
    val currentUser: User? = null,
    val selectedTab: Int = 0, // 0: Citas y Llamador, 1: Admisión & Camas, 2: Ambulancia & Turnos
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val errorMessage: String? = null
)

class RecepcionistaViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val recepcionistaRepository = RecepcionistaRepository(application)

    private val _uiState = MutableStateFlow(RecepcionistaUiState(currentUser = authRepository.getCurrentUser()))
    val uiState: StateFlow<RecepcionistaUiState> = _uiState.asStateFlow()

    val citas = recepcionistaRepository.citasState
    val turnoEnBox = recepcionistaRepository.turnoEnBox
    val camas = recepcionistaRepository.camasState
    val ambulancia = recepcionistaRepository.ambulanciaState
    val personalGuardia = recepcionistaRepository.getPersonalGuardia()

    init {
        refreshCitas()
    }

    fun cargarDatos() {
        refreshCitas()
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun refreshCitas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            recepcionistaRepository.getCitas()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun agendarCita(
        pacienteNombre: String,
        dui: String,
        fechaNacimiento: String?,
        fecha: String,
        horaInicio: String,
        motivo: String,
        especialidad: String,
        box: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = recepcionistaRepository.agendarCita(
                pacienteNombre = pacienteNombre,
                dui = dui,
                fechaNacimiento = fechaNacimiento,
                fecha = fecha,
                horaInicio = horaInicio,
                motivo = motivo,
                especialidad = especialidad,
                box = box
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userMessage = if (result.isSuccess) "Cita agendada (Bloque de 30 min asignado)." else null,
                errorMessage = result.exceptionOrNull()?.message
            )
            if (result.isSuccess) onSuccess()
        }
    }

    fun registrarLlegada(citaId: String) {
        viewModelScope.launch {
            recepcionistaRepository.marcarLlegada(citaId)
            _uiState.value = _uiState.value.copy(userMessage = "Llegada del paciente confirmada. Pasó a Sala de Espera.")
        }
    }

    fun cancelarCita(citaId: String, motivo: String) {
        viewModelScope.launch {
            recepcionistaRepository.cancelarCita(citaId, motivo)
            _uiState.value = _uiState.value.copy(userMessage = "Cita cancelada correctamente.")
        }
    }

    fun reLlamar() {
        recepcionistaRepository.reLlamarTurno()
        _uiState.value = _uiState.value.copy(userMessage = "Llamado re-emitido a la pantalla pública central.")
    }

    fun siguienteTurno() {
        recepcionistaRepository.siguienteTurno()
        _uiState.value = _uiState.value.copy(userMessage = "Siguiente paciente llamado a Box.")
    }

    fun registrarEIngresarPaciente(
        paciente: Paciente,
        signosVitales: SignosVitales?,
        codigoCama: String?,
        motivo: String,
        esEmergenciaDirecta: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = recepcionistaRepository.registrarEIngresarPaciente(
                paciente = paciente,
                signosVitales = signosVitales,
                codigoCama = codigoCama,
                motivoIngreso = motivo,
                esEmergenciaDirecta = esEmergenciaDirecta
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                userMessage = if (res.isSuccess) {
                    if (esEmergenciaDirecta) "🚨 Paciente derivado DIRECTO a Consulta de Urgencias."
                    else "Paciente registrado e ingresado en $codigoCama."
                } else null,
                errorMessage = res.exceptionOrNull()?.message
            )
            if (res.isSuccess) onSuccess()
        }
    }

    fun cambiarEstadoAmbulancia(nuevoEstado: String) {
        viewModelScope.launch {
            recepcionistaRepository.cambiarEstadoAmbulancia(nuevoEstado)
            _uiState.value = _uiState.value.copy(userMessage = "Estado operativo de ambulancia actualizado a $nuevoEstado.")
        }
    }

    fun solicitarDespacho(direccion: String, paciente: String, descripcion: String, prioridad: String) {
        viewModelScope.launch {
            recepcionistaRepository.solicitarDespachoAmbulancia(direccion, paciente, descripcion, prioridad)
            _uiState.value = _uiState.value.copy(userMessage = "Ambulancia Alfa-1 despachada en Código Rojo.")
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
