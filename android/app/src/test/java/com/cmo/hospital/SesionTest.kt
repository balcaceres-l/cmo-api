package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.network.AuthInterceptor
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SesionTest {

    private var token: String? = "abc"
    private var expiraciones = 0
    private var authRecibido: String? = null

    private fun cliente(codigo: Int) = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor({ token }, { token = null; expiraciones++ }))
        .addInterceptor(Interceptor { chain ->
            authRecibido = chain.request().header("Authorization")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(codigo)
                .message("x")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        })
        .build()

    private fun pedir(codigo: Int, ruta: String) {
        cliente(codigo).newCall(Request.Builder().url("http://api.test/api/v1/$ruta").build()).execute().close()
    }

    @Test
    fun testEnviaElTokenBearer() {
        pedir(200, "citas")
        assertEquals("Bearer abc", authRecibido)
        assertEquals(0, expiraciones)
    }

    @Test
    fun test401ConTokenCierraLaSesion() {
        pedir(401, "citas")
        assertEquals(1, expiraciones)
        assertNull(token)
    }

    @Test
    fun test401EnLoginNoEsSesionVencida() {
        pedir(401, "auth/login")
        pedir(401, "auth/logout")
        assertEquals(0, expiraciones)
    }

    @Test
    fun test401SinTokenNoAvisa() {
        token = null
        pedir(401, "citas")
        assertEquals(0, expiraciones)
    }

    @Test
    fun test403NoCierraLaSesion() {
        pedir(403, "pacientes/1/expediente")
        assertEquals(0, expiraciones)
    }

    @Test
    fun testLimpiarBorraLosDatosDelUsuarioAnterior() {
        val ds = HospitalClinicalDataSource()
        ds.solicitarAperturaExpediente("EXP-1")
        ds.limpiar()
        assertTrue(ds.citasState.value.isEmpty())
        assertTrue(ds.camasState.value.isEmpty())
        assertTrue(ds.expedientes.value.isEmpty())
        assertNull(ds.turnoEnBox.value)
        assertNull(ds.expedienteSolicitado.value)
    }
}
