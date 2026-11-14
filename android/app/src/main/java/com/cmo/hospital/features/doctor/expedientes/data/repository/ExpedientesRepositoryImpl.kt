package com.cmo.hospital.features.doctor.expedientes.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.data.mapper.aExpedienteDetalle
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.llamarApi
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import kotlinx.coroutines.flow.StateFlow

class ExpedientesRepositoryImpl(
    private val api: ApiService,
    private val clinicalDataSource: HospitalClinicalDataSource
) : ExpedientesRepository {

    override val expedienteSolicitadoFlow: StateFlow<String?>
        get() = clinicalDataSource.expedienteSolicitado

    override fun consumirExpedienteSolicitado() {
        clinicalDataSource.consumirExpedienteSolicitado()
    }

    override suspend fun buscarPacientes(texto: String): Result<List<Paciente>> =
        llamarApi { api.getPacientes(query = texto.trim().ifBlank { null }, perPage = 50) }.map { it.data }

    override suspend fun expediente(idPaciente: String): Result<ExpedienteDetalle> =
        llamarApi { api.getExpediente(idPaciente) }.map { it.aExpedienteDetalle() }

    override suspend fun guardarAntecedentes(idPaciente: String, personales: String?, familiares: String?): Result<Unit> =
        llamarApi {
            api.actualizarHistorial(
                idPaciente,
                mapOf("antecedentes_personales" to personales, "antecedentes_familiares" to familiares)
            )
        }.map { }
}
