package com.cmo.hospital.features.doctor.consulta.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.RecetaItem
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.features.doctor.consulta.domain.repository.ConsultaRepository
import kotlinx.coroutines.flow.StateFlow

class ConsultaRepositoryImpl(
    private val clinicalDataSource: HospitalClinicalDataSource
) : ConsultaRepository {

    override val consultaActivaFlow: StateFlow<ConsultaActiva>
        get() = clinicalDataSource.consultaActiva

    override val citasFlow: StateFlow<List<Cita>>
        get() = clinicalDataSource.citasState

    override fun actualizarSegundos(segundos: Int) {
        clinicalDataSource.actualizarSegundosConsulta(segundos)
    }

    override fun actualizarSignos(signos: SignosVitales) {
        clinicalDataSource.actualizarSignosConsulta(signos)
    }

    override fun agregarCie10(codigo: String, descripcion: String) {
        clinicalDataSource.agregarDiagnosticoCie10(codigo, descripcion)
    }

    override fun eliminarCie10(codigo: String) {
        clinicalDataSource.eliminarDiagnosticoCie10(codigo)
    }

    override fun actualizarAnamnesis(texto: String) {
        clinicalDataSource.actualizarAnamnesis(texto)
    }

    override suspend fun agregarReceta(receta: RecetaItem): Result<RecetaItem> =
        clinicalDataSource.agregarReceta(receta)

    override suspend fun eliminarReceta(recetaId: String): Result<Unit> =
        clinicalDataSource.eliminarReceta(recetaId)

    override suspend fun guardarCambios(): Result<Unit> =
        clinicalDataSource.guardarConsultaRemota()

    override suspend fun finalizarConsulta(): Result<String> =
        clinicalDataSource.finalizarConsulta()

    override suspend fun agendarCitaSeguimiento(cita: Cita) {
        clinicalDataSource.agendarCitaRemotaOLocal(cita)
    }
}
