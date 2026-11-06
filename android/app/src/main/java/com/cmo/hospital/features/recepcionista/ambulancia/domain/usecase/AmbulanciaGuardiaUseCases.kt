package com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase

import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.MedicoDeTurno
import com.cmo.hospital.core.domain.model.ReporteAmbulancia
import com.cmo.hospital.core.domain.model.ServicioAmbulanciaDetalle
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.core.realtime.EventoTiempoReal
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import kotlinx.coroutines.flow.Flow

/** Prioridades y estados del paciente que acepta la API. */
object CatalogoAmbulancia {
    val PRIORIDADES = listOf("BAJA", "MEDIA", "ALTA", "CRITICA")
    val ESTADOS_PACIENTE = listOf("ESTABLE", "DELICADO", "CRITICO")

    fun etiqueta(valor: String): String = when (valor) {
        "CRITICA" -> "Crítica"
        "CRITICO" -> "Crítico"
        "EN_SERVICIO" -> "En servicio"
        "EN_MANTENIMIENTO" -> "En mantenimiento"
        "EN_CURSO" -> "En curso"
        else -> valor.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/** Estado actual, bitácora reciente y médicos de turno, en paralelo para la pantalla. */
data class PanelAmbulancia(
    val ambulancia: AmbulanciaRecurso,
    val bitacora: List<ServicioAmbulanciaDetalle>,
    val medicosDeTurno: List<MedicoDeTurno>
)

class CargarPanelAmbulanciaUseCase(private val repository: AmbulanciaGuardiaRepository) {
    /** La ambulancia es obligatoria; bitácora y turnos se muestran vacíos si fallan. */
    suspend operator fun invoke(): Result<PanelAmbulancia> {
        val ambulancia = repository.ambulancia().getOrElse { return Result.failure(it) }
        return Result.success(
            PanelAmbulancia(
                ambulancia = ambulancia,
                bitacora = repository.bitacora().getOrDefault(emptyList()),
                medicosDeTurno = repository.medicosDeTurno().getOrDefault(emptyList())
            )
        )
    }
}

class SolicitarDespachoAmbulanciaUseCase(private val repository: AmbulanciaGuardiaRepository) {
    suspend operator fun invoke(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String?
    ): Result<ServicioAmbulanciaDetalle> {
        if (direccion.isBlank()) return Result.failure(ApiValidationException("direccion", "Ingrese la dirección exacta."))
        if (paciente.isBlank()) return Result.failure(ApiValidationException("paciente_nombre", "Ingrese el nombre o una referencia del paciente."))
        if (descripcion.isBlank()) return Result.failure(ApiValidationException("descripcion", "Describa la emergencia."))
        if (prioridad !in CatalogoAmbulancia.PRIORIDADES) {
            return Result.failure(ApiValidationException("prioridad", "Seleccione la prioridad."))
        }
        return repository.despachar(direccion.trim(), paciente.trim(), descripcion.trim(), prioridad!!)
    }
}

class FinalizarServicioAmbulanciaUseCase(private val repository: AmbulanciaGuardiaRepository) {
    suspend operator fun invoke(idServicio: String, notas: String?): Result<ServicioAmbulanciaDetalle> =
        repository.finalizar(idServicio, notas?.trim()?.ifBlank { null })
}

class EnviarReporteAmbulanciaUseCase(private val repository: AmbulanciaGuardiaRepository) {
    suspend operator fun invoke(
        idServicio: String,
        estadoPaciente: String?,
        observaciones: String?,
        minutosTexto: String
    ): Result<ReporteAmbulancia> {
        if (estadoPaciente !in CatalogoAmbulancia.ESTADOS_PACIENTE) {
            return Result.failure(ApiValidationException("estado_paciente", "Seleccione el estado del paciente."))
        }
        val minutos = minutosTexto.trim().takeIf { it.isNotEmpty() }?.let {
            it.toIntOrNull()?.takeIf { m -> m in 0..240 }
                ?: return Result.failure(ApiValidationException("minutos_llegada", "Los minutos deben estar entre 0 y 240."))
        }
        return repository.reportar(idServicio, estadoPaciente!!, observaciones?.trim(), minutos)
    }
}

class ObservarAmbulanciaEnVivoUseCase(private val repository: AmbulanciaGuardiaRepository) {
    operator fun invoke(): Flow<EventoTiempoReal> = repository.cambiosEnVivo()
}
