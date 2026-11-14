package com.cmo.hospital.features.auth.data.repository

import com.cmo.hospital.core.config.AppConfig
import com.cmo.hospital.core.domain.model.RoleAccessPolicy
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.dto.DemoPersonalItem
import com.cmo.hospital.core.network.dto.LoginRequest
import com.cmo.hospital.core.session.SessionManager
import com.cmo.hospital.features.auth.domain.repository.AuthRepository
import com.cmo.hospital.features.auth.domain.repository.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.SocketTimeoutException

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(usuario: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(usuario, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    val user = data.usuario
                    if (!RoleAccessPolicy.isAllowedInMobileApp(user.rol)) {
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
        } catch (_: ConnectException) {
            AuthResult.Error(
                "No se pudo conectar al servidor API (${AppConfig.API_BASE_URL}). Verifique que el backend Laravel esté encendido en el puerto 8000."
            )
        } catch (_: SocketTimeoutException) {
            AuthResult.Error(
                "Tiempo de espera agotado al conectar con el servidor API. Verifique la red o que el servidor Laravel esté activo."
            )
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "No se pudo conectar con el servidor.")
        }
    }

    override suspend fun getPersonalDemo(): Result<List<DemoPersonalItem>> {
        return try {
            val response = withTimeoutOrNull(6_000) { apiService.getPersonalDemo() }
                ?: return Result.failure(IllegalStateException("Tiempo de espera agotado al consultar el personal."))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data.orEmpty())
            } else {
                Result.failure(
                    IllegalStateException(
                        response.body()?.message ?: "No se pudo cargar el personal demo (${response.code()})."
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: ConnectException) {
            Result.failure(
                IllegalStateException("No se pudo conectar con la API (${AppConfig.API_BASE_URL}).")
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Error al cargar el personal."))
        }
    }

    override suspend fun logout() {
        // Revoca el token en el servidor, pero sin dejar al usuario esperando
        // si la red falla: la sesión local se borra igual.
        try {
            withTimeoutOrNull(5_000) { apiService.logout() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        sessionManager.clear()
    }

    override fun getCurrentUser(): User? = sessionManager.getUser()

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
}
