package com.cmo.hospital.features.doctor.laboratorio.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.laboratorio.domain.repository.LaboratorioRepository
import kotlinx.coroutines.flow.StateFlow

class LaboratorioRepositoryImpl(
    private val dataSource: HospitalClinicalDataSource
) : LaboratorioRepository {

    override val ordenesLabFlow: StateFlow<List<OrdenLaboratorioItem>> = dataSource.ordenesLab

    override fun emitirOrdenesLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente?,
        estadoInicial: String
    ): List<OrdenLaboratorioItem> = dataSource.emitirOrdenesLaboratorio(
        examenes = examenes,
        prioridad = prioridad,
        observaciones = observaciones,
        paciente = paciente,
        estadoInicial = estadoInicial
    )

    override fun cambiarEstadoOrdenLaboratorio(ordenId: String, nuevoEstado: String) {
        dataSource.cambiarEstadoOrdenLaboratorio(ordenId, nuevoEstado)
    }
}
