package com.cmo.hospital.features.auth.domain.repository

import com.cmo.hospital.core.domain.model.User

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class RoleNotAllowed(val role: String) : AuthResult()
}

interface AuthRepository {
    suspend fun login(usuario: String, password: String): AuthResult
    suspend fun logout()
    fun getCurrentUser(): User?
    fun isLoggedIn(): Boolean
}
