package com.cmo.hospital.data.model

import com.google.gson.annotations.SerializedName

data class PacienteInfo(
    val id: String?,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("numero_expediente") val numeroExpediente: String?,
    val dui: String?,
    val sexo: String?,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val telefono: String? = null,
    val alergias: String? = null,
    @SerializedName("tipo_sangre") val tipoSangre: String? = null
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
    val consultorio: ConsultorioInfo?,
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
) {
    /**
     * Calcula la hora de finalización sumando el bloque de 30 minutos obligatorio.
     */
    companion object {
        fun calcularFin30Minutos(horaInicio: String): String {
            return try {
                val parts = horaInicio.split(":")
                if (parts.size >= 2) {
                    var h = parts[0].trim().toInt()
                    var m = parts[1].trim().toInt() + 30
                    if (m >= 60) {
                        h = (h + m / 60) % 24
                        m %= 60
                    }
                    String.format(java.util.Locale.US, "%02d:%02d", h, m)
                } else horaInicio
            } catch (e: Exception) {
                horaInicio
            }
        }
    }
}

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
