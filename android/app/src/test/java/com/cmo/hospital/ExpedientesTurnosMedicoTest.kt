package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.features.doctor.expedientes.data.repository.ExpedientesRepositoryImpl
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.GuardarAntecedentesUseCase
import com.cmo.hospital.features.doctor.inicio.data.repository.InicioDoctorRepositoryImpl
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ResumirMisTurnosUseCase
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.LocalTime

/** Expedientes y turnos del médico contra una API simulada con la forma del backend. */
class ExpedientesTurnosMedicoTest {

    private val consultas = mutableMapOf<String, String>()
    private val cuerpos = mutableMapOf<String, Map<*, *>>()

    private fun ok(data: String) = 200 to """{"success":true,"message":"ok","data":$data}"""

    private val expediente = """{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001",
        "fecha_nacimiento":"1990-05-10T00:00:00.000000Z","dui":"01234567-8","sexo":"F","telefono":null,"tipo_sangre":"O+","alergias":"Penicilina",
        "historial":{"antecedentes_personales":"Asma","antecedentes_familiares":"  "},
        "citas":[
          {"id_cita":"c1","fecha":"2026-10-01T00:00:00.000000Z","hora_inicio":"09:00:00","estado":"ATENDIDA","motivo":"Tos",
           "medico":{"id":"m1","nombre_completo":"Dra. Ana Martínez","cargo":"Medicina General"},
           "consulta":{"id_consulta":"k1","id_cita":"c1","motivo":"Tos","diagnostico":"J20 — Bronquitis aguda","notas":"Tos seca",
             "fecha_hora_inicio":null,"fecha_hora_fin":"2026-10-01T15:30:00Z",
             "recetas":[{"id_receta":"r1","medicamento":"Ambroxol","dosis":"30 mg","frecuencia":"Cada 8 h","duracion":"5 días","indicaciones":null}]}},
          {"id_cita":"c2","fecha":"2026-11-03T00:00:00.000000Z","hora_inicio":"10:30:00","estado":"ATENDIDA","motivo":"Control",
           "medico":{"id":"m1","nombre_completo":"Dra. Ana Martínez","cargo":"Medicina General"},
           "consulta":{"id_consulta":"k2","id_cita":"c2","motivo":"Control","diagnostico":null,"notas":null,
             "fecha_hora_inicio":null,"fecha_hora_fin":null,"recetas":[]}},
          {"id_cita":"c3","fecha":"2026-11-20T00:00:00.000000Z","hora_inicio":"08:00:00","estado":"AGENDADA","motivo":"Futuro","medico":null,"consulta":null}
        ],
        "ordenes_laboratorio":[
          {"id_orden":"o1","id_consulta":"k1","id_paciente":"u-p1","id_examen":"e1","estado":"COMPLETADO","observaciones":null,
           "fecha_solicitud":"2026-10-01T15:10:00Z","fecha_resultado":null,"examen":{"id_examen":"e1","nombre":"Hemograma"}},
          {"id_orden":"o2","id_consulta":"k2","id_paciente":"u-p1","id_examen":"e2","estado":"PENDIENTE","observaciones":"[STAT] Urgente",
           "fecha_solicitud":"2026-11-03T16:40:00Z","fecha_resultado":null,"examen":{"id_examen":"e2","nombre":"Glucosa"}}
        ],
        "ingresos":[
          {"id_ingreso":"i0","fecha_ingreso":"2026-09-01T10:00:00Z","fecha_alta":"2026-09-03T10:00:00Z","motivo":"x","cama":{"codigo":"HAB-201-A","habitacion":{"tipo":"SEMIPRIVADA","planta":2}}},
          {"id_ingreso":"i1","fecha_ingreso":"2026-11-04T10:00:00Z","fecha_alta":null,"motivo":"y","cama":{"codigo":"VIP-302","habitacion":{"tipo":"VIP","planta":3}}}
        ]}"""

    private fun responder(metodo: String, ruta: String): Pair<Int, String> = when ("$metodo $ruta") {
        "GET pacientes" -> ok("""{"data":[{"id_paciente":"u-p1","nombre_completo":"Ana López","numero_expediente":"EXP-2026-000001"}],"current_page":1,"last_page":1,"total":1}""")
        "GET pacientes/u-p1/expediente" -> ok(expediente)
        "PUT pacientes/u-p1/historial" -> ok("""{"antecedentes_personales":"Asma","antecedentes_familiares":null}""")
        "GET mi/turnos" -> ok("""{"data":[
            {"id_turno":"t0","fecha":"2026-11-05T00:00:00.000000Z","hora_inicio":"07:00:00","hora_fin":"15:00:00","area":"Consulta externa"},
            {"id_turno":"t1","fecha":"2026-11-06T00:00:00.000000Z","hora_inicio":"19:00:00","hora_fin":"07:00:00","area":"Emergencia"},
            {"id_turno":"t2","fecha":"2026-11-08T00:00:00.000000Z","hora_inicio":"07:00:00","hora_fin":"15:00:00","area":null}
            ],"current_page":1,"last_page":1,"total":3}""")
        else -> 404 to """{"success":false,"message":"no simulado","errors":null}"""
    }

    private val api: ApiService = Retrofit.Builder()
        .baseUrl("http://localhost/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val req = chain.request()
            val clave = "${req.method} ${req.url.encodedPath.removePrefix("/api/v1/")}"
            req.url.query?.let { consultas[clave] = it }
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
    private val repo = ExpedientesRepositoryImpl(api, ds)

    @Test
    fun testBusquedaVaciaNoMandaFiltro() = runTest {
        assertEquals("u-p1", repo.buscarPacientes("  ").getOrThrow().single().id)
        assertEquals("per_page=50", consultas["GET pacientes"])
        repo.buscarPacientes("Ana")
        assertEquals("q=Ana&per_page=50", consultas["GET pacientes"])
    }

    @Test
    fun testExpedienteSeArmaConLoQueDevuelveLaApi() = runTest {
        val exp = repo.expediente("u-p1").getOrThrow()

        assertEquals("1990-05-10", exp.paciente.fechaNacimiento)
        assertEquals(listOf("Penicilina"), exp.alergiasCriticas)
        assertEquals(listOf("Personales: Asma"), exp.comorbilidades)
        assertNull(exp.antecedentesFamiliares)
        assertEquals("Cama VIP-302 • VIP, planta 3", exp.cama)

        // Solo citas con consulta, de la más reciente a la más antigua.
        assertEquals(listOf("2026-11-03", "2026-10-01"), exp.atencionesPrevias.map { it.fecha })
        val reciente = exp.atencionesPrevias.first()
        assertEquals("Control", reciente.diagnostico)
        assertEquals("Consulta en curso", reciente.notas)
        val antigua = exp.atencionesPrevias.last()
        assertEquals("J20 — Bronquitis aguda", antigua.diagnostico)
        assertEquals("Dra. Ana Martínez", antigua.medico)
        assertEquals("Medicina General", antigua.especialidad)
        assertTrue(antigua.notas.contains("℞ Ambroxol 30 mg"))

        assertEquals(listOf("Glucosa", "Hemograma"), exp.ordenesLaboratorio.map { it.examenNombre })
        assertEquals("STAT", exp.ordenesLaboratorio.first().prioridad)
    }

    @Test
    fun testAntecedentesVaciosSeMandanComoNull() = runTest {
        assertTrue(GuardarAntecedentesUseCase(repo)("u-p1", " Asma ", "   ").isSuccess)
        assertEquals(mapOf("antecedentes_personales" to "Asma"), cuerpos["PUT pacientes/u-p1/historial"])
    }

    @Test
    fun testTurnoNocturnoCuentaComoActualDespuesDeMedianoche() = runTest {
        val repoInicio = InicioDoctorRepositoryImpl(ds, api)
        val resumen = ResumirMisTurnosUseCase(repoInicio) { LocalDateTime.of(2026, 11, 7, 2, 0) }().getOrThrow()

        assertEquals("Emergencia", resumen.actual?.area)
        assertEquals(LocalTime.of(7, 0), resumen.actual?.horaFin)
        assertEquals(listOf("2026-11-08"), resumen.proximos.map { it.fecha.toString() })
        assertEquals("per_page=100", consultas["GET mi/turnos"])
    }

    @Test
    fun testSinTurnoAhoraSoloQuedanLosProximos() = runTest {
        val repoInicio = InicioDoctorRepositoryImpl(ds, api)
        val resumen = ResumirMisTurnosUseCase(repoInicio) { LocalDateTime.of(2026, 11, 7, 12, 0) }().getOrThrow()

        assertNull(resumen.actual)
        assertEquals(1, resumen.proximos.size)
    }
}
