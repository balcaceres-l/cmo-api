package com.cmo.hospital.core.network.dto

import com.cmo.hospital.core.domain.model.PacienteInfo
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.domain.model.User
import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val usuario: String,
    val password: String
)

data class AuthResponseData(
    val usuario: User,
    @SerializedName("id_paciente") val idPaciente: String?,
    val token: String,
    @SerializedName("token_type") val tokenType: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val errors: Map<String, List<String>>?
)

data class PaginatedData<T>(
    val data: List<T>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int
)

/** Consulta tal como la devuelve la API (`recetas` solo viene en GET consultas/{id}). */
data class ConsultaDto(
    @SerializedName("id_consulta") val id: String,
    @SerializedName("id_cita") val idCita: String?,
    val motivo: String?,
    val diagnostico: String?,
    val notas: String?,
    @SerializedName("fecha_hora_inicio") val fechaHoraInicio: String?,
    @SerializedName("fecha_hora_fin") val fechaHoraFin: String?,
    val recetas: List<RecetaDto>?,
    @SerializedName("ordenes_laboratorio") val ordenesLaboratorio: List<OrdenLaboratorioDto>? = null
)

data class ExamenDto(
    @SerializedName("id_examen") val id: String,
    val nombre: String
)

/** `paciente` y `examen` vienen cargados en GET ordenes-laboratorio. */
data class OrdenLaboratorioDto(
    @SerializedName("id_orden") val id: String,
    @SerializedName("id_consulta") val idConsulta: String?,
    @SerializedName("id_paciente") val idPaciente: String?,
    @SerializedName("id_examen") val idExamen: String?,
    val estado: String?,
    val observaciones: String?,
    @SerializedName("fecha_solicitud") val fechaSolicitud: String?,
    @SerializedName("fecha_resultado") val fechaResultado: String?,
    val paciente: PacienteInfo?,
    val examen: ExamenDto?
)

data class RecetaDto(
    @SerializedName("id_receta") val id: String,
    val medicamento: String,
    val dosis: String,
    val frecuencia: String,
    val duracion: String,
    val indicaciones: String?
)

data class CodigoActivacion(
    val codigo: String,
    @SerializedName("vence_en") val venceEn: String?
)

data class IngresoPacienteRequest(
    @SerializedName("id_paciente") val idPaciente: String,
    val tipo: String,
    @SerializedName("id_cama") val idCama: String? = null,
    @SerializedName("id_medico") val idMedico: String? = null,
    val motivo: String,
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
)
