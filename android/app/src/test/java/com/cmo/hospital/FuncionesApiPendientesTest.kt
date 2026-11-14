package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.core.realtime.ReverbClient
import com.cmo.hospital.features.doctor.consulta.data.repository.ConsultaRepositoryImpl
import com.cmo.hospital.features.doctor.consulta.domain.usecase.AgendarCitaSeguimientoDoctorUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.data.repository.AmbulanciaGuardiaRepositoryImpl
import com.cmo.hospital.features.recepcionista.citas.data.repository.CitaRepositoryImpl
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

/** Seguimiento del médico, signos vitales, pacientes y ambulancia del médico contra una API simulada. */
class FuncionesApiPendientesTest {

    private val llamadas = mutableListOf<String>()
    private val cuerpos = mutableMapOf<String, Map<*, *>>()
    private val hoy = LocalDate.now().toString()
    private val enSieteDias = LocalDate.now().plusDays(7).toString()
    private var pacientesApi = """[{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001"}]"""

    private fun ok(data: String, code: Int = 200) = code to """{"success":true,"message":"ok","data":$data}"""

    private val citaHoy = """{"id_cita":"u-c1","fecha":"$hoy","hora_inicio":"09:00:00","hora_fin":"09:30:00","estado":"EN_ESPERA","motivo":"Fiebre",
        "id_medico":"u-med","paciente":{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001","dui":null,"sexo":"F"},
        "medico":{"id":"u-med","nombre_completo":"Dra. Ana Martínez","usuario":"medico.general","email":null,"rol":"MEDICO","cargo":"Medicina General","telefono":null,"estado":"ACTIVO"},
        "consultorio":null,"consulta":null,"signos_vitales":null}"""

    private fun responder(metodo: String, ruta: String): Pair<Int, String> = when ("$metodo $ruta") {
        "GET citas" -> ok("""{"data":[$citaHoy],"current_page":1,"last_page":1,"total":1}""")
        "GET pacientes" -> ok("""{"data":$pacientesApi,"current_page":1,"last_page":1,"total":1}""")
        "GET agenda/disponibilidad" -> ok("""[{"hora_inicio":"10:00","hora_fin":"10:30"}]""")
        "POST citas" -> ok("""{"id_cita":"u-c2","fecha":"$enSieteDias","hora_inicio":"10:00","hora_fin":"10:30","estado":"AGENDADA","motivo":"Control: x"}""", 201)
        "PUT citas/u-c1/signos-vitales" -> ok("""{"id_signo":"s1","id_cita":"u-c1","presion_sistolica":120,"presion_diastolica":80,"temperatura_c":36.6}""")
        "GET ambulancia" -> ok("""{"estado":"EN_SERVICIO","servicio_activo":{"id_servicio":"u-srv","prioridad":"CRITICA","hora_salida":"2026-11-13T16:41:00Z",
            "ultimo_reporte":{"estado_paciente":"DELICADO","observaciones":"PA 90/60","llegada_estimada":null,"fecha":"2026-11-13T16:50:00Z"}}}""")
        "POST broadcasting/auth" -> 200 to """{"auth":"hospital-key:firma"}"""
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

    private val ds = HospitalClinicalDataSource(api)

    @Test
    fun testSeguimientoDelMedicoUsaPostCitasConSusPacientesYHorarios() = runTest {
        val repo = ConsultaRepositoryImpl(ds)
        assertEquals("u-p1", repo.pacientesTratados().getOrThrow().single().id)
        assertEquals("10:00", repo.disponibilidad("u-med", enSieteDias).getOrThrow().single().horaInicio)

        val agendar = AgendarCitaSeguimientoDoctorUseCase(repo)
        fun campo(r: Result<*>) = (r.exceptionOrNull() as ApiValidationException).campo
        assertEquals("id_paciente", campo(agendar(null, "u-med", enSieteDias, "10:00", "x")))
        assertEquals("hora_inicio", campo(agendar("u-p1", "u-med", enSieteDias, null, "x")))
        assertEquals("motivo", campo(agendar("u-p1", "u-med", enSieteDias, "10:00", " ")))
        assertFalse("POST citas" in llamadas)

        assertTrue(agendar("u-p1", "u-med", enSieteDias, "10:00", " Revisar exámenes ").isSuccess)
        assertEquals(
            mapOf("id_medico" to "u-med", "fecha" to enSieteDias, "hora_inicio" to "10:00", "id_paciente" to "u-p1", "motivo" to "Control: Revisar exámenes"),
            cuerpos["POST citas"]
        )
    }

    @Test
    fun testSignosVitalesDeRecepcionVanALaApiYPasarAConsultaNo() = runTest {
        ds.syncCitasFromRemote()
        val repo = CitaRepositoryImpl(ds)
        val signos = SignosVitales(presionSistolica = 120, presionDiastolica = 80, temperaturaC = 36.6)

        assertTrue(repo.registrarTriajeOPasarAConsulta("u-c1", signos, pasarAConsulta = true).isFailure)
        assertFalse("PUT citas/u-c1/signos-vitales" in llamadas)
        assertTrue(repo.registrarTriajeOPasarAConsulta("u-c1", SignosVitales(), pasarAConsulta = false).isFailure)

        assertTrue(repo.registrarTriajeOPasarAConsulta("u-c1", signos, pasarAConsulta = false).isSuccess)
        // Gson no envía los campos nulos ni las propiedades calculadas (IMC).
        assertEquals(
            mapOf("presion_sistolica" to 120.0, "presion_diastolica" to 80.0, "temperatura_c" to 36.6),
            cuerpos["PUT citas/u-c1/signos-vitales"]
        )
        assertEquals(120, ds.citasState.value.single { it.id == "u-c1" }.signosVitales?.presionSistolica)
    }

    @Test
    fun testPacientesDelMedicoSeReemplazanConLosDeLaApi() = runTest {
        assertTrue(ds.expedientes.value.isEmpty())
        ds.syncPacientesFromRemote()
        assertEquals(listOf("Ana López"), ds.expedientes.value.map { it.paciente.nombreCompleto })

        pacientesApi = """[{"id_paciente":"u-p9","nombre_completo":"Luis Pérez","numero_expediente":"EXP-2026-000009"}]"""
        ds.syncPacientesFromRemote()
        assertEquals(listOf("Luis Pérez"), ds.expedientes.value.map { it.paciente.nombreCompleto })
    }

    @Test
    fun testAmbulanciaDelMedicoLeeLaFormaReducidaYAutorizaElCanalPrivado() = runTest {
        val repo = AmbulanciaGuardiaRepositoryImpl(api, ReverbClient(urlBase = "ws://localhost/"))

        val amb = repo.ambulancia().getOrThrow()
        assertEquals("EN_SERVICIO", amb.estado)
        assertTrue(amb.servicios.isEmpty())
        val srv = amb.servicioActivo!!
        assertEquals("u-srv", srv.id)
        assertEquals("CRITICA", srv.prioridad)
        assertEquals("DELICADO", srv.ultimoReporte?.estadoPaciente)
        assertNull(srv.pacienteNombre)

        val res = api.autorizarCanal(mapOf("socket_id" to "123.456", "channel_name" to "private-personal"))
        assertEquals("hospital-key:firma", res.body()?.get("auth"))
        assertEquals(mapOf("socket_id" to "123.456", "channel_name" to "private-personal"), cuerpos["POST broadcasting/auth"])
    }
}
