package com.cmo.hospital.core.data.mapper

import com.cmo.hospital.core.domain.model.AtencionHistorica
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.network.dto.CitaExpedienteDto
import com.cmo.hospital.core.network.dto.ExpedienteDto

/**
 * Expediente de la API → pantalla del médico. Las atenciones son las citas con
 * consulta registrada (de la más reciente a la más antigua). La API no guarda
 * signos vitales, así que esa sección queda vacía.
 */
fun ExpedienteDto.aExpedienteDetalle(): ExpedienteDetalle {
    val paciente = Paciente(
        id = id,
        nombreCompleto = nombreCompleto,
        fechaNacimiento = fechaNacimiento?.take(10),
        dui = dui,
        sexo = sexo,
        telefono = telefono,
        tipoSangre = tipoSangre,
        alergias = alergias,
        numeroExpediente = numeroExpediente
    )

    val atenciones = citas.orEmpty()
        .filter { it.consulta != null }
        .sortedWith(compareByDescending<CitaExpedienteDto> { it.fecha.orEmpty() }
            .thenByDescending { it.horaInicio.orEmpty() })
        .map { cita ->
            val consulta = cita.consulta!!
            val recetas = consulta.recetas.orEmpty().joinToString("\n") {
                "℞ ${it.medicamento} ${it.dosis} • ${it.frecuencia} • ${it.duracion}"
            }
            AtencionHistorica(
                fecha = cita.fecha?.take(10).orEmpty(),
                hora = cita.horaInicio?.take(5).orEmpty(),
                especialidad = cita.especialidad?.takeIf { it.isNotBlank() }
                    ?: cita.medico?.especialidadVisible
                    ?: "Consulta externa",
                diagnostico = consulta.diagnostico?.takeIf { it.isNotBlank() }
                    ?: cita.motivo ?: "Sin diagnóstico registrado",
                medico = cita.medico?.nombreCompleto ?: "Médico",
                notas = listOfNotNull(
                    consulta.notas?.takeIf { it.isNotBlank() },
                    recetas.takeIf { it.isNotBlank() },
                    if (consulta.fechaHoraFin == null) "Consulta en curso" else null
                ).joinToString("\n").ifBlank { "Sin notas" }
            )
        }

    val ingresoActivo = ingresos.orEmpty().firstOrNull { it.fechaAlta == null }
    val ubicacion = ingresoActivo?.cama?.let { cama ->
        val tipo = when (cama.habitacion?.tipo) {
            "VIP" -> "VIP"
            "SEMIPRIVADA" -> "semiprivada"
            else -> null
        }
        listOfNotNull("Cama ${cama.codigo}", tipo?.let { "$it, planta ${cama.habitacion?.planta}" })
            .joinToString(" • ")
    }

    val personales = historial?.antecedentesPersonales?.trim()?.ifBlank { null }
    val familiares = historial?.antecedentesFamiliares?.trim()?.ifBlank { null }

    return ExpedienteDetalle(
        paciente = paciente,
        cama = ubicacion,
        signosVitales = SignosVitales(),
        alergiasCriticas = listOfNotNull(alergias?.trim()?.ifBlank { null }),
        comorbilidades = listOfNotNull(
            personales?.let { "Personales: $it" },
            familiares?.let { "Familiares: $it" }
        ),
        atencionesPrevias = atenciones,
        // Se ordena por la fecha ISO de la API (la legible "dd/MM HH:mm" no ordena bien).
        ordenesLaboratorio = ordenesLaboratorio.orEmpty()
            .sortedByDescending { it.fechaSolicitud.orEmpty() }
            .map { it.aOrdenLaboratorio(pacienteConsulta = paciente) },
        antecedentesPersonales = personales,
        antecedentesFamiliares = familiares
    )
}
