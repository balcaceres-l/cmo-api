package com.cmo.hospital.features.doctor.laboratorio.domain.repository

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import kotlinx.coroutines.flow.StateFlow

interface LaboratorioRepository {
    val ordenesLabFlow: StateFlow<List<OrdenLaboratorioItem>>

    fun emitirOrdenesLaboratorio(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente?,
        estadoInicial: String
    ): List<OrdenLaboratorioItem>

    fun cambiarEstadoOrdenLaboratorio(ordenId: String, nuevoEstado: String)
}
