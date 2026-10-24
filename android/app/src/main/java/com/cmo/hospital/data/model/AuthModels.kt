package com.cmo.hospital.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String?,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    val usuario: String,
    val email: String?,
    val rol: String,
    val cargo: String?,
    val telefono: String?,
    val estado: String
)

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
