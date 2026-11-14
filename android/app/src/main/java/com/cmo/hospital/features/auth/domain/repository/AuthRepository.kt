package com.cmo.hospital.features.auth.domain.repository

import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.dto.DemoPersonalItem

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class RoleNotAllowed(val role: String) : AuthResult()
}

interface AuthRepository {
    suspend fun login(usuario: String, password: String): AuthResult
    suspend fun getPersonalDemo(): Result<List<DemoPersonalItem>>
    suspend fun logout()
    fun getCurrentUser(): User?
    fun isLoggedIn(): Boolean
}
