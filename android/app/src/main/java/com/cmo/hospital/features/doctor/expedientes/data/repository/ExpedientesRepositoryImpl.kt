package com.cmo.hospital.features.doctor.expedientes.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import kotlinx.coroutines.flow.StateFlow

class ExpedientesRepositoryImpl(
    private val clinicalDataSource: HospitalClinicalDataSource
) : ExpedientesRepository {
    override val expedientesFlow: StateFlow<List<ExpedienteDetalle>>
        get() = clinicalDataSource.expedientes

    override val expedienteSolicitadoFlow: StateFlow<String?>
        get() = clinicalDataSource.expedienteSolicitado

    override fun consumirExpedienteSolicitado() {
        clinicalDataSource.consumirExpedienteSolicitado()
    }
}
