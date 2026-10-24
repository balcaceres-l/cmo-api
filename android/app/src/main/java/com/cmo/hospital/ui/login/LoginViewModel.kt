package com.cmo.hospital.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.data.model.User
import com.cmo.hospital.repository.AuthRepository
import com.cmo.hospital.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: User? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(usuario: String, pass: String) {
        if (usuario.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa usuario y contraseña")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(usuario.trim(), pass)) {
                is AuthResult.Success -> {
                    _uiState.value = LoginUiState(isLoading = false, loggedInUser = result.user)
                }
                is AuthResult.RoleNotAllowed -> {
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "Acceso reservado a Personal Clínico (Médicos y Recepción). El rol ${result.role} debe ingresar desde el Panel Web o App iOS."
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = LoginUiState(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun getExistingUser(): User? = authRepository.getCurrentUser()
}
