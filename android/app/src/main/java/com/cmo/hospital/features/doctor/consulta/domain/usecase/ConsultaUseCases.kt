package com.cmo.hospital.features.doctor.consulta.domain.usecase

import com.cmo.hospital.features.recepcionista.citas.domain.usecase.ValidarProgramacionCita
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.ConsultorioInfo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.PacienteInfo
import com.cmo.hospital.core.domain.model.RecetaItem
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.features.doctor.consulta.domain.repository.ConsultaRepository
import kotlinx.coroutines.flow.StateFlow

class GetConsultaActivaFlowUseCase(private val repository: ConsultaRepository) {
    operator fun invoke(): StateFlow<ConsultaActiva> = repository.consultaActivaFlow
}

class TickCronometroConsultaUseCase(private val repository: ConsultaRepository) {
    operator fun invoke(segundos: Int) {
        repository.actualizarSegundos(segundos.coerceAtLeast(0))
    }
}

class ActualizarSignosConsultaUseCase(private val repository: ConsultaRepository) {
    suspend operator fun invoke(signos: SignosVitales): Result<Unit> = repository.guardarSignos(signos)
}

class GestionarCie10ConsultaUseCase(private val repository: ConsultaRepository) {
    fun agregar(codigo: String, descripcion: String): Result<Unit> {
        if (codigo.isBlank() || descripcion.isBlank()) {
            return Result.failure(IllegalArgumentException("Seleccione un código CIE-10 válido."))
        }
        repository.agregarCie10(codigo.trim().uppercase(), descripcion.trim())
        return Result.success(Unit)
    }

    fun eliminar(codigo: String) {
        repository.eliminarCie10(codigo)
    }
}

class GuardarAnamnesisUseCase(private val repository: ConsultaRepository) {
    operator fun invoke(texto: String) {
        repository.actualizarAnamnesis(texto)
    }
}

class GestionarRecetaConsultaUseCase(private val repository: ConsultaRepository) {
    /** Valida en el teléfono antes de enviar; la receta solo aparece cuando la API la guarda. */
    fun validar(
        medicamento: String,
        dosis: String,
        frecuencia: String,
        duracion: String,
        indicaciones: String?,
        tipo: String
    ): Result<RecetaItem> {
        if (medicamento.isBlank() || dosis.isBlank() || frecuencia.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Medicamento, dosis y frecuencia son campos obligatorios.")
            )
        }
        return Result.success(
            RecetaItem(
                medicamento = medicamento.trim(),
                dosis = dosis.trim(),
                frecuencia = frecuencia.trim(),
                duracion = duracion.trim().ifBlank { "Según evolución" },
                indicaciones = indicaciones?.trim()?.ifBlank { null },
                tipo = tipo.ifBlank { "Crónico" }
            )
        )
    }

    suspend fun guardar(item: RecetaItem): Result<RecetaItem> = repository.agregarReceta(item)

    suspend fun eliminar(recetaId: String): Result<Unit> = repository.eliminarReceta(recetaId)
}

class GuardarCambiosConsultaUseCase(private val repository: ConsultaRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.guardarCambios()
}

class FinalizarConsultaUseCase(private val repository: ConsultaRepository) {
    suspend operator fun invoke(): Result<String> = repository.finalizarConsulta()
}

/**
 * Control de seguimiento agendado por el médico. La API solo lo acepta para
 * pacientes que ya atiende y dentro de su turno; aquí se valida lo básico.
 */
class AgendarCitaSeguimientoDoctorUseCase(
    private val repository: ConsultaRepository,
    private val validador: ValidarProgramacionCita = ValidarProgramacionCita()
) {
    suspend operator fun invoke(
        idPaciente: String?,
        idMedico: String? = null,
        idEspecialidad: String? = null,
        fecha: String,
        horaInicio: String?,
        motivo: String
    ): Result<Cita> {
        if (idPaciente.isNullOrBlank()) {
            return Result.failure(ApiValidationException("id_paciente", "Seleccione el paciente."))
        }
        val esp = idEspecialidad?.takeIf { it.isNotBlank() }
        val med = if (esp != null) null else idMedico?.takeIf { it.isNotBlank() }
        val programacion = ProgramacionCita(
            idMedico = med,
            fecha = fecha.trim(),
            horaInicio = horaInicio.orEmpty(),
            idEspecialidad = esp
        )
        validador.validar(programacion)?.let { return Result.failure(it) }
        if (motivo.isBlank()) {
            return Result.failure(ApiValidationException("motivo", "El motivo del control es obligatorio."))
        }
        return repository.agendarSeguimiento(idPaciente, programacion, "Control: ${motivo.trim()}")
    }
}
