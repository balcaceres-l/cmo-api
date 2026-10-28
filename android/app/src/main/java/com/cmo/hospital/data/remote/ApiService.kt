package com.cmo.hospital.data.remote

import com.cmo.hospital.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // -------------------------------------------------------------
    // Autenticación
    // -------------------------------------------------------------
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponseData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<User>>

    // -------------------------------------------------------------
    // Citas y Turnos de Atención (30 min)
    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
    // Pacientes y Expedientes
    // -------------------------------------------------------------
    @GET("pacientes")
    suspend fun getPacientes(
        @Query("q") query: String? = null
    ): Response<ApiResponse<PaginatedData<Paciente>>>

    @POST("pacientes")
    suspend fun registrarPaciente(
        @Body request: Map<String, Any?>
    ): Response<ApiResponse<Paciente>>

    @GET("pacientes/{id}/expediente")
    suspend fun getExpediente(
        @Path("id") id: String
    ): Response<ApiResponse<Map<String, Any>>>

    // -------------------------------------------------------------
    // Habitaciones y Camas Hospitalarias (17 camas estrictas)
    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
    // Ambulancia Única (Tiempo Real y Bitácora)
    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
    // Consulta Médica, CIE-10 y Recetas Digitales
    // -------------------------------------------------------------
    @POST("citas/{id}/consulta")
    suspend fun abrirConsulta(
        @Path("id") citaId: String
    ): Response<ApiResponse<Map<String, Any>>>

    @PUT("consultas/{id}")
    suspend fun actualizarConsulta(
        @Path("id") consultaId: String,
        @Body data: Map<String, Any?>
    ): Response<ApiResponse<Unit>>

    @PATCH("consultas/{id}/finalizar")
    suspend fun finalizarConsulta(
        @Path("id") consultaId: String
    ): Response<ApiResponse<Unit>>

    @POST("consultas/{id}/recetas")
    suspend fun agregarReceta(
        @Path("id") consultaId: String,
        @Body receta: RecetaItem
    ): Response<ApiResponse<RecetaItem>>

    @DELETE("recetas/{id}")
    suspend fun eliminarReceta(
        @Path("id") recetaId: String
    ): Response<ApiResponse<Unit>>

    // -------------------------------------------------------------
    // Laboratorio Clínico (LIS)
    // -------------------------------------------------------------
    @GET("examenes")
    suspend fun getExamenes(): Response<ApiResponse<List<Map<String, Any>>>>

    @POST("consultas/{id}/ordenes-laboratorio")
    suspend fun crearOrdenLaboratorio(
        @Path("id") consultaId: String,
        @Body orden: Map<String, Any?>
    ): Response<ApiResponse<OrdenLaboratorioItem>>

    @GET("ordenes-laboratorio")
    suspend fun getOrdenesLaboratorio(): Response<ApiResponse<PaginatedData<OrdenLaboratorioItem>>>

    // -------------------------------------------------------------
    // Turnos y Personal de Guardia
    // -------------------------------------------------------------
    @GET("mi/turnos")
    suspend fun getMisTurnos(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("publico/medicos-de-turno")
    suspend fun getMedicosDeTurno(): Response<ApiResponse<List<Map<String, Any>>>>
}
