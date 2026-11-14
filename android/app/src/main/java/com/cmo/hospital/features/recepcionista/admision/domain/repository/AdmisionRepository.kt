package com.cmo.hospital.features.recepcionista.admision.domain.repository

import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.SignosVitales
import kotlinx.coroutines.flow.StateFlow

interface AdmisionRepository {
    val camasFlow: StateFlow<List<Cama>>

    fun obtenerCapacidadActual(): CapacidadHospitalaria

    suspend fun registrarEIngresarPaciente(
        paciente: Paciente,
        signosVitales: SignosVitales?,
        codigoCama: String?,
        motivoIngreso: String,
        esEmergenciaDirecta: Boolean
    ): Result<Paciente>

    suspend fun refrescarCamas() {}

    suspend fun trasladarCama(origenCodigo: String, destinoCodigo: String): Result<String> =
        Result.failure(UnsupportedOperationException("No implementado"))

    suspend fun darAltaCama(camaCodigo: String): Result<String> =
        Result.failure(UnsupportedOperationException("No implementado"))
}
