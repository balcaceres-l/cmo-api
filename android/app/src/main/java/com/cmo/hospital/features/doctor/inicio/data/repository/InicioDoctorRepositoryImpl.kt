package com.cmo.hospital.features.doctor.inicio.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.features.doctor.inicio.domain.repository.InicioDoctorRepository
import kotlinx.coroutines.flow.StateFlow

class InicioDoctorRepositoryImpl(
    private val clinicalDataSource: HospitalClinicalDataSource
) : InicioDoctorRepository {

    override val citasFlow: StateFlow<List<Cita>>
        get() = clinicalDataSource.citasState

    override val consultaActivaFlow: StateFlow<ConsultaActiva>
        get() = clinicalDataSource.consultaActiva

    override val expedientesFlow: StateFlow<List<ExpedienteDetalle>>
        get() = clinicalDataSource.expedientes

    override suspend fun sincronizarPanelMedico() {
        clinicalDataSource.syncPacientesFromRemote()
        clinicalDataSource.syncCitasFromRemote()
    }

    override suspend fun iniciarORetomarConsulta(citaId: String): Result<ConsultaActiva> =
        clinicalDataSource.iniciarORetomarConsultaDoctor(citaId)

    override fun solicitarAperturaExpediente(numeroExpediente: String?) {
        clinicalDataSource.solicitarAperturaExpediente(numeroExpediente)
    }
}
