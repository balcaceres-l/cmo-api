package com.cmo.hospital.features.doctor.consulta.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.BloqueAgenda
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.EspecialidadCatalogo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.ProgramacionCita
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

    override suspend fun guardarSignos(signos: SignosVitales): Result<Unit> {
        val consulta = clinicalDataSource.consultaActiva.value
        if (consulta.finalizada || consulta.citaId.isBlank()) {
            return Result.failure(IllegalStateException("No hay una consulta abierta."))
        }
        return clinicalDataSource.guardarSignosVitales(consulta.citaId, signos).map { }
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

    override suspend fun pacientesTratados(): Result<List<Paciente>> = clinicalDataSource.buscarPacientes("")

    override suspend fun especialidades(): Result<List<EspecialidadCatalogo>> =
        clinicalDataSource.getEspecialidades()

    override suspend fun disponibilidad(
        idMedico: String?,
        fecha: String,
        idEspecialidad: String?
    ): Result<List<BloqueAgenda>> =
        clinicalDataSource.getDisponibilidad(idMedico = idMedico, fecha = fecha, idEspecialidad = idEspecialidad)

    override suspend fun agendarSeguimiento(
        idPaciente: String,
        programacion: ProgramacionCita,
        motivo: String
    ): Result<Cita> = clinicalDataSource.agendarCita(idPaciente, programacion, motivo)
}
