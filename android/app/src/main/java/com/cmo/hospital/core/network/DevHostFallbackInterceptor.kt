package com.cmo.hospital.core.network

import android.os.Build
import com.cmo.hospital.core.config.AppConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Interceptor resiliente para entorno de desarrollo local (`DEBUG`).
 * Si `API_BASE_URL` apunta a `localhost` / `127.0.0.1` y el túnel USB `adb reverse` no está activo
 * (p. ej. uso por Wi-Fi sin cable o ejecución en Emulador Android), reintenta automáticamente
 * contra `10.0.2.2` (emulador) y las IPs LAN del servidor de desarrollo (`DEV_FALLBACK_HOSTS`),
 * memorizando el host exitoso para no penalizar llamadas posteriores.
 */
class DevHostFallbackInterceptor : Interceptor {

    @Volatile
    private var preferredHost: String? = null

    private val localDevHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHost = originalRequest.url.host

        if (!AppConfig.IS_DEBUG || originalHost !in localDevHosts) {
            return chain.proceed(originalRequest)
        }

        val candidates = buildCandidateHosts(originalHost)
        var lastException: IOException? = null

        for ((index, candidateHost) in candidates.withIndex()) {
            val candidateUrl = originalRequest.url.newBuilder()
                .host(candidateHost)
                .build()
            val candidateRequest = originalRequest.newBuilder()
                .url(candidateUrl)
                .build()

            // Timeout de conexión ágil (3s) al probar candidatos para respuesta inmediata en UI
            val effectiveChain = if (candidates.size > 1 && index < candidates.lastIndex) {
                chain.withConnectTimeout(3, TimeUnit.SECONDS)
            } else {
                chain.withConnectTimeout(5, TimeUnit.SECONDS)
            }

            try {
                val response = effectiveChain.proceed(candidateRequest)
                preferredHost = candidateHost
                return response
            } catch (e: ConnectException) {
                lastException = e
                if (preferredHost == candidateHost) preferredHost = null
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (preferredHost == candidateHost) preferredHost = null
            }
        }

        throw lastException ?: ConnectException(
            "No se pudo conectar al servidor API en ninguno de los hosts: ${candidates.joinToString(", ")}"
        )
    }

    private fun buildCandidateHosts(originalHost: String): List<String> {
        val list = mutableListOf<String>()
        preferredHost?.let { list.add(it) }

        if (isProbablyEmulator()) {
            list.add("10.0.2.2")
            list.add(originalHost)
        } else {
            list.add(originalHost)
            list.addAll(AppConfig.DEV_FALLBACK_HOSTS)
            list.add("10.0.2.2")
        }
        return list.distinct()
    }

    private fun isProbablyEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        return fp.startsWith("generic") ||
            fp.contains("emulator") ||
            model.contains("sdk_gphone") ||
            model.contains("emulator") ||
            product.contains("sdk")
    }
}
