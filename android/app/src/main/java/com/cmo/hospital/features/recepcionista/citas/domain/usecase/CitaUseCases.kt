package com.cmo.hospital.features.recepcionista.citas.domain.usecase

import com.cmo.hospital.core.domain.model.BloqueAgenda
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultorioInfo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.citas.domain.repository.CitaRepository
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeParseException

class GetCitasFlowUseCase(private val repository: CitaRepository) {
    operator fun invoke(): StateFlow<List<Cita>> = repository.citasFlow
}

class GetTurnoEnBoxFlowUseCase(private val repository: CitaRepository) {
    operator fun invoke(): StateFlow<Cita?> = repository.turnoEnBoxFlow
}

class RefreshCitasUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(): List<Cita> = repository.refreshCitas()
}

/** Médicos activos y consultorios: lo que necesita el formulario para programar. */
class CargarCatalogosAgendaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(): Result<Pair<List<User>, List<ConsultorioInfo>>> {
        val medicos = repository.medicos().getOrElse { return Result.failure(it) }
        val consultorios = repository.consultorios().getOrElse { return Result.failure(it) }
        return Result.success(medicos to consultorios)
    }
}

class GetDisponibilidadUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(idMedico: String, fecha: String): Result<List<BloqueAgenda>> =
        repository.disponibilidad(idMedico, fecha)
}

class BuscarPacientesParaCitaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(texto: String): Result<List<Paciente>> {
        val limpio = texto.trim()
        if (limpio.length < 2) return Result.success(emptyList())
        return repository.buscarPacientes(limpio)
    }
}

/**
 * Reglas locales antes de llamar a la API (la API vuelve a validarlas, más
 * turno del médico y solapes). Los campos usan el nombre de la API para que el
 * formulario marque el mismo campo con errores locales o del servidor.
 */
class ValidarProgramacionCita(private val hoy: () -> LocalDate = { LocalDate.now() }) {
    fun validar(programacion: ProgramacionCita): ApiValidationException? {
        if (programacion.idMedico.isBlank()) {
            return ApiValidationException("id_medico", "Seleccione el médico.")
        }
        val fecha = try {
            LocalDate.parse(programacion.fecha.trim())
        } catch (_: DateTimeParseException) {
            return ApiValidationException("fecha", "Ingrese la fecha como AAAA-MM-DD.")
        }
        if (fecha.isBefore(hoy())) {
            return ApiValidationException("fecha", "La cita no puede quedar en una fecha pasada.")
        }
        if (!Regex("^\\d{2}:\\d{2}$").matches(programacion.horaInicio)) {
            return ApiValidationException("hora_inicio", "Seleccione un horario disponible.")
        }
        return null
    }
}

class AgendarCitaUseCase(
    private val repository: CitaRepository,
    private val validador: ValidarProgramacionCita = ValidarProgramacionCita()
) {
    suspend operator fun invoke(
        idPaciente: String?,
        programacion: ProgramacionCita,
        motivo: String
    ): Result<Cita> {
        if (idPaciente.isNullOrBlank()) {
            return Result.failure(ApiValidationException("id_paciente", "Busque y seleccione al paciente."))
        }
        validador.validar(programacion)?.let { return Result.failure(it) }
        if (motivo.isBlank()) {
            return Result.failure(ApiValidationException("motivo", "Ingrese el motivo de la cita."))
        }
        return repository.agendarCita(idPaciente, programacion.normalizada(), motivo.trim())
    }
}

class ConfirmarCitaUseCase(
    private val repository: CitaRepository,
    private val validador: ValidarProgramacionCita = ValidarProgramacionCita()
) {
    suspend operator fun invoke(citaId: String, programacion: ProgramacionCita): Result<Cita> {
        validador.validar(programacion)?.let { return Result.failure(it) }
        return repository.confirmarCita(citaId, programacion.normalizada())
    }
}

class ReprogramarCitaUseCase(
    private val repository: CitaRepository,
    private val validador: ValidarProgramacionCita = ValidarProgramacionCita()
) {
    suspend operator fun invoke(citaId: String, programacion: ProgramacionCita): Result<Cita> {
        validador.validar(programacion)?.let { return Result.failure(it) }
        return repository.reprogramarCita(citaId, programacion.normalizada())
    }
}

class MarcarLlegadaCitaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(citaId: String): Result<Cita> = repository.marcarLlegada(citaId)
}

class CancelarCitaUseCase(private val repository: CitaRepository) {
    suspend operator fun invoke(citaId: String, motivo: String): Result<Cita> {
        if (motivo.isBlank()) {
            return Result.failure(ApiValidationException("motivo_cancelacion", "Indique el motivo de la cancelación."))
        }
        return repository.cancelarCita(citaId, motivo.trim())
    }
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

private fun ProgramacionCita.normalizada() = copy(fecha = fecha.trim())
