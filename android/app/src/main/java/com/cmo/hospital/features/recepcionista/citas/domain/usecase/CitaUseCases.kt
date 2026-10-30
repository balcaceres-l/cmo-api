package com.cmo.hospital.features.recepcionista.citas.domain.usecase

import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.features.recepcionista.citas.domain.repository.CitaRepository
import kotlinx.coroutines.flow.StateFlow

class GetCitasFlowUseCase(private val repository: CitaRepository) {
    operator fun invoke(): StateFlow<List<Cita>> = repository.citasFlow
}

class GetTurnoEnBoxFlowUseCase(private val repository: CitaRepository) {
    operator fun invoke(): StateFlow<Cita?> = repository.turnoEnBoxFlow
}

class RefreshCitasUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(): List<Cita> = repository.refreshCitas()
}

class AgendarCitaCascadaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(
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
        if (pacienteNombre.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingrese el nombre completo del paciente."))
        }
        if (motivo.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingrese el motivo clínico de la cita."))
        }
        if (Cita.hayConflictoHorario(repository.citasFlow.value, medicoNombre, fecha, horaInicio)) {
            return Result.failure(
                IllegalStateException(
                    "Horario no disponible: $medicoNombre ya tiene una cita agendada a las $horaInicio el $fecha. Por ningún motivo se permiten citas duplicadas."
                )
            )
        }
        return repository.agendarCita(
            especialidad = especialidad,
            medicoNombre = medicoNombre,
            pacienteNombre = pacienteNombre.trim(),
            dui = dui.trim(),
            fechaNacimiento = fechaNacimiento?.trim()?.ifBlank { null },
            fecha = fecha.trim(),
            horaInicio = horaInicio,
            motivo = motivo.trim(),
            box = box.trim().ifBlank { "Box 03" }
        )
    }
}

class MarcarLlegadaCitaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(citaId: String) = repository.marcarLlegada(citaId)
}

class CancelarCitaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(citaId: String, motivo: String = "Cancelada en mostrador") =
        repository.cancelarCita(citaId, motivo)
}

class RegistrarTriajeYPasarConsultaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ): Result<String> = repository.registrarTriajeOPasarAConsulta(citaId, signosVitales, pasarAConsulta)
}

class ControlarLlamadorSalaUseCase(private val repository: CitaRepository) {
    fun reLlamar(): Cita? = repository.reLlamarTurno()
    fun llamarSiguiente(): Cita? = repository.siguienteTurno()
}
