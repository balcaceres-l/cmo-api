package com.cmo.hospital.features.doctor.expedientes.domain.usecase

import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository

/** Pacientes que atiende el médico; vacío = todos los suyos. */
class BuscarPacientesTratadosUseCase(private val repository: ExpedientesRepository) {
    suspend operator fun invoke(texto: String): Result<List<Paciente>> = repository.buscarPacientes(texto)
}

class CargarExpedienteUseCase(private val repository: ExpedientesRepository) {
    suspend operator fun invoke(idPaciente: String): Result<ExpedienteDetalle> = repository.expediente(idPaciente)
}

class GuardarAntecedentesUseCase(private val repository: ExpedientesRepository) {
    suspend operator fun invoke(idPaciente: String, personales: String, familiares: String): Result<Unit> =
        repository.guardarAntecedentes(
            idPaciente,
            personales.trim().ifBlank { null },
            familiares.trim().ifBlank { null }
        )
}
