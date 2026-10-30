package com.cmo.hospital.features.doctor.laboratorio.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.dto.ExamenDto
import com.cmo.hospital.features.doctor.laboratorio.domain.repository.LaboratorioRepository
import kotlinx.coroutines.flow.StateFlow

class LaboratorioRepositoryImpl(
    private val dataSource: HospitalClinicalDataSource
) : LaboratorioRepository {

    override val ordenesLabFlow: StateFlow<List<OrdenLaboratorioItem>> = dataSource.ordenesLab

    override val catalogoExamenesFlow: StateFlow<List<ExamenDto>> = dataSource.examenesCatalogo

    override suspend fun refrescar(): Result<Unit> {
        dataSource.syncExamenesCatalogo().onFailure { return Result.failure(it) }
        return dataSource.syncOrdenesLabFromRemote().map { }
    }

    override suspend fun emitirOrdenesLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente?
    ): Result<List<OrdenLaboratorioItem>> = dataSource.emitirOrdenesLaboratorio(
        examenes = examenes,
        prioridad = prioridad,
        observaciones = observaciones,
        paciente = paciente
    )

    override suspend fun cancelarOrden(ordenId: String): Result<OrdenLaboratorioItem> =
        dataSource.cancelarOrdenLaboratorio(ordenId)
}
