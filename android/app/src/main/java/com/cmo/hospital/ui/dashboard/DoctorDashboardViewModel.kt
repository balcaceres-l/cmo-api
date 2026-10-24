package com.cmo.hospital.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.data.model.Cita
import com.cmo.hospital.data.model.DoctorSummary
import com.cmo.hospital.data.model.User
import com.cmo.hospital.repository.AuthRepository
import com.cmo.hospital.repository.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DoctorDashboardUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val summary: DoctorSummary = DoctorSummary(0, 0, 0, "DISPONIBLE"),
    val agendaList: List<Cita> = emptyList(),
    val errorMessage: String? = null
)

class DoctorDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val doctorRepository = DoctorRepository(application)

    private val _uiState = MutableStateFlow(DoctorDashboardUiState())
    val uiState: StateFlow<DoctorDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = authRepository.getCurrentUser()
            val summary = doctorRepository.getDoctorSummary()
            val agenda = doctorRepository.getAgendaDelDia()

            _uiState.value = DoctorDashboardUiState(
                isLoading = false,
                user = user,
                summary = summary,
                agendaList = agenda
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
