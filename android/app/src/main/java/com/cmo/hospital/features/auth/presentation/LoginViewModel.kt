package com.cmo.hospital.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.features.auth.domain.repository.AuthRepository
import com.cmo.hospital.features.auth.domain.repository.AuthResult
import com.cmo.hospital.features.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(usuario: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(usuario, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loggedInUser = result.user
                        )
                    }
                }
                is AuthResult.RoleNotAllowed -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Acceso restringido: El rol '${result.role}' debe ingresar desde el portal web administrativo/pacientes. La app móvil es exclusiva para Médicos y Recepción."
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadDemoStaff(forceRefresh: Boolean = true) {
        if (!forceRefresh && _uiState.value.demoStaff.isNotEmpty()) return
        if (_uiState.value.isLoadingDemoStaff) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDemoStaff = true, demoStaffError = null) }
            authRepository.getPersonalDemo()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoadingDemoStaff = false,
                            demoStaff = list,
                            demoStaffError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingDemoStaff = false,
                            demoStaffError = err.message ?: "No se pudo obtener la lista de personal."
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeLoginEvent() {
        _uiState.update { it.copy(loggedInUser = null) }
    }
}
