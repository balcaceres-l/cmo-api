package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.admision.data.repository.AdmisionRepositoryImpl
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.DarAltaUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.IngresarPacienteUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.TrasladarPacienteUseCase
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Camas e ingresos de recepción contra una API simulada con la forma de GET /habitaciones. */
class CamasRecepcionTest {

    private val llamadas = mutableListOf<String>()
    private val cuerpos = mutableMapOf<String, Map<*, *>>()

    /** Ocupación del servidor: código de cama → id de ingreso. */
    private val ocupadas = mutableMapOf("VIP-301" to "u-ing-1")
    private var camaOcupadaPorOtro = false

    private fun ok(data: String, code: Int = 200) = code to """{"success":true,"message":"ok","data":$data}"""

    private fun camaJson(codigo: String): String {
        val ingreso = ocupadas[codigo]
        val paciente = ingreso?.let {
            """{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001"}"""
        } ?: "null"
        return """{"id_cama":"id-$codigo","codigo":"$codigo","estado":"${if (ingreso != null) "OCUPADA" else "DISPONIBLE"}",
            "id_ingreso":${ingreso?.let { "\"$it\"" } ?: "null"},"paciente":$paciente}"""
    }

    /** Agrupado por planta como el controlador: [[planta 2], [planta 3]]. */
    private fun habitaciones(): String {
        val semis = (201..206).joinToString(",") { n ->
            """{"codigo":"HAB-$n","tipo":"SEMIPRIVADA","capacidad":2,"camas":[${camaJson("HAB-$n-A")},${camaJson("HAB-$n-B")}]}"""
        }
        val vips = (301..305).joinToString(",") { n ->
            """{"codigo":"VIP-$n","tipo":"VIP","capacidad":1,"camas":[${camaJson("VIP-$n")}]}"""
        }
        return "[[$semis],[$vips]]"
    }

    private fun responder(metodo: String, ruta: String): Pair<Int, String> = when ("$metodo $ruta") {
        "GET habitaciones" -> ok(habitaciones())
        "POST ingresos" ->
            if (camaOcupadaPorOtro) {
                409 to """{"success":false,"message":"La cama seleccionada está ocupada.","errors":null}"""
            } else {
                val codigo = (cuerpos["POST ingresos"]?.get("id_cama") as String).removePrefix("id-")
                ocupadas[codigo] = "u-ing-2"
                ok("""{"id_ingreso":"u-ing-2"}""", 201)
            }
        "PATCH ingresos/u-ing-1/alta" -> { ocupadas.remove("VIP-301"); ok("""{"id_ingreso":"u-ing-1"}""") }
        "PATCH ingresos/u-ing-1/trasladar" -> {
            val destino = (cuerpos["PATCH ingresos/u-ing-1/trasladar"]?.get("id_cama") as String).removePrefix("id-")
            ocupadas.remove("VIP-301")
            ocupadas[destino] = "u-ing-3"
            ok("""{"id_ingreso":"u-ing-3"}""")
        }
        else -> 404 to """{"success":false,"message":"no simulado","errors":null}"""
    }

    private val api: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val clave = "${req.method} ${req.url.encodedPath.removePrefix("/api/v1/")}"
            llamadas += clave
            req.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8().takeIf { it.isNotBlank() }?.let {
                    cuerpos[clave] = Gson().fromJson(it, Map::class.java)
                }
            }
            val (code, json) = responder(req.method, clave.substringAfter(' '))
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val ds = HospitalClinicalDataSource(api)
    private val repo = AdmisionRepositoryImpl(ds)

    private fun cama(codigo: String, lista: List<Cama> = ds.camasState.value) = lista.single { it.codigo == codigo }

    @Test
    fun testMapaDeCamasVieneDeLaApiSinDatosDeEjemplo() = runTest {
        assertTrue(ds.camasState.value.isEmpty())

        repo.refrescarCamas().getOrThrow()
        val cap = repo.obtenerCapacidadActual()

        assertEquals(17, cap.totalCamas)
        assertEquals(1, cap.ocupadas)
        assertEquals(5, cap.vipCamas.size)
        assertEquals(6, cap.salasSemiprivadas.size)
        val vip = cama("VIP-301")
        assertEquals("id-VIP-301", vip.id)
        assertEquals("u-ing-1", vip.idIngreso)
        assertEquals("Ana López", vip.paciente?.nombreCompleto)
        assertNull(cama("HAB-201-A").idIngreso)
    }

    @Test
    fun testIngresoEnviaTipoSegunLaCamaYRefrescaElMapa() = runTest {
        repo.refrescarCamas()
        val ingresar = IngresarPacienteUseCase(repo)

        assertTrue(ingresar("u-p2", cama("HAB-203-B"), "u-med", "  Neumonía ").isSuccess)

        assertEquals(
            mapOf("id_paciente" to "u-p2", "tipo" to "SEMIPRIVADA", "id_cama" to "id-HAB-203-B", "id_medico" to "u-med", "motivo" to "Neumonía"),
            cuerpos["POST ingresos"]
        )
        assertFalse(cama("HAB-203-B").estaDisponible)
        assertEquals(2, llamadas.count { it == "GET habitaciones" })

        ingresar("u-p3", cama("VIP-304"), "u-med", "Control")
        assertEquals("VIP", cuerpos["POST ingresos"]?.get("tipo"))
    }

    @Test
    fun testValidacionLocalDelIngreso() = runTest {
        repo.refrescarCamas()
        val ingresar = IngresarPacienteUseCase(repo)
        fun campo(r: Result<Unit>) = (r.exceptionOrNull() as ApiValidationException).campo

        assertEquals("id_paciente", campo(ingresar(null, cama("VIP-302"), "u-med", "x")))
        assertEquals("id_medico", campo(ingresar("u-p2", cama("VIP-302"), null, "x")))
        assertEquals("motivo", campo(ingresar("u-p2", cama("VIP-302"), "u-med", " ")))
        assertEquals("id_cama", campo(ingresar("u-p2", null, "u-med", "x")))
        assertEquals("id_cama", campo(ingresar("u-p2", cama("VIP-301"), "u-med", "x")))
        assertFalse("POST ingresos" in llamadas)
    }

    @Test
    fun testConflictoDeLaApiLlegaComoError() = runTest {
        repo.refrescarCamas()
        camaOcupadaPorOtro = true

        val err = IngresarPacienteUseCase(repo)("u-p2", cama("VIP-302"), "u-med", "x").exceptionOrNull()

        assertEquals("La cama seleccionada está ocupada.", err?.message)
    }

    @Test
    fun testAltaYTrasladoUsanElIngresoDeLaCama() = runTest {
        repo.refrescarCamas()
        val trasladar = TrasladarPacienteUseCase(repo)

        assertTrue(trasladar(cama("VIP-301"), cama("VIP-301")).isFailure)
        assertTrue(trasladar(cama("VIP-301"), cama("HAB-205-A")).isSuccess)
        assertEquals(mapOf("id_cama" to "id-HAB-205-A"), cuerpos["PATCH ingresos/u-ing-1/trasladar"])
        assertTrue(cama("VIP-301").estaDisponible)
        assertEquals("u-ing-3", cama("HAB-205-A").idIngreso)

        ocupadas["VIP-301"] = "u-ing-1"
        repo.refrescarCamas()
        assertTrue(DarAltaUseCase(repo)(cama("VIP-301")).isSuccess)
        assertTrue("PATCH ingresos/u-ing-1/alta" in llamadas)
        assertTrue(cama("VIP-301").estaDisponible)
        assertTrue(DarAltaUseCase(repo)(cama("VIP-302")).isFailure)
    }

    @Test
    fun testParseaHabitacionesCuandoBackendDevuelveObjetoPorPlanta() {
        val jsonObjetoPorPlanta = """
            {
              "2": [
                {"codigo":"HAB-201","tipo":"SEMIPRIVADA","capacidad":2,"camas":[${camaJson("HAB-201-A")},${camaJson("HAB-201-B")}]}
              ],
              "3": [
                {"codigo":"VIP-301","tipo":"VIP","capacidad":1,"camas":[${camaJson("VIP-301")}]}
              ]
            }
        """.trimIndent()
        val elem = Gson().fromJson(jsonObjetoPorPlanta, com.google.gson.JsonElement::class.java)
        val parseadas = com.cmo.hospital.core.data.mapper.parsearHabitacionesJson(elem)
        assertEquals(2, parseadas.size)
        assertEquals("HAB-201", parseadas[0].codigo)
        assertEquals("VIP-301", parseadas[1].codigo)
    }
}
