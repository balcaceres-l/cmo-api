package com.cmo.hospital.features.doctor.inicio.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.TurnoPersonal
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.llamarApi
import java.time.LocalDate
import java.time.LocalTime
import com.cmo.hospital.features.doctor.inicio.domain.repository.InicioDoctorRepository
import kotlinx.coroutines.flow.StateFlow

class InicioDoctorRepositoryImpl(
    private val clinicalDataSource: HospitalClinicalDataSource,
    private val api: ApiService? = null
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

    override suspend fun misTurnos(): Result<List<TurnoPersonal>> {
        val servicio = api ?: return Result.failure(IllegalStateException("La API no está configurada."))
        return llamarApi { servicio.getMisTurnos() }.map { pagina ->
            pagina.data.mapNotNull { t ->
                runCatching {
                    TurnoPersonal(
                        fecha = LocalDate.parse(t.fecha.take(10)),
                        horaInicio = LocalTime.parse(t.horaInicio.take(5)),
                        horaFin = LocalTime.parse(t.horaFin.take(5)),
                        area = t.area
                    )
                }.getOrNull()
            }
        }
    }
}
