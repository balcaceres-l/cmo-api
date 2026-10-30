package com.cmo.hospital.features.doctor.laboratorio.domain.usecase

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.laboratorio.domain.repository.LaboratorioRepository
import kotlinx.coroutines.flow.StateFlow

class GetOrdenesLabFlowUseCase(private val repository: LaboratorioRepository) {
    operator fun invoke(): StateFlow<List<OrdenLaboratorioItem>> = repository.ordenesLabFlow
}

class EmitirOrdenLabUseCase(private val repository: LaboratorioRepository) {
    operator fun invoke(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente? = null,
        estadoInicial: String = "En Espera"
    ): Result<List<OrdenLaboratorioItem>> {
        val limpios = examenes.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (limpios.isEmpty()) {
            return Result.failure(IllegalArgumentException("Seleccione al menos un examen de laboratorio."))
        }
        val estadoValido = if (estadoInicial in OrdenLaboratorioItem.ESTADOS_VALIDOS) {
            estadoInicial
        } else "En Espera"
        val creadas = repository.emitirOrdenesLaboratorio(
            examenes = limpios,
            prioridad = prioridad,
            observaciones = observaciones.trim(),
            paciente = paciente,
            estadoInicial = estadoValido
        )
        return Result.success(creadas)
    }
}

class CambiarEstadoOrdenLabUseCase(private val repository: LaboratorioRepository) {
    operator fun invoke(ordenId: String, nuevoEstado: String): Result<Unit> {
        if (nuevoEstado !in OrdenLaboratorioItem.ESTADOS_VALIDOS) {
            return Result.failure(IllegalArgumentException("Estado de laboratorio inválido: $nuevoEstado"))
        }
        repository.cambiarEstadoOrdenLaboratorio(ordenId, nuevoEstado)
        return Result.success(Unit)
    }
}
