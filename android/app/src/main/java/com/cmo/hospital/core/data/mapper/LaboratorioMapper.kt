package com.cmo.hospital.core.data.mapper

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.dto.OrdenLaboratorioDto
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Orden de la API → orden de pantalla. Lo usan el médico y recepción para que
 * ambos muestren igual los estados (Pendiente, En Proceso, Completado, Cancelada).
 *
 * @param nombreExamen nombre a usar si la respuesta no trae el examen cargado.
 * @param pacienteConsulta paciente a usar si la respuesta no trae el paciente cargado.
 */
fun OrdenLaboratorioDto.aOrdenLaboratorio(
    nombreExamen: String? = null,
    pacienteConsulta: Paciente? = null
): OrdenLaboratorioItem {
    val esStat = observaciones?.startsWith(OrdenLaboratorioItem.MARCA_STAT) == true
    return OrdenLaboratorioItem(
        id = id,
        pacienteId = idPaciente,
        pacienteNombre = paciente?.nombreCompleto ?: pacienteConsulta?.nombreCompleto,
        numeroExpediente = paciente?.numeroExpediente ?: pacienteConsulta?.numeroExpediente,
        idExamen = idExamen,
        examenNombre = examen?.nombre ?: nombreExamen ?: "Examen de laboratorio",
        tipoTubo = null,
        prioridad = if (esStat) "STAT" else "RUTINA",
        estado = OrdenLaboratorioItem.estadoDesdeApi(estado),
        fechaSolicitud = fechaLegible(fechaSolicitud),
        fechaResultado = fechaLegible(fechaResultado),
        observaciones = observaciones?.removePrefix(OrdenLaboratorioItem.MARCA_STAT)?.trim()?.ifBlank { null },
        medicoSolicitante = null,
        idConsulta = idConsulta
    )
}

/** "2026-10-30T15:04:00.000000Z" → "30/10 09:04" en la hora del teléfono. */
fun fechaLegible(iso: String?): String? = iso?.let {
    try {
        OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
    } catch (_: Exception) {
        it
    }
}
