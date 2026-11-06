package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.EstadoCita
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.citas.data.repository.CitaRepositoryImpl
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.AgendarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.BuscarPacientesParaCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.CancelarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.ValidarProgramacionCita
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
import java.time.LocalDate

/** Agenda de recepción contra una API simulada con las respuestas del backend Laravel. */
class CitasRecepcionTest {

    private val llamadas = mutableListOf<String>()
    private val cuerpos = mutableMapOf<String, Map<*, *>>()
    private val consultas = mutableMapOf<String, String>()

    private val hoy = LocalDate.now().toString()
    private val manana = LocalDate.now().plusDays(1).toString()

    /** Estado del servidor: cambia con cada acción, como la base real. */
    private val estados = mutableMapOf("u-sol" to "SOLICITADA", "u-age" to "AGENDADA")
    private var citaNueva = false
    private var horarioOcupado = false

    private fun ok(data: String, code: Int = 200) = code to """{"success":true,"message":"ok","data":$data}"""

    private val paciente = """{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001","dui":"01234567-8","sexo":"F"}"""
    private val medico = """{"id":"u-med","nombre_completo":"Dra. Ana Martínez","usuario":"medico.general","email":null,"rol":"MEDICO","cargo":"Medicina General","telefono":null,"estado":"ACTIVO"}"""
    private val alergista = """{"id":"u-ale","nombre_completo":"Dr. Luis Pérez","usuario":"medico.alergista","email":null,"rol":"MEDICO","cargo":"Alergología","telefono":null,"estado":"ACTIVO"}"""

    private fun citas(): String {
        // La solicitud del paciente llega sin fecha, hora, médico ni consultorio.
        val solicitud = """{"id_cita":"u-sol","fecha":null,"hora_inicio":null,"hora_fin":null,"estado":"${estados["u-sol"]}",
            "motivo":"Dolor de garganta","id_medico":null,"paciente":$paciente,"medico":null,"consultorio":null,"consulta":null}"""
        val agendada = """{"id_cita":"u-age","fecha":"${hoy}T00:00:00.000000Z","hora_inicio":"09:00:00","hora_fin":"09:30:00",
            "estado":"${estados["u-age"]}","motivo":"Control","id_medico":"u-med","paciente":$paciente,"medico":$medico,
            "consultorio":{"id_consultorio":"u-c1","codigo":"C-01","nombre":"Consultorio 1"},"consulta":null}"""
        val nueva = """{"id_cita":"u-new","fecha":"$manana","hora_inicio":"10:00:00","hora_fin":"10:30:00","estado":"AGENDADA",
            "motivo":"Chequeo","id_medico":"u-med","paciente":$paciente,"medico":$medico,"consultorio":null,"consulta":null}"""
        val lista = listOfNotNull(solicitud, agendada, nueva.takeIf { citaNueva })
        return """{"data":[${lista.joinToString(",")}],"current_page":1,"last_page":1,"total":${lista.size}}"""
    }

    /** Respuesta de PATCH/POST: la cita sin relaciones, como devuelve el controlador. */
    private fun citaPlana(id: String, estado: String) =
        """{"id_cita":"$id","fecha":"$manana","hora_inicio":"10:00","hora_fin":"10:30","estado":"$estado","motivo":"x"}"""

    private fun responder(metodo: String, ruta: String): Pair<Int, String> = when ("$metodo $ruta") {
        "GET citas" -> ok(citas())
        "GET medicos" -> ok("""{"data":[$medico,$alergista],"current_page":1,"last_page":1,"total":2}""")
        "GET consultorios" -> ok("""[{"id_consultorio":"u-c1","codigo":"C-01","nombre":"Consultorio 1","planta":"1"}]""")
        "GET agenda/disponibilidad" -> ok("""[{"hora_inicio":"10:00","hora_fin":"10:30"},{"hora_inicio":"10:30","hora_fin":"11:00"}]""")
        "GET pacientes" -> ok("""{"data":[$paciente],"current_page":1,"last_page":1,"total":1}""")
        "POST citas" ->
            if (horarioOcupado) {
                422 to """{"success":false,"message":"Datos inválidos","errors":{"hora_inicio":["El médico ya tiene una cita en ese horario."]}}"""
            } else {
                citaNueva = true
                ok(citaPlana("u-new", "AGENDADA"), 201)
            }
        "PATCH citas/u-sol/confirmar" -> { estados["u-sol"] = "AGENDADA"; ok(citaPlana("u-sol", "AGENDADA")) }
        "PATCH citas/u-age/reprogramar" -> ok(citaPlana("u-age", "AGENDADA"))
        "PATCH citas/u-age/llegada" -> { estados["u-age"] = "EN_ESPERA"; ok(citaPlana("u-age", "EN_ESPERA")) }
        "PATCH citas/u-sol/llegada" ->
            409 to """{"success":false,"message":"No se puede registrar la llegada de una cita en estado SOLICITADA.","errors":null}"""
        "PATCH citas/u-age/cancelar" -> { estados["u-age"] = "CANCELADA"; ok(citaPlana("u-age", "CANCELADA")) }
        else -> 404 to """{"success":false,"message":"no simulado","errors":null}"""
    }

    private val api: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val ruta = req.url.encodedPath.removePrefix("/api/v1/")
            val clave = "${req.method} $ruta"
            llamadas += clave
            req.url.query?.let { consultas[clave] = it }
            req.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                // PATCH de llegada va sin cuerpo: Gson devolvería null.
                buffer.readUtf8().takeIf { it.isNotBlank() }?.let {
                    cuerpos[clave] = Gson().fromJson(it, Map::class.java)
                }
            }
            val (code, json) = responder(req.method, ruta)
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val ds = HospitalClinicalDataSource(api)
    private val repo = CitaRepositoryImpl(ds)

    private fun cita(id: String): Cita = ds.citasState.value.single { it.id == id }

    @Test
    fun testListaCitasAceptaSolicitudesSinHorarioYNormalizaFechas() = runTest {
        ds.syncCitasFromRemote()

        assertEquals("per_page=100", consultas["GET citas"])
        assertEquals(listOf("u-sol", "u-age"), ds.citasState.value.map { it.id })
        val solicitud = cita("u-sol")
        assertEquals("", solicitud.horaInicio)
        assertNull(solicitud.horaFin)
        assertEquals("Médico por asignar", solicitud.medicoNombre)
        val agendada = cita("u-age")
        assertEquals(hoy, agendada.fecha)
        assertEquals("09:00", agendada.horaInicio)
        assertEquals("u-med", agendada.idMedico)
        assertEquals("Medicina General", agendada.especialidad)
        assertEquals("C-01", agendada.consultorio?.codigo)
    }

    @Test
    fun testCatalogosDisponibilidadYBusquedaUsanLaApi() = runTest {
        assertEquals(listOf("Dra. Ana Martínez", "Dr. Luis Pérez"), repo.medicos().getOrThrow().map { it.nombreCompleto })
        assertEquals("u-c1", repo.consultorios().getOrThrow().single().id)

        val bloques = repo.disponibilidad("u-med", manana).getOrThrow()
        assertEquals(listOf("10:00", "10:30"), bloques.map { it.horaInicio })
        assertEquals("id_medico=u-med&fecha=$manana", consultas["GET agenda/disponibilidad"])

        val buscar = BuscarPacientesParaCitaUseCase(repo)
        assertTrue(buscar("a").getOrThrow().isEmpty())
        assertFalse("GET pacientes" in llamadas)
        assertEquals("u-p1", buscar("01234567").getOrThrow().single().id)
        assertEquals("q=01234567&per_page=8", consultas["GET pacientes"])
    }

    @Test
    fun testAgendarEnviaLoQuePideLaApiYRecargaLaLista() = runTest {
        ds.syncCitasFromRemote()
        val agendar = AgendarCitaUseCase(repo)

        val res = agendar("u-p1", ProgramacionCita("u-med", manana, "10:00"), "  Chequeo ")

        assertTrue(res.isSuccess)
        assertEquals(
            mapOf("id_medico" to "u-med", "fecha" to manana, "hora_inicio" to "10:00", "id_paciente" to "u-p1", "motivo" to "Chequeo"),
            cuerpos["POST citas"]
        )
        // Tras crear, la lista se vuelve a pedir y trae paciente y médico resueltos.
        assertEquals("Ana López", res.getOrThrow().paciente?.nombreCompleto)
        assertTrue(ds.citasState.value.any { it.id == "u-new" })
        assertFalse(ds.citasState.value.any { it.id.startsWith("c-") })
    }

    @Test
    fun testAgendarConConsultorioLoEnviaYSinElNo() = runTest {
        AgendarCitaUseCase(repo)("u-p1", ProgramacionCita("u-med", manana, "10:00", "u-c1"), "Chequeo")
        assertEquals("u-c1", cuerpos["POST citas"]?.get("id_consultorio"))
    }

    @Test
    fun testHorarioOcupadoLlegaComoErrorDelCampo() = runTest {
        horarioOcupado = true
        val err = AgendarCitaUseCase(repo)("u-p1", ProgramacionCita("u-med", manana, "10:00"), "Chequeo")
            .exceptionOrNull() as ApiValidationException

        assertEquals("hora_inicio", err.campo)
        assertEquals("El médico ya tiene una cita en ese horario.", err.message)
    }

    @Test
    fun testValidacionLocalNoLlamaALaApi() = runTest {
        val validador = ValidarProgramacionCita { LocalDate.parse("2026-11-06") }
        val agendar = AgendarCitaUseCase(repo, validador)

        fun campo(r: Result<Cita>) = (r.exceptionOrNull() as ApiValidationException).campo
        assertEquals("id_paciente", campo(agendar(null, ProgramacionCita("u-med", "2026-11-07", "10:00"), "x")))
        assertEquals("id_medico", campo(agendar("u-p1", ProgramacionCita("", "2026-11-07", "10:00"), "x")))
        assertEquals("fecha", campo(agendar("u-p1", ProgramacionCita("u-med", "2026-11-05", "10:00"), "x")))
        assertEquals("fecha", campo(agendar("u-p1", ProgramacionCita("u-med", "07/11/2026", "10:00"), "x")))
        assertEquals("hora_inicio", campo(agendar("u-p1", ProgramacionCita("u-med", "2026-11-07", ""), "x")))
        assertEquals("motivo", campo(agendar("u-p1", ProgramacionCita("u-med", "2026-11-07", "10:00"), " ")))
        assertFalse("POST citas" in llamadas)
    }

    @Test
    fun testConfirmarReprogramarLlegadaYCancelar() = runTest {
        ds.syncCitasFromRemote()

        assertTrue(repo.confirmarCita("u-sol", ProgramacionCita("u-med", manana, "10:00")).isSuccess)
        assertEquals(mapOf("id_medico" to "u-med", "fecha" to manana, "hora_inicio" to "10:00"), cuerpos["PATCH citas/u-sol/confirmar"])
        assertEquals("AGENDADA", cita("u-sol").estado)

        assertTrue(repo.reprogramarCita("u-age", ProgramacionCita("u-med", manana, "10:30", "u-c1")).isSuccess)
        assertEquals("u-c1", cuerpos["PATCH citas/u-age/reprogramar"]?.get("id_consultorio"))

        assertTrue(repo.marcarLlegada("u-age").isSuccess)
        assertEquals("EN_ESPERA", cita("u-age").estado)

        val cancelar = CancelarCitaUseCase(repo)
        assertEquals("motivo_cancelacion", (cancelar("u-age", " ").exceptionOrNull() as ApiValidationException).campo)
        assertTrue(cancelar("u-age", "Paciente avisó").isSuccess)
        assertEquals(mapOf("motivo_cancelacion" to "Paciente avisó"), cuerpos["PATCH citas/u-age/cancelar"])
        assertEquals("CANCELADA", cita("u-age").estado)
    }

    @Test
    fun testErrorDeLaApiNoCambiaLaLista() = runTest {
        ds.syncCitasFromRemote()
        val antes = ds.citasState.value

        val err = repo.marcarLlegada("u-sol").exceptionOrNull()

        assertEquals("No se puede registrar la llegada de una cita en estado SOLICITADA.", err?.message)
        assertEquals(antes, ds.citasState.value)
    }

    @Test
    fun testMostradorOrdenaSolicitudesYEsperaPrimero() {
        fun c(id: String, estado: String, fecha: String, hora: String) =
            Cita(id = id, fecha = fecha, horaInicio = hora, horaFin = null, estado = estado)
        val lista = listOf(
            c("vieja", "ATENDIDA", "2026-11-01", "08:00"),
            c("tarde", "AGENDADA", "2026-11-08", "09:00"),
            c("pronto", "AGENDADA", "2026-11-07", "11:00"),
            c("espera", "EN_ESPERA", "2026-11-06", "10:00"),
            c("sol", "SOLICITADA", "", ""),
            c("reciente", "CANCELADA", "2026-11-05", "08:00")
        )
        assertEquals(
            listOf("sol", "espera", "pronto", "tarde", "reciente", "vieja"),
            EstadoCita.ordenarParaMostrador(lista).map { it.id }
        )
    }
}
