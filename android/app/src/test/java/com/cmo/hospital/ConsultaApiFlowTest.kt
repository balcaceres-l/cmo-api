package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.network.ApiService
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate

/** Flujo de consulta del médico contra una API simulada con las respuestas reales del backend. */
class ConsultaApiFlowTest {

    private val llamadas = mutableListOf<String>()
    private val cuerpos = mutableMapOf<String, String>()
    private var rechazarApertura = false
    private var citaEnAtencion = false

    private val hoy = LocalDate.now().toString()
    private val inicio = Instant.now().minusSeconds(300).toString()

    private fun ok(data: String, code: Int = 200) = code to """{"success":true,"message":"ok","data":$data}"""

    private fun cita(): String {
        val estado = if (citaEnAtencion) "EN_ATENCION" else "EN_ESPERA"
        val consulta = if (citaEnAtencion) """{"id_consulta":"u-cons-1","id_cita":"u-cita-1","fecha_hora_fin":null}""" else "null"
        return """{"id_cita":"u-cita-1","fecha":"$hoy","hora_inicio":"09:00:00","hora_fin":"09:30:00","estado":"$estado","motivo":"Fiebre",
            "paciente":{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001","dui":"01234567-8","sexo":"F"},
            "medico":{"id":"u-med","nombre_completo":"Dra. Ana Martínez","usuario":"medico.general","rol":"MEDICO","cargo":"Medicina general","estado":"ACTIVO"},
            "consultorio":null,"consulta":$consulta}"""
    }

    private fun consulta(fin: String? = null, diagnostico: String? = null, recetas: String = "[]") =
        """{"id_consulta":"u-cons-1","id_cita":"u-cita-1","motivo":"Fiebre","diagnostico":${diagnostico?.let { "\"$it\"" } ?: "null"},
            "notas":${if (diagnostico != null) "\"Dolor de cabeza\"" else "null"},"fecha_hora_inicio":"$inicio",
            "fecha_hora_fin":${fin?.let { "\"$it\"" } ?: "null"},"recetas":$recetas}"""

    private var ordenesCreadas = 0

    private fun orden(id: String, examen: String, estado: String, conRelaciones: Boolean = false): String {
        val relaciones = if (conRelaciones) {
            ""","paciente":{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001","dui":null,"sexo":"F"},"examen":{"id_examen":"$examen","nombre":"Glucosa en ayunas"}"""
        } else ""
        return """{"id_orden":"$id","id_consulta":"u-cons-1","id_paciente":"u-p1","id_examen":"$examen","estado":"$estado",
            "observaciones":"[STAT] Ayuno de 8 h","fecha_solicitud":"$inicio","fecha_resultado":null$relaciones}"""
    }

    private val receta = """{"id_receta":"u-rec-1","medicamento":"Ibuprofeno","dosis":"400 mg","frecuencia":"Cada 8 h","duracion":"5 días","indicaciones":null}"""

    private fun responder(metodo: String, ruta: String): Pair<Int, String> = when ("$metodo $ruta") {
        "GET citas" -> ok("""{"data":[${cita()}],"current_page":1,"last_page":1,"total":1}""")
        "POST citas/u-cita-1/consulta" ->
            if (rechazarApertura) 409 to """{"success":false,"message":"La consulta solo se inicia el día de la cita.","errors":null}"""
            else ok(consulta(), 201)
        "GET consultas/u-cons-1" -> ok(consulta(diagnostico = "R51 — Cefalea", recetas = "[$receta]"))
        "PUT consultas/u-cons-1" -> ok(consulta())
        "POST consultas/u-cons-1/recetas" -> ok(receta, 201)
        "DELETE recetas/u-rec-1" -> ok("null")
        "PATCH consultas/u-cons-1/finalizar" -> ok(consulta(fin = Instant.now().toString()))
        "GET examenes" -> ok("""{"data":[{"id_examen":"u-ex-1","nombre":"Hemograma completo"},{"id_examen":"u-ex-2","nombre":"Glucosa en ayunas"}],"current_page":1,"last_page":1,"total":2}""")
        "POST consultas/u-cons-1/ordenes-laboratorio" -> {
            ordenesCreadas++
            ok(orden("u-ord-$ordenesCreadas", if (ordenesCreadas == 1) "u-ex-1" else "u-ex-2", "PENDIENTE"), 201)
        }
        "PATCH ordenes-laboratorio/u-ord-1/cancelar" -> ok(orden("u-ord-1", "u-ex-1", "CANCELADA"))
        "GET ordenes-laboratorio" -> ok("""{"data":[${orden("u-ord-2", "u-ex-2", "EN_PROCESO", conRelaciones = true)}],"current_page":1,"last_page":1,"total":1}""")
        else -> 404 to """{"success":false,"message":"no simulado","errors":null}"""
    }

    private val api: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val ruta = req.url.encodedPath.removePrefix("/api/v1/")
            llamadas += "${req.method} $ruta"
            req.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                cuerpos["${req.method} $ruta"] = buffer.readUtf8()
            }
            val (code, json) = responder(req.method, ruta)
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    @Test
    fun testAbrirGuardarRecetarYFinalizarEnLaApi() = runTest {
        val ds = HospitalClinicalDataSource(api)
        ds.syncCitasFromRemote()
        assertEquals(listOf("u-cita-1"), ds.citasState.value.map { it.id })
        assertTrue(ds.consultaActiva.value.finalizada)

        assertTrue(ds.iniciarORetomarConsultaDoctor("u-cita-1").isSuccess)
        val abierta = ds.consultaActiva.value
        assertEquals("u-cons-1", abierta.id)
        assertEquals("Ana López", abierta.paciente.nombreCompleto)
        assertFalse(abierta.finalizada)
        assertTrue(abierta.segundosTranscurridos >= 290)
        assertEquals("EN_ATENCION", ds.citasState.value.single().estado)

        ds.agregarDiagnosticoCie10("R51", "Cefalea")
        ds.actualizarAnamnesis("Dolor de cabeza")
        assertTrue(ds.guardarConsultaRemota().isSuccess)
        assertTrue(cuerpos["PUT consultas/u-cons-1"]!!.contains("R51 — Cefalea"))
        assertTrue(cuerpos["PUT consultas/u-cons-1"]!!.contains("Dolor de cabeza"))

        val rec = ds.agregarReceta(com.cmo.hospital.core.domain.model.RecetaItem(medicamento = "Ibuprofeno", dosis = "400 mg", frecuencia = "Cada 8 h", duracion = "5 días"))
        assertEquals("u-rec-1", rec.getOrThrow().id)
        assertEquals(1, ds.consultaActiva.value.recetas.size)
        assertTrue(ds.eliminarReceta("u-rec-1").isSuccess)
        assertTrue(ds.consultaActiva.value.recetas.isEmpty())

        assertTrue(ds.finalizarConsulta().isSuccess)
        assertTrue("PATCH consultas/u-cons-1/finalizar" in llamadas)
        assertTrue(ds.consultaActiva.value.finalizada)
        assertEquals("ATENDIDA", ds.citasState.value.single().estado)
        // Ya cerrada, no admite más recetas.
        assertTrue(ds.agregarReceta(com.cmo.hospital.core.domain.model.RecetaItem(medicamento = "X", dosis = "1", frecuencia = "1", duracion = "1")).isFailure)
    }

    @Test
    fun testSiLaApiRechazaLaAperturaSeMuestraElMotivo() = runTest {
        rechazarApertura = true
        val ds = HospitalClinicalDataSource(api)
        ds.syncCitasFromRemote()

        val res = ds.iniciarORetomarConsultaDoctor("u-cita-1")
        assertEquals("La consulta solo se inicia el día de la cita.", res.exceptionOrNull()?.message)
        assertTrue(ds.consultaActiva.value.finalizada)
        assertEquals("EN_ESPERA", ds.citasState.value.single().estado)
    }

    @Test
    fun testAlSincronizarSeRetomaLaConsultaAbiertaConLoGuardado() = runTest {
        citaEnAtencion = true
        val ds = HospitalClinicalDataSource(api)
        ds.syncCitasFromRemote()

        val retomada = ds.consultaActiva.value
        assertEquals("u-cons-1", retomada.id)
        assertFalse(retomada.finalizada)
        assertEquals("R51", retomada.diagnosticosCie10.single().codigo)
        assertEquals("Dolor de cabeza", retomada.anamnesis)
        assertEquals("u-rec-1", retomada.recetas.single().id)
        assertFalse("POST citas/u-cita-1/consulta" in llamadas)
    }

    @Test
    fun testOrdenesDeLaboratorioDesdeLaConsultaAbierta() = runTest {
        val ds = HospitalClinicalDataSource(api)
        ds.syncCitasFromRemote()
        ds.iniciarORetomarConsultaDoctor("u-cita-1")
        assertTrue(ds.syncExamenesCatalogo().isSuccess)

        val creadas = ds.emitirOrdenesLaboratorio(
            examenes = listOf("Hemograma completo", "Glucosa en ayunas"),
            prioridad = "STAT",
            observaciones = "Ayuno de 8 h",
            paciente = ds.consultaActiva.value.paciente
        ).getOrThrow()
        assertEquals(listOf("u-ord-1", "u-ord-2"), creadas.map { it.id })
        assertTrue(cuerpos["POST consultas/u-cons-1/ordenes-laboratorio"]!!.contains("\"id_examen\":\"u-ex-2\""))
        assertTrue(cuerpos["POST consultas/u-cons-1/ordenes-laboratorio"]!!.contains("[STAT] Ayuno de 8 h"))
        assertTrue(creadas.all { it.estado == "Pendiente" && it.prioridad == "STAT" && it.observaciones == "Ayuno de 8 h" })
        assertEquals(2, ds.consultaActiva.value.ordenesLaboratorio.size)

        // Un examen que no está en el catálogo no llega a la API.
        assertTrue(ds.emitirOrdenesLaboratorio(listOf("Otro"), "RUTINA", "", null).isFailure)
        assertEquals(2, ordenesCreadas)

        assertEquals("Cancelada", ds.cancelarOrdenLaboratorio("u-ord-1").getOrThrow().estado)
        assertEquals("Cancelada", ds.consultaActiva.value.ordenesLaboratorio.first { it.id == "u-ord-1" }.estado)

        // El laboratorio avanzó la otra orden: al refrescar se ve En Proceso y ya no se puede cancelar.
        val remotas = ds.syncOrdenesLabFromRemote().getOrThrow()
        assertEquals("En Proceso", remotas.single().estado)
        assertEquals("Glucosa en ayunas", remotas.single().examenNombre)
        assertFalse(remotas.single().cancelable)
    }
}
