package com.cmo.hospital.features.doctor.laboratorio.domain.usecase

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.laboratorio.domain.repository.LaboratorioRepository
import kotlinx.coroutines.flow.StateFlow

class GetOrdenesLabFlowUseCase(private val repository: LaboratorioRepository) {
    operator fun invoke(): StateFlow<List<OrdenLaboratorioItem>> = repository.ordenesLabFlow
}

class RefrescarLaboratorioUseCase(private val repository: LaboratorioRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.refrescar()
}

class EmitirOrdenLabUseCase(private val repository: LaboratorioRepository) {
    /** Nombres del catálogo del laboratorio (vacío hasta que se carga desde la API). */
    fun catalogo(): List<String> = repository.catalogoExamenesFlow.value.map { it.nombre }

    suspend fun cargarCatalogo(): Result<Unit> = repository.refrescar()

    suspend operator fun invoke(
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente? = null
    ): Result<List<OrdenLaboratorioItem>> {
        val limpios = examenes.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (limpios.isEmpty()) {
            return Result.failure(IllegalArgumentException("Seleccione al menos un examen de laboratorio."))
        }
        return repository.emitirOrdenesLaboratorio(
            examenes = limpios,
            prioridad = prioridad,
            observaciones = observaciones.trim(),
            paciente = paciente
        )
    }
}

class CancelarOrdenLabUseCase(private val repository: LaboratorioRepository) {
    suspend operator fun invoke(orden: OrdenLaboratorioItem): Result<OrdenLaboratorioItem> {
        if (!orden.cancelable) {
            return Result.failure(
                IllegalStateException("Solo se puede cancelar una orden Pendiente; esta ya está ${orden.estado}.")
            )
        }
        return repository.cancelarOrden(orden.id)
    }
}
