package com.cmo.hospital.features.doctor.expedientes.domain.usecase

import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import kotlinx.coroutines.flow.StateFlow

class GetExpedientesFlowUseCase(private val repository: ExpedientesRepository) {
    operator fun invoke(): StateFlow<List<ExpedienteDetalle>> = repository.expedientesFlow
}

class FiltrarExpedientesUseCase {
    operator fun invoke(
        expedientes: List<ExpedienteDetalle>,
        query: String
    ): List<ExpedienteDetalle> {
        val q = query.trim()
        if (q.isBlank()) return expedientes
        return expedientes.filter { exp ->
            exp.paciente.nombreCompleto.contains(q, ignoreCase = true) ||
                (exp.paciente.numeroExpediente?.contains(q, ignoreCase = true) == true) ||
                (exp.paciente.dui?.contains(q, ignoreCase = true) == true)
        }
    }
}
