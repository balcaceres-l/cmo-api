package com.cmo.hospital.repository

import android.content.Context
import com.cmo.hospital.data.local.SessionManager
import com.cmo.hospital.data.model.LoginRequest
import com.cmo.hospital.data.model.User
import com.cmo.hospital.data.remote.RetrofitClient

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class RoleNotAllowed(val role: String) : AuthResult()
}

class AuthRepository(private val context: Context) {
    private val apiService = RetrofitClient.getApiService(context)
    private val sessionManager = SessionManager(context)

    suspend fun login(usuario: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(usuario, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    val user = data.usuario
                    // Validar roles permitidos en Android (MEDICO y RECEPCIONISTA)
                    if (user.rol == "ADMINISTRADOR" || user.rol == "PACIENTE") {
                        AuthResult.RoleNotAllowed(user.rol)
                    } else {
                        sessionManager.saveAuth(data.token, user)
                        AuthResult.Success(user)
                    }
                } else {
                    AuthResult.Error("Respuesta de datos vacía.")
                }
            } else {
                val errorMsg = response.body()?.message ?: when (response.code()) {
                    401 -> "Credenciales incorrectas."
                    429 -> "Demasiados intentos. Espera un momento."
                    else -> "Error en el inicio de sesión (${response.code()})"
                }
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "No se pudo conectar con el servidor.")
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (e: Exception) {}
        sessionManager.clear()
    }

    fun getCurrentUser(): User? = sessionManager.getUser()
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
}
