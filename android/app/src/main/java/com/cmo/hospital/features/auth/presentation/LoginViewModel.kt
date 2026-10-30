package com.cmo.hospital.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.features.auth.domain.repository.AuthResult
import com.cmo.hospital.features.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(usuario: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true, errorMessage = null)
            when (val result = loginUseCase(usuario, password)) {
                is AuthResult.Success -> {
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        loggedInUser = result.user
                    )
                }
                is AuthResult.RoleNotAllowed -> {
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "Acceso restringido: El rol '${result.role}' debe ingresar desde el portal web administrativo/pacientes. La app móvil es exclusiva para Médicos y Recepción."
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun consumeLoginEvent() {
        _uiState.value = _uiState.value.copy(loggedInUser = null)
    }
}
