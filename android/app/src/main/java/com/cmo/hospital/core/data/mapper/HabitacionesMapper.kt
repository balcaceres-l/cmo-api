package com.cmo.hospital.core.data.mapper

import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.Habitacion
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi
import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * Deserializa la respuesta de GET /habitaciones independientemente de si Laravel
 * devuelve un objeto agrupado por planta ({"2":[...],"3":[...]}) o un arreglo ([[...],[...]]).
 */
fun parsearHabitacionesJson(elemento: JsonElement?, gson: Gson = Gson()): List<Habitacion> {
    if (elemento == null || elemento.isJsonNull) return emptyList()
    val resultado = mutableListOf<Habitacion>()
    when {
        elemento.isJsonArray -> {
            for (item in elemento.asJsonArray) {
                if (item.isJsonArray) {
                    for (sub in item.asJsonArray) {
                        if (sub.isJsonObject) {
                            runCatching { gson.fromJson(sub, Habitacion::class.java) }.getOrNull()?.let { resultado += it }
                        }
                    }
                } else if (item.isJsonObject) {
                    runCatching { gson.fromJson(item, Habitacion::class.java) }.getOrNull()?.let { resultado += it }
                }
            }
        }
        elemento.isJsonObject -> {
            for ((_, valor) in elemento.asJsonObject.entrySet()) {
                resultado += parsearHabitacionesJson(valor, gson)
            }
        }
    }
    return resultado
}

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
