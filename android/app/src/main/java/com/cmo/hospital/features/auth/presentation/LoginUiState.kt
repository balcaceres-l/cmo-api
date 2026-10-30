package com.cmo.hospital.features.auth.presentation

import com.cmo.hospital.core.domain.model.User

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: User? = null
)

interface AuthNavigationHost {
    fun onAuthenticated(user: User)
    fun onLogoutCompleted()
}
