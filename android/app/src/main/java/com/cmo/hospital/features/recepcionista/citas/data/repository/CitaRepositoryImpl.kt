package com.cmo.hospital.features.recepcionista.citas.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.features.recepcionista.citas.domain.repository.CitaRepository
import kotlinx.coroutines.flow.StateFlow

class CitaRepositoryImpl(
    private val dataSource: HospitalClinicalDataSource
) : CitaRepository {

    override val citasFlow: StateFlow<List<Cita>> = dataSource.citasState
    override val turnoEnBoxFlow: StateFlow<Cita?> = dataSource.turnoEnBox

    override suspend fun refreshCitas(): List<Cita> = dataSource.syncCitasFromRemote()

    override suspend fun medicos(): Result<List<User>> = dataSource.getMedicosAgenda()

    override suspend fun consultorios(): Result<List<ConsultorioInfo>> = dataSource.getConsultorios()

    override suspend fun disponibilidad(idMedico: String, fecha: String): Result<List<BloqueAgenda>> =
        dataSource.getDisponibilidad(idMedico, fecha)

    override suspend fun buscarPacientes(texto: String): Result<List<Paciente>> =
        dataSource.buscarPacientes(texto)

    override suspend fun agendarCita(
        idPaciente: String,
        programacion: ProgramacionCita,
        motivo: String
    ): Result<Cita> = dataSource.agendarCita(idPaciente, programacion, motivo)

    override suspend fun confirmarCita(citaId: String, programacion: ProgramacionCita): Result<Cita> =
        dataSource.confirmarCita(citaId, programacion)

    override suspend fun reprogramarCita(citaId: String, programacion: ProgramacionCita): Result<Cita> =
        dataSource.reprogramarCita(citaId, programacion)

    override suspend fun marcarLlegada(citaId: String): Result<Cita> = dataSource.marcarLlegada(citaId)

    override suspend fun cancelarCita(citaId: String, motivo: String): Result<Cita> =
        dataSource.cancelarCita(citaId, motivo)

    override suspend fun registrarTriajeOPasarAConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ): Result<String> {
        val citaPrevia = dataSource.citasState.value.firstOrNull { it.id == citaId }
            ?: return Result.failure(IllegalArgumentException("No se encontró la cita seleccionada."))
        if (!citaPrevia.esDelDia()) {
            return Result.failure(
                IllegalStateException("La toma de signos vitales solo está habilitada el día de la cita.")
            )
        }
        val transferida = dataSource.actualizarTriajeOPasarAConsulta(
            citaId = citaId,
            signosVitales = signosVitales,
            pasarAConsulta = pasarAConsulta
        )
        return if (pasarAConsulta && transferida != null) {
            Result.success(
                "Paciente ${transferida.paciente?.nombreCompleto} transferido a Consulta Activa en ${transferida.consultorio?.codigo ?: "su consultorio"}."
            )
        } else {
            Result.success("Signos vitales de triaje guardados correctamente.")
        }
    }

    override fun reLlamarTurno(): Cita? = dataSource.reLlamarTurno()

    override fun siguienteTurno(): Cita? = dataSource.siguienteTurno()
}
