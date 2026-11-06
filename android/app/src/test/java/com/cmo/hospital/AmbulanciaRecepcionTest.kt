package com.cmo.hospital

import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.MedicoDeTurno
import com.cmo.hospital.core.domain.model.ReporteAmbulancia
import com.cmo.hospital.core.domain.model.ServicioAmbulanciaDetalle
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.core.realtime.EventoTiempoReal
import com.cmo.hospital.core.realtime.ReverbClient
import com.cmo.hospital.features.recepcionista.ambulancia.data.repository.AmbulanciaGuardiaRepositoryImpl
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.*
import com.cmo.hospital.features.recepcionista.ambulancia.presentation.AmbulanciaGuardiaViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Ambulancia de recepción contra una API simulada con las respuestas de Laravel. */
@OptIn(ExperimentalCoroutinesApi::class)
class AmbulanciaRecepcionTest {

    private val llamadas = mutableListOf<String>()
    private val cuerpos = mutableMapOf<String, Map<*, *>>()
    private var enServicio = true

    private fun ok(data: String, code: Int = 200) = code to """{"success":true,"message":"ok","data":$data}"""

    private val servicio = """{"id_servicio":"u-srv","id_ambulancia":"u-amb","paciente_nombre":"Juan Pérez","direccion":"Col. Escalón #12",
        "descripcion":"Dolor torácico","prioridad":"CRITICA","estado":"EN_CURSO","hora_solicitud":"2026-11-06T16:40:00.000000Z",
        "hora_salida":"2026-11-06T16:41:00.000000Z","hora_cierre":null,"notas":null,
        "ultimo_reporte":{"id_reporte":"u-rep","estado_paciente":"DELICADO","observaciones":"PA 90/60","llegada_estimada":"2026-11-06T17:00:00.000000Z","fecha":"2026-11-06T16:50:00.000000Z"}}"""

    private fun responder(metodo: String, ruta: String): Pair<Int, String> = when ("$metodo $ruta") {
        "GET ambulancia" -> ok("""{"id_ambulancia":"u-amb","placa":"P-123456","estado":"${if (enServicio) "EN_SERVICIO" else "DISPONIBLE"}",
            "servicios":[${if (enServicio) servicio else ""}]}""")
        "GET ambulancia/servicios" -> ok("""{"data":[$servicio],"current_page":1,"last_page":1,"total":1}""")
        "GET publico/medicos-de-turno" -> ok("""[{"nombre":"Dra. Ana Martínez","cargo":"Medicina General","area":"Consulta externa"}]""")
        "POST ambulancia/servicios" ->
            if (enServicio) 409 to """{"success":false,"message":"La ambulancia no está disponible; estado actual: EN_SERVICIO.","errors":null}"""
            else ok(servicio, 201)
        "PATCH ambulancia/servicios/u-srv/finalizar" -> { enServicio = false; ok(servicio.replace("EN_CURSO", "FINALIZADO")) }
        "POST ambulancia/servicios/u-srv/reportes" -> ok("""{"id_reporte":"u-rep2","estado_paciente":"CRITICO","observaciones":null,"llegada_estimada":null,"fecha":null}""", 201)
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
                buffer.readUtf8().takeIf { it.isNotBlank() }?.let { cuerpos[clave] = Gson().fromJson(it, Map::class.java) }
            }
            val (code, json) = responder(req.method, clave.substringAfter(' '))
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val repo = AmbulanciaGuardiaRepositoryImpl(api, ReverbClient(urlBase = "ws://localhost/"))

    @Test
    fun testPanelLeeElServicioEnCursoYSuUltimoReporte() = runTest {
        val panel = CargarPanelAmbulanciaUseCase(repo)().getOrThrow()

        assertEquals("P-123456", panel.ambulancia.placa)
        val srv = panel.ambulancia.servicioActivo!!
        assertEquals("u-srv", srv.id)
        assertEquals("CRITICA", srv.prioridad)
        assertEquals("DELICADO", srv.ultimoReporte?.estadoPaciente)
        assertEquals(1, panel.bitacora.size)
        assertEquals("Dra. Ana Martínez", panel.medicosDeTurno.single().nombre)
    }

    @Test
    fun testDespachoValidaLocalYMuestraElConflictoDeLaApi() = runTest {
        val despachar = SolicitarDespachoAmbulanciaUseCase(repo)
        fun campo(r: Result<*>) = (r.exceptionOrNull() as ApiValidationException).campo

        assertEquals("direccion", campo(despachar(" ", "Juan", "Dolor", "ALTA")))
        assertEquals("paciente_nombre", campo(despachar("Calle 1", "", "Dolor", "ALTA")))
        assertEquals("descripcion", campo(despachar("Calle 1", "Juan", "", "ALTA")))
        assertEquals("prioridad", campo(despachar("Calle 1", "Juan", "Dolor", null)))
        assertFalse("POST ambulancia/servicios" in llamadas)

        val ocupada = despachar("Calle 1", "Juan", "Dolor", "ALTA").exceptionOrNull()
        assertEquals("La ambulancia no está disponible; estado actual: EN_SERVICIO.", ocupada?.message)

        enServicio = false
        assertTrue(despachar(" Calle 1 ", "Juan", "Dolor", "ALTA").isSuccess)
        assertEquals(
            mapOf("direccion" to "Calle 1", "paciente_nombre" to "Juan", "descripcion" to "Dolor", "prioridad" to "ALTA"),
            cuerpos["POST ambulancia/servicios"]
        )
    }

    @Test
    fun testReporteYFinalizarEnvianLoQuePideLaApi() = runTest {
        val reportar = EnviarReporteAmbulanciaUseCase(repo)
        fun campo(r: Result<*>) = (r.exceptionOrNull() as ApiValidationException).campo

        assertEquals("estado_paciente", campo(reportar("u-srv", null, null, "")))
        assertEquals("minutos_llegada", campo(reportar("u-srv", "CRITICO", null, "300")))
        assertTrue(reportar("u-srv", "CRITICO", "  ", "12").isSuccess)
        assertEquals(mapOf("estado_paciente" to "CRITICO", "minutos_llegada" to 12.0), cuerpos["POST ambulancia/servicios/u-srv/reportes"])

        assertTrue(FinalizarServicioAmbulanciaUseCase(repo)("u-srv", "  ").isSuccess)
        // Gson omite las notas nulas: el cuerpo llega vacío y la API las guarda como null.
        assertTrue("PATCH ambulancia/servicios/u-srv/finalizar" in llamadas)
        assertNull(CargarPanelAmbulanciaUseCase(repo)().getOrThrow().ambulancia.servicioActivo)
    }

    // ─── ViewModel: un evento en vivo recarga el panel ────────────────────────

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class RepoFalso : AmbulanciaGuardiaRepository {
        val eventos = Channel<EventoTiempoReal>(Channel.UNLIMITED)
        var cargas = 0
        var estado = "DISPONIBLE"
        override suspend fun ambulancia(): Result<AmbulanciaRecurso> {
            cargas++
            return Result.success(AmbulanciaRecurso(placa = "P-1", estado = estado))
        }
        override suspend fun bitacora() = Result.success(emptyList<ServicioAmbulanciaDetalle>())
        override suspend fun medicosDeTurno() = Result.success(emptyList<MedicoDeTurno>())
        override suspend fun despachar(direccion: String, paciente: String, descripcion: String, prioridad: String) =
            Result.failure<ServicioAmbulanciaDetalle>(UnsupportedOperationException())
        override suspend fun finalizar(idServicio: String, notas: String?) =
            Result.failure<ServicioAmbulanciaDetalle>(UnsupportedOperationException())
        override suspend fun reportar(idServicio: String, estadoPaciente: String, observaciones: String?, minutosLlegada: Int?) =
            Result.failure<ReporteAmbulancia>(UnsupportedOperationException())
        override fun cambiosEnVivo(): Flow<EventoTiempoReal> = eventos.receiveAsFlow()
    }

    @Test
    fun testEventoEnVivoRecargaElPanel() = runTest(dispatcher) {
        val falso = RepoFalso()
        val vm = AmbulanciaGuardiaViewModel(
            CargarPanelAmbulanciaUseCase(falso),
            SolicitarDespachoAmbulanciaUseCase(falso),
            FinalizarServicioAmbulanciaUseCase(falso),
            EnviarReporteAmbulanciaUseCase(falso),
            ObservarAmbulanciaEnVivoUseCase(falso)
        )
        advanceUntilIdle()
        assertEquals(1, falso.cargas)
        assertFalse(vm.estado.value.enVivo)

        falso.eventos.send(EventoTiempoReal.Conectado)
        advanceUntilIdle()
        assertTrue(vm.estado.value.enVivo)

        falso.estado = "EN_SERVICIO"
        falso.eventos.send(EventoTiempoReal.Mensaje("App\\Events\\AmbulanciaActualizada", """{"estado":"EN_SERVICIO"}"""))
        advanceUntilIdle()
        assertEquals(2, falso.cargas)
        assertEquals("EN_SERVICIO", vm.estado.value.panel?.ambulancia?.estado)
    }
}
