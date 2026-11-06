package com.cmo.hospital.core.domain.model

import com.google.gson.annotations.SerializedName

/** Bloque libre que devuelve `GET /agenda/disponibilidad` (horas en formato HH:mm). */
data class BloqueAgenda(
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String
)

/** Médico, día, hora y consultorio: lo que piden agendar, confirmar y reprogramar. */
data class ProgramacionCita(
    val idMedico: String,
    val fecha: String,
    val horaInicio: String,
    val idConsultorio: String? = null
) {
    fun aMapa(): Map<String, Any?> = buildMap {
        put("id_medico", idMedico)
        put("fecha", fecha)
        put("hora_inicio", horaInicio)
        if (!idConsultorio.isNullOrBlank()) put("id_consultorio", idConsultorio)
    }
}

/** Estados de la cita en la API y su texto para el mostrador. */
object EstadoCita {
    const val SOLICITADA = "SOLICITADA"
    const val AGENDADA = "AGENDADA"
    const val EN_ESPERA = "EN_ESPERA"
    const val EN_ATENCION = "EN_ATENCION"
    const val ATENDIDA = "ATENDIDA"
    const val CANCELADA = "CANCELADA"
    const val NO_ASISTIO = "NO_ASISTIO"

    fun etiqueta(estado: String): String = when (estado) {
        NO_ASISTIO -> "No asistió"
        SOLICITADA -> "Solicitada"
        AGENDADA -> "Agendada"
        EN_ESPERA -> "En espera"
        EN_ATENCION -> "En atención"
        ATENDIDA -> "Atendida"
        CANCELADA -> "Cancelada"
        else -> estado.lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * Primero lo que pide acción en el mostrador (solicitudes, sala de espera,
     * agendadas de la más próxima a la más lejana); al final el historial
     * cerrado, del más reciente al más antiguo.
     */
    fun ordenarParaMostrador(citas: List<Cita>): List<Cita> {
        fun grupo(c: Cita) = when (c.estado) {
            SOLICITADA -> 0
            EN_ESPERA, EN_ATENCION -> 1
            AGENDADA -> 2
            else -> 3
        }
        val (abiertas, cerradas) = citas.partition { grupo(it) < 3 }
        return abiertas.sortedWith(compareBy<Cita>({ grupo(it) }, { it.fecha }, { it.horaInicio })) +
            cerradas.sortedWith(compareByDescending<Cita> { it.fecha }.thenBy { it.horaInicio })
    }
}
