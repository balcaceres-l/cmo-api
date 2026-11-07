package com.cmo.hospital.core.data.mapper

import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi

/**
 * Agrupa las camas (de GET /habitaciones) por habitación para la pestaña
 * "Vista de habitaciones": VIP individual en planta 3, semiprivadas A/B en planta 2.
 */
fun agruparHabitaciones(camas: List<Cama>): List<HabitacionDetalleUi> {
    val vips = camas.filter { it.esVip }.sortedBy { it.codigo }.map { cama ->
        HabitacionDetalleUi(
            codigo = cama.codigo,
            tipo = "VIP Individual",
            planta = "3ª Planta",
            camas = listOf(cama),
            totalCamas = 1,
            camasOcupadas = if (cama.estaDisponible) 0 else 1,
            camasDisponibles = if (cama.estaDisponible) 1 else 0
        )
    }
    val semiprivadas = camas.filter { !it.esVip }
        .groupBy { it.codigo.substringBeforeLast("-") }
        .entries.sortedBy { it.key }
        .map { (codigoSala, camasSala) ->
            val ordenadas = camasSala.sortedBy { it.codigo }
            HabitacionDetalleUi(
                codigo = codigoSala,
                tipo = "Semiprivada",
                planta = "2ª Planta",
                camas = ordenadas,
                totalCamas = ordenadas.size,
                camasOcupadas = ordenadas.count { !it.estaDisponible },
                camasDisponibles = ordenadas.count { it.estaDisponible }
            )
        }
    return vips + semiprivadas
}
