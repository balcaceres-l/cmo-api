package com.cmo.hospital.features.recepcionista.admision.domain.usecase

import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.admision.domain.repository.AdmisionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCapacidadCamasUseCase(
    private val repository: AdmisionRepository
) {
    operator fun invoke(): Flow<CapacidadHospitalaria> =
        repository.camasFlow.map { repository.obtenerCapacidadActual() }
}

class ObserveHabitacionesDetalladasUseCase(
    private val repository: AdmisionRepository
) {
    operator fun invoke(): Flow<List<HabitacionDetalleUi>> =
        repository.camasFlow.map { repository.obtenerHabitacionesDetalladas() }
}

class RefrescarCamasUseCase(private val repository: AdmisionRepository) {
    suspend operator fun invoke(): Result<List<Cama>> = repository.refrescarCamas()
}

class CargarMedicosTratantesUseCase(private val repository: AdmisionRepository) {
    suspend operator fun invoke(): Result<List<User>> = repository.medicos()
}

class BuscarPacientesIngresoUseCase(private val repository: AdmisionRepository) {
    suspend operator fun invoke(texto: String): Result<List<Paciente>> {
        val limpio = texto.trim()
        return repository.buscarPacientes(limpio)
    }
}

/**
 * Ingreso hospitalario: paciente registrado + cama libre + médico tratante +
 * motivo. El tipo (VIP / SEMIPRIVADA) sale de la cama elegida. La API vuelve a
 * validar que la cama siga libre y que el paciente no tenga otro ingreso activo.
 */
class IngresarPacienteUseCase(private val repository: AdmisionRepository) {
    suspend operator fun invoke(
        idPaciente: String?,
        cama: Cama?,
        idMedico: String?,
        motivo: String
    ): Result<Unit> {
        if (idPaciente.isNullOrBlank()) {
            return Result.failure(ApiValidationException("id_paciente", "Busque y seleccione al paciente."))
        }
        if (idMedico.isNullOrBlank()) {
            return Result.failure(ApiValidationException("id_medico", "Seleccione el médico tratante."))
        }
        if (motivo.isBlank()) {
            return Result.failure(ApiValidationException("motivo", "Ingrese el motivo del ingreso."))
        }
        if (cama == null) {
            return Result.failure(ApiValidationException("id_cama", "Seleccione una cama libre en el mapa."))
        }
        if (!cama.estaDisponible) {
            return Result.failure(ApiValidationException("id_cama", "La cama ${cama.codigo} está ocupada."))
        }
        return repository.ingresar(idPaciente, cama, idMedico, motivo.trim())
    }
}

class DarAltaUseCase(private val repository: AdmisionRepository) {
    suspend operator fun invoke(cama: Cama): Result<Unit> {
        val ingreso = cama.idIngreso
            ?: return Result.failure(IllegalStateException("La cama ${cama.codigo} no tiene un ingreso activo."))
        return repository.darAlta(ingreso)
    }
}

class TrasladarPacienteUseCase(private val repository: AdmisionRepository) {
    suspend operator fun invoke(origen: Cama, destino: Cama): Result<Unit> {
        val ingreso = origen.idIngreso
            ?: return Result.failure(IllegalStateException("La cama ${origen.codigo} no tiene un ingreso activo."))
        if (!destino.estaDisponible || destino.id == origen.id) {
            return Result.failure(IllegalStateException("Elija una cama libre distinta de ${origen.codigo}."))
        }
        return repository.trasladar(ingreso, destino.id)
    }
}
