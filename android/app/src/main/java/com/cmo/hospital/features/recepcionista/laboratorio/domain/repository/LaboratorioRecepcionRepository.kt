package com.cmo.hospital.features.recepcionista.laboratorio.domain.repository

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem

/** Recepción y laboratorio avanzan las órdenes; el resultado del examen no pasa por el sistema. */
interface LaboratorioRecepcionRepository {
    /** [estado] en formato de la API (PENDIENTE, EN_PROCESO…) o null para todas. */
    suspend fun listar(estado: String?): Result<List<OrdenLaboratorioItem>>
    suspend fun iniciar(ordenId: String): Result<OrdenLaboratorioItem>
    suspend fun completar(ordenId: String): Result<OrdenLaboratorioItem>
}
