package com.cmo.hospital.core.network

import com.cmo.hospital.core.session.EventosSesion
import com.cmo.hospital.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega el token Bearer y detecta la sesión vencida: un 401 con token
 * (fuera del login y del logout) borra la sesión y avisa para volver al login.
 */
class AuthInterceptor(
    private val token: () -> String?,
    private val alExpirarSesion: () -> Unit
) : Interceptor {

    constructor(sessionManager: SessionManager) : this(
        token = sessionManager::getToken,
        alExpirarSesion = {
            sessionManager.clear()
            EventosSesion.avisarSesionExpirada()
        }
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        val enviado = token()
        if (!enviado.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $enviado")
        }

        val response = chain.proceed(builder.build())

        // En el login un 401 son credenciales malas, no una sesión vencida. Solo
        // se borra si el token no cambió mientras tanto (otro login pudo entrar).
        val ruta = originalRequest.url.encodedPath
        if (response.code == 401 && !enviado.isNullOrBlank() &&
            !ruta.endsWith("/auth/login") && !ruta.endsWith("/auth/logout") &&
            token() == enviado
        ) {
            alExpirarSesion()
        }
        return response
    }
}
