package com.cmo.hospital.core.network.dto

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

data class IngresoPacienteRequest(
    @SerializedName("id_paciente") val idPaciente: String,
    val tipo: String,
    @SerializedName("id_cama") val idCama: String? = null,
    @SerializedName("id_medico") val idMedico: String? = null,
    val motivo: String,
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
)
