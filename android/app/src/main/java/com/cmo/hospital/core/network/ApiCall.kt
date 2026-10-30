package com.cmo.hospital.core.network

import com.cmo.hospital.core.network.dto.ApiResponse
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Dato rechazado por la validación local o por la API (422). [campo] usa el
 * nombre del campo en la API (p. ej. "dui") para marcarlo en un formulario.
 */
class ApiValidationException(val campo: String?, message: String) : IllegalArgumentException(message)

private val gson = Gson()

/**
 * Ejecuta una llamada a la API y la convierte en [Result]. Cualquier fallo
 * (red, 4xx/5xx, respuesta inesperada) llega como error legible, sin cerrar la app.
 */
suspend fun <T> llamarApi(request: suspend () -> Response<ApiResponse<T>>): Result<T> {
    return try {
        val response = request()
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            Result.success(body.data)
        } else {
            Result.failure(errorDe(response))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        Result.failure(IllegalStateException("No se pudo conectar con el servidor. Verifique la red y que la API esté encendida."))
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Error inesperado: ${e.message ?: e::class.java.simpleName}"))
    }
}

/** Para endpoints que responden `data: null` (p. ej. eliminar). */
suspend fun llamarApiSinDatos(request: suspend () -> Response<ApiResponse<Unit>>): Result<Unit> {
    return try {
        val response = request()
        if (response.isSuccessful && response.body()?.success == true) {
            Result.success(Unit)
        } else {
            Result.failure(errorDe(response))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        Result.failure(IllegalStateException("No se pudo conectar con el servidor. Verifique la red y que la API esté encendida."))
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Error inesperado: ${e.message ?: e::class.java.simpleName}"))
    }
}

/** Prioriza el primer error de validación (422) porque dice qué campo corregir. */
private fun errorDe(response: Response<*>): Exception {
    val error = try {
        response.errorBody()?.charStream()?.use { gson.fromJson(it, ApiResponse::class.java) }
    } catch (_: Exception) {
        null
    }
    error?.errors?.entries?.firstOrNull()?.let { (campo, mensajes) ->
        mensajes.firstOrNull()?.let { return ApiValidationException(campo, it) }
    }
    return IllegalStateException(
        error?.message ?: when (response.code()) {
            401 -> "La sesión expiró. Vuelva a iniciar sesión."
            403 -> "No tiene permisos para esta acción."
            404 -> "El registro solicitado ya no existe."
            else -> "Error del servidor (${response.code()})."
        }
    )
}
