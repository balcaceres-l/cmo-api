package com.cmo.hospital.data.model

import com.google.gson.annotations.SerializedName

data class PacienteInfo(
    val id: String?,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("numero_expediente") val numeroExpediente: String?,
    val dui: String?,
    val sexo: String?
)

data class ConsultorioInfo(
    val id: String?,
    val nombre: String?,
    val codigo: String?
)

data class Cita(
    val id: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String?,
    val estado: String,
    val motivo: String?,
    val paciente: PacienteInfo?,
    val consultorio: ConsultorioInfo?
)

data class PaginatedData<T>(
    val data: List<T>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int
)

data class DoctorSummary(
    val citasHoy: Int,
    val pacientesAtendidos: Int,
    val ordenesLaboratorio: Int,
    val ambulanciaEstado: String
)
