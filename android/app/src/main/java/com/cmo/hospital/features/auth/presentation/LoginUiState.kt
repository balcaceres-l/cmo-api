package com.cmo.hospital.features.auth.presentation

import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.dto.DemoPersonalItem

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: User? = null,
    val demoStaff: List<DemoPersonalItem> = emptyList(),
    val isLoadingDemoStaff: Boolean = false,
    val demoStaffError: String? = null
)

interface AuthNavigationHost {
    fun onAuthenticated(user: User)
    fun onLogoutCompleted()
}
