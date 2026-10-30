package com.cmo.hospital.features.doctor.laboratorio.domain.repository

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.dto.ExamenDto
import kotlinx.coroutines.flow.StateFlow

interface LaboratorioRepository {
    val ordenesLabFlow: StateFlow<List<OrdenLaboratorioItem>>
    val catalogoExamenesFlow: StateFlow<List<ExamenDto>>

    /** Carga el catálogo de exámenes y las órdenes del médico desde la API. */
    suspend fun refrescar(): Result<Unit>

    suspend fun emitirOrdenesLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente?
    ): Result<List<OrdenLaboratorioItem>>

    suspend fun cancelarOrden(ordenId: String): Result<OrdenLaboratorioItem>
}
