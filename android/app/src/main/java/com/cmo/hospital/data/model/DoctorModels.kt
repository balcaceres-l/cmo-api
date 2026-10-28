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
    val motivo: String? = null,
    val paciente: PacienteInfo? = null,
    val consultorio: ConsultorioInfo? = null,
    @SerializedName("id_medico") val idMedico: String? = null,
    @SerializedName("medico_nombre") val medicoNombre: String? = "Dr. Alejandro Restrepo",
    @SerializedName("especialidad") val especialidad: String? = "Medicina General",
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
) {
    /**
     * Calcula la hora de finalización sumando el bloque de 30 minutos obligatorio.
     */
    companion object {
        val BLOQUES_30_MINUTOS = listOf(
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
        )

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

        /**
         * Validación estricta: por ningún motivo habrán citas agendadas a la misma hora para el mismo médico.
         */
        fun hayConflictoHorario(
            citasExistentes: List<Cita>,
            medicoNombre: String?,
            fecha: String,
            horaInicio: String,
            citaIdExcluir: String? = null
        ): Boolean {
            return citasExistentes.any { cita ->
                if (cita.id == citaIdExcluir || cita.estado == "CANCELADA") return@any false
                val coincideFecha = cita.fecha == fecha
                val coincideHora = cita.horaInicio == horaInicio
                val coincideMedico = medicoNombre.isNullOrBlank() ||
                        cita.medicoNombre.isNullOrBlank() ||
                        cita.medicoNombre.equals(medicoNombre, ignoreCase = true)
                coincideFecha && coincideHora && coincideMedico
            }
        }
    }
}

val BLOQUES_30_MINUTOS = Cita.BLOQUES_30_MINUTOS

val CATALOGO_ESPECIALIDADES = mapOf(
    "Medicina General" to listOf("Dr. Alejandro Restrepo", "Dra. Sofía Valenzuela"),
    "Medicina Interna" to listOf("Dr. Alejandro Restrepo", "Dr. Marcos Peralta"),
    "Cardiología" to listOf("Dra. Camila Benítez", "Dr. Fernando Ruiz"),
    "Pediatría" to listOf("Dra. Elena Ramos", "Dr. Andrés Meza"),
    "Cirugía General" to listOf("Dr. Javier Morales"),
    "Ginecología y Obstetricia" to listOf("Dra. Lucía Domínguez")
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
