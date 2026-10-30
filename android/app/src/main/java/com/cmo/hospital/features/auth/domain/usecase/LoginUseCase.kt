package com.cmo.hospital.features.auth.domain.usecase

import com.cmo.hospital.core.domain.model.RoleAccessPolicy
import com.cmo.hospital.features.auth.domain.repository.AuthRepository
import com.cmo.hospital.features.auth.domain.repository.AuthResult

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(usuario: String, password: String): AuthResult {
        val cleanUser = usuario.trim()
        if (cleanUser.isBlank() || password.isBlank()) {
            return AuthResult.Error("Por favor ingresa tu usuario y contraseña.")
        }

        val result = authRepository.login(cleanUser, password)
        return when (result) {
            is AuthResult.Success -> {
                if (!RoleAccessPolicy.isAllowedInMobileApp(result.user.rol)) {
                    authRepository.logout()
                    AuthResult.RoleNotAllowed(result.user.rol)
                } else {
                    result
                }
            }
            else -> result
        }
    }
}
