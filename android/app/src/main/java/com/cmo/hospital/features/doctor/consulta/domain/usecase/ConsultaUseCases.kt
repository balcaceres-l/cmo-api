package com.cmo.hospital.features.doctor.consulta.domain.usecase

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
    operator fun invoke(signos: SignosVitales) {
        repository.actualizarSignos(signos)
    }
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

class AgendarCitaSeguimientoDoctorUseCase(private val repository: ConsultaRepository) {
    suspend operator fun invoke(
        paciente: Paciente,
        medicoNombre: String,
        especialidad: String,
        fecha: String,
        horaInicio: String,
        motivo: String
    ): Result<Cita> {
        if (fecha.isBlank()) {
            return Result.failure(IllegalArgumentException("Indique la fecha del control médico."))
        }
        if (horaInicio !in Cita.BLOQUES_30_MINUTOS) {
            return Result.failure(
                IllegalArgumentException("Seleccione un bloque reglamentario de 30 minutos.")
            )
        }
        if (motivo.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El motivo clínico de seguimiento es obligatorio.")
            )
        }

        val existentes = repository.citasFlow.value
        if (Cita.hayConflictoHorario(existentes, medicoNombre, fecha.trim(), horaInicio)) {
            return Result.failure(
                IllegalStateException(
                    "Conflicto de agenda: $medicoNombre ya tiene un paciente asignado el $fecha a las $horaInicio."
                )
            )
        }

        val nueva = Cita(
            id = "c-seg-" + System.currentTimeMillis().toString().takeLast(5),
            fecha = fecha.trim(),
            horaInicio = horaInicio,
            horaFin = Cita.calcularFin30Minutos(horaInicio),
            estado = "AGENDADA",
            motivo = "CONTROL MÉDICO: ${motivo.trim()}",
            paciente = PacienteInfo(
                id = paciente.id,
                nombreCompleto = paciente.nombreCompleto,
                numeroExpediente = paciente.numeroExpediente
                    ?: Paciente.generarNumeroExpediente(paciente.nombreCompleto),
                dui = paciente.dui,
                sexo = paciente.sexo,
                fechaNacimiento = paciente.fechaNacimiento,
                alergias = paciente.alergias,
                tipoSangre = paciente.tipoSangre,
                protegido = paciente.protegido
            ),
            consultorio = ConsultorioInfo("cs-1", especialidad, "Box 03"),
            medicoNombre = medicoNombre,
            especialidad = especialidad
        )
        repository.agendarCitaSeguimiento(nueva)
        return Result.success(nueva)
    }
}
