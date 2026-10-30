package com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.features.recepcionista.laboratorio.domain.repository.LaboratorioRecepcionRepository

/** Filtros de la pantalla: etiqueta visible → estado de la API (null = todas). */
enum class FiltroLaboratorio(val etiqueta: String, val estadoApi: String?) {
    PENDIENTES("Pendientes", "PENDIENTE"),
    EN_PROCESO("En proceso", "EN_PROCESO"),
    COMPLETADAS("Completadas", "COMPLETADO"),
    TODAS("Todas", null)
}

class ListarOrdenesLaboratorioUseCase(private val repository: LaboratorioRecepcionRepository) {
    suspend operator fun invoke(filtro: FiltroLaboratorio): Result<List<OrdenLaboratorioItem>> =
        repository.listar(filtro.estadoApi)
}

/** Avanza la orden un paso: Pendiente → En Proceso → Completado. */
class AvanzarOrdenLaboratorioUseCase(private val repository: LaboratorioRecepcionRepository) {
    suspend operator fun invoke(orden: OrdenLaboratorioItem): Result<OrdenLaboratorioItem> = when (orden.estado) {
        OrdenLaboratorioItem.PENDIENTE -> repository.iniciar(orden.id)
        OrdenLaboratorioItem.EN_PROCESO -> repository.completar(orden.id)
        else -> Result.failure(IllegalStateException("La orden ya está ${orden.estado}; no tiene un siguiente paso."))
    }

    companion object {
        /** Texto del botón para el siguiente paso, o null si la orden ya terminó. */
        fun accionPara(orden: OrdenLaboratorioItem): String? = when (orden.estado) {
            OrdenLaboratorioItem.PENDIENTE -> "Iniciar"
            OrdenLaboratorioItem.EN_PROCESO -> "Completar"
            else -> null
        }
    }
}
