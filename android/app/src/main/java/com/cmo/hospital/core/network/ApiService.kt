package com.cmo.hospital.core.network

import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.core.network.dto.*
import retrofit2.Response
import retrofit2.http.*

// Kotlin compila Map<String, Any?> como Map<String, ?> y Retrofit rechaza los
// comodines en @Body: sin esto, cada llamada con un Map falla al invocarse.
@JvmSuppressWildcards
interface ApiService {
    // Autenticación
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponseData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<User>>

    // Citas y Turnos de Atención (30 min)
    @GET("citas")
    suspend fun getCitas(): Response<ApiResponse<PaginatedData<Cita>>>

    @POST("citas")
    suspend fun agendarCita(
        @Body request: Map<String, Any?>
    ): Response<ApiResponse<Cita>>

    @PATCH("citas/{id}/llegada")
    suspend fun registrarLlegada(
        @Path("id") id: String
    ): Response<ApiResponse<Cita>>

    @PATCH("citas/{id}/cancelar")
    suspend fun cancelarCita(
        @Path("id") id: String,
        @Body motivo: Map<String, String>
    ): Response<ApiResponse<Cita>>

    // Pacientes y Expedientes
    @GET("pacientes")
    suspend fun getPacientes(
        @Query("q") query: String? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<ApiResponse<PaginatedData<Paciente>>>

    @POST("pacientes")
    suspend fun registrarPaciente(
        @Body request: Map<String, Any?>
    ): Response<ApiResponse<Paciente>>

    @PUT("pacientes/{id}")
    suspend fun actualizarPaciente(
        @Path("id") id: String,
        @Body request: Map<String, Any?>
    ): Response<ApiResponse<Paciente>>

    @POST("pacientes/{id}/codigo-activacion")
    suspend fun generarCodigoActivacion(
        @Path("id") id: String
    ): Response<ApiResponse<CodigoActivacion>>

    @GET("pacientes/{id}/expediente")
    suspend fun getExpediente(
        @Path("id") id: String
    ): Response<ApiResponse<Map<String, Any>>>

    // Habitaciones y Camas Hospitalarias (17 camas estrictas)
    @GET("habitaciones")
    suspend fun getHabitaciones(): Response<ApiResponse<List<List<Habitacion>>>>

    @POST("ingresos")
    suspend fun ingresarPaciente(
        @Body request: IngresoPacienteRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @PATCH("ingresos/{id}/alta")
    suspend fun altaPaciente(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    // Ambulancia Única (Tiempo Real y Bitácora)
    @GET("ambulancia")
    suspend fun getAmbulancia(): Response<ApiResponse<Map<String, Any>>>

    @GET("publico/ambulancia")
    suspend fun getAmbulanciaPublica(): Response<ApiResponse<Map<String, Any>>>

    @POST("ambulancia/servicios")
    suspend fun despacharAmbulancia(
        @Body request: Map<String, Any?>
    ): Response<ApiResponse<ServicioAmbulanciaDetalle>>

    @PATCH("ambulancia/servicios/{id}/finalizar")
    suspend fun finalizarServicioAmbulancia(
        @Path("id") id: String,
        @Body notas: Map<String, String>
    ): Response<ApiResponse<Unit>>

    @POST("ambulancia/servicios/{id}/reportes")
    suspend fun reportarDesdeAmbulancia(
        @Path("id") id: String,
        @Body request: ReporteAmbulancia
    ): Response<ApiResponse<ReporteAmbulancia>>

    @GET("ambulancia/servicios")
    suspend fun getBitacoraAmbulancia(): Response<ApiResponse<PaginatedData<ServicioAmbulanciaDetalle>>>

    // Consulta Médica, CIE-10 y Recetas Digitales
    @POST("citas/{id}/consulta")
    suspend fun abrirConsulta(
        @Path("id") citaId: String
    ): Response<ApiResponse<ConsultaDto>>

    @GET("consultas/{id}")
    suspend fun getConsulta(
        @Path("id") consultaId: String
    ): Response<ApiResponse<ConsultaDto>>

    @PUT("consultas/{id}")
    suspend fun actualizarConsulta(
        @Path("id") consultaId: String,
        @Body data: Map<String, Any?>
    ): Response<ApiResponse<ConsultaDto>>

    @PATCH("consultas/{id}/finalizar")
    suspend fun finalizarConsulta(
        @Path("id") consultaId: String
    ): Response<ApiResponse<ConsultaDto>>

    @POST("consultas/{id}/recetas")
    suspend fun agregarReceta(
        @Path("id") consultaId: String,
        @Body receta: Map<String, Any?>
    ): Response<ApiResponse<RecetaDto>>

    @DELETE("recetas/{id}")
    suspend fun eliminarReceta(
        @Path("id") recetaId: String
    ): Response<ApiResponse<Unit>>

    // Laboratorio: solo estados (PENDIENTE → EN_PROCESO → COMPLETADO, o CANCELADA)
    @GET("examenes")
    suspend fun getExamenes(
        @Query("per_page") perPage: Int = 100
    ): Response<ApiResponse<PaginatedData<ExamenDto>>>

    @POST("consultas/{id}/ordenes-laboratorio")
    suspend fun crearOrdenLaboratorio(
        @Path("id") consultaId: String,
        @Body orden: Map<String, Any?>
    ): Response<ApiResponse<OrdenLaboratorioDto>>

    @GET("ordenes-laboratorio")
    suspend fun getOrdenesLaboratorio(
        @Query("per_page") perPage: Int = 100,
        @Query("estado") estado: String? = null
    ): Response<ApiResponse<PaginatedData<OrdenLaboratorioDto>>>

    /** Recepción / laboratorio: PENDIENTE → EN_PROCESO. */
    @PATCH("ordenes-laboratorio/{id}/iniciar")
    suspend fun iniciarOrdenLaboratorio(
        @Path("id") ordenId: String
    ): Response<ApiResponse<OrdenLaboratorioDto>>

    /** Recepción / laboratorio: EN_PROCESO → COMPLETADO (sin resultados). */
    @PATCH("ordenes-laboratorio/{id}/completar")
    suspend fun completarOrdenLaboratorio(
        @Path("id") ordenId: String
    ): Response<ApiResponse<OrdenLaboratorioDto>>

    @PATCH("ordenes-laboratorio/{id}/cancelar")
    suspend fun cancelarOrdenLaboratorio(
        @Path("id") ordenId: String,
        @Body motivo: Map<String, Any?>
    ): Response<ApiResponse<OrdenLaboratorioDto>>

    // Turnos y Personal de Guardia
    @GET("mi/turnos")
    suspend fun getMisTurnos(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("publico/medicos-de-turno")
    suspend fun getMedicosDeTurno(): Response<ApiResponse<List<Map<String, Any>>>>
}
