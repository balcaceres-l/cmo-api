package com.cmo.hospital.features.auth.domain.usecase

import com.cmo.hospital.core.domain.model.RoleAccessPolicy
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.features.auth.domain.repository.AuthRepository

class GetActiveUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): User? {
        if (!authRepository.isLoggedIn()) return null
        val user = authRepository.getCurrentUser() ?: return null
        return if (RoleAccessPolicy.isAllowedInMobileApp(user.rol)) user else null
    }
}
