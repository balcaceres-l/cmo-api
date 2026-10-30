package com.cmo.hospital

import com.cmo.hospital.core.network.ApiService
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit valida cada método al invocarlo: un @Body con comodines
 * (Map<String, ?>) falla en tiempo de ejecución, no al compilar.
 */
class ApiServiceContratoTest {

    private var ultimoCuerpo: String? = null

    private val api: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/api/v1/")
        .client(
            OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
                val buffer = okio.Buffer()
                chain.request().body?.writeTo(buffer)
                ultimoCuerpo = buffer.readUtf8()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(201)
                    .message("Created")
                    .body("""{"success":true,"message":"ok","data":{"id_paciente":"p1","nombre_completo":"Ana"}}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }).build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    @Test
    fun testBodyConMapSeEnviaComoJson() = runTest {
        val res = api.registrarPaciente(mapOf("nombre_completo" to "Ana", "dui" to null, "sexo" to "F"))
        assertTrue(res.isSuccessful)
        assertEquals("p1", res.body()?.data?.id)
        assertTrue(ultimoCuerpo!!.contains("\"nombre_completo\":\"Ana\""))

        // Los demás métodos con Map también deben poder invocarse.
        api.actualizarPaciente("p1", mapOf("telefono" to "7777-0000"))
        api.agendarCita(mapOf("fecha" to "2026-11-02"))
        api.despacharAmbulancia(mapOf("prioridad" to "ALTA"))
    }
}
