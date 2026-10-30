package com.cmo.hospital.features.auth.domain.usecase

import com.cmo.hospital.features.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}
