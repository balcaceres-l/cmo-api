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

    override suspend fun agendarCita(
        especialidad: String,
        medicoNombre: String,
        pacienteNombre: String,
        dui: String,
        fechaNacimiento: String?,
        fecha: String,
        horaInicio: String,
        motivo: String,
        box: String
    ): Result<Cita> {
        if (Cita.hayConflictoHorario(citasFlow.value, medicoNombre, fecha, horaInicio)) {
            return Result.failure(
                IllegalStateException(
                    "Horario no disponible: $medicoNombre ya tiene una cita a las $horaInicio."
                )
            )
        }
        val horaFin = Cita.calcularFin30Minutos(horaInicio)
        val nuevaCita = Cita(
            id = "c-" + System.currentTimeMillis().toString().takeLast(6),
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            estado = "AGENDADA",
            motivo = motivo,
            medicoNombre = medicoNombre,
            especialidad = especialidad,
            paciente = PacienteInfo(
                id = "p-" + System.currentTimeMillis().toString().takeLast(4),
                nombreCompleto = pacienteNombre,
                numeroExpediente = Paciente.generarNumeroExpediente(
                    pacienteNombre,
                    2026,
                    (10..99).random()
                ),
                dui = dui.ifBlank { null },
                sexo = "M",
                fechaNacimiento = fechaNacimiento
            ),
            consultorio = ConsultorioInfo(
                id = "cs-1",
                nombre = "$especialidad ($box)",
                codigo = box
            )
        )
        dataSource.agendarCitaRemotaOLocal(nuevaCita)
        return Result.success(nuevaCita)
    }

    override suspend fun marcarLlegada(citaId: String) {
        dataSource.marcarLlegada(citaId)
    }

    override suspend fun cancelarCita(citaId: String, motivo: String) {
        dataSource.cancelarCita(citaId, motivo)
    }

    override suspend fun registrarTriajeOPasarAConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ): Result<String> {
        val transferida = dataSource.actualizarTriajeOPasarAConsulta(
            citaId = citaId,
            signosVitales = signosVitales,
            pasarAConsulta = pasarAConsulta
        )
        return if (pasarAConsulta && transferida != null) {
            Result.success(
                "Paciente ${transferida.paciente?.nombreCompleto} transferido a Consulta Activa en ${transferida.consultorio?.codigo ?: "Box 03"}."
            )
        } else {
            Result.success("Signos vitales de triaje guardados correctamente.")
        }
    }

    override fun reLlamarTurno(): Cita? = dataSource.reLlamarTurno()

    override fun siguienteTurno(): Cita? = dataSource.siguienteTurno()
}
