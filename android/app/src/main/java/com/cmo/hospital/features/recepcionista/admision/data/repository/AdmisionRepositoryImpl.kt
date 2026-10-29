package com.cmo.hospital.features.recepcionista.admision.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.features.recepcionista.admision.domain.repository.AdmisionRepository
import kotlinx.coroutines.flow.StateFlow

class AdmisionRepositoryImpl(
    private val dataSource: HospitalClinicalDataSource
) : AdmisionRepository {

    override val camasFlow: StateFlow<List<Cama>> = dataSource.camasState

    override fun obtenerCapacidadActual(): CapacidadHospitalaria =
        dataSource.construirCapacidadHospitalaria()

    override suspend fun registrarEIngresarPaciente(
        paciente: Paciente,
        signosVitales: SignosVitales?,
        codigoCama: String?,
        motivoIngreso: String,
        esEmergenciaDirecta: Boolean
    ): Result<Paciente> = dataSource.registrarEIngresarPaciente(
        paciente = paciente,
        signosVitales = signosVitales,
        codigoCama = codigoCama,
        motivoIngreso = motivoIngreso,
        esEmergenciaDirecta = esEmergenciaDirecta
    )
}
