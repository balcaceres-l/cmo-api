package com.cmo.hospital.features.doctor.inicio.domain.usecase

import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.features.doctor.inicio.domain.repository.InicioDoctorRepository
import kotlinx.coroutines.flow.StateFlow

data class PacienteMedicoResumen(
    val paciente: Paciente,
    val ubicacionActual: String,
    val estadoUltimaCita: String?,
    val ultimoMotivo: String?,
    val alertaAlergia: String?
)

class ObserveCitasMedicoUseCase(private val repository: InicioDoctorRepository) {
    operator fun invoke(): StateFlow<List<Cita>> = repository.citasFlow
}

class ObserveConsultaEnCursoUseCase(private val repository: InicioDoctorRepository) {
    operator fun invoke(): StateFlow<ConsultaActiva> = repository.consultaActivaFlow
}

class ObserveExpedientesMedicoUseCase(private val repository: InicioDoctorRepository) {
    operator fun invoke(): StateFlow<List<ExpedienteDetalle>> = repository.expedientesFlow
}

class SincronizarPanelMedicoUseCase(private val repository: InicioDoctorRepository) {
    suspend operator fun invoke() {
        repository.sincronizarPanelMedico()
    }
}

class IniciarORetomarConsultaMedicoUseCase(private val repository: InicioDoctorRepository) {
    suspend operator fun invoke(citaId: String): Result<ConsultaActiva> {
        if (citaId.isBlank()) {
            return Result.failure(IllegalArgumentException("Identificador de cita inválido."))
        }
        return repository.iniciarORetomarConsulta(citaId)
    }
}

class AbrirExpedientePacienteUseCase(private val repository: InicioDoctorRepository) {
    operator fun invoke(numeroExpediente: String?) {
        repository.solicitarAperturaExpediente(numeroExpediente)
    }
}

class ConstruirPacientesMedicoUseCase {
    operator fun invoke(
        citas: List<Cita>,
        expedientes: List<ExpedienteDetalle>,
        query: String
    ): List<PacienteMedicoResumen> {
        val mapaPorClave = linkedMapOf<String, PacienteMedicoResumen>()

        // 1. Pacientes presentes en citas del médico
        citas.forEach { cita ->
            val pInfo = cita.paciente ?: return@forEach
            val expCodigo = pInfo.numeroExpediente
                ?: Paciente.generarNumeroExpediente(pInfo.nombreCompleto)
            val clave = expCodigo.uppercase()
            if (!mapaPorClave.containsKey(clave)) {
                val fichaExp = expedientes.firstOrNull {
                    it.paciente.numeroExpediente.equals(expCodigo, ignoreCase = true) ||
                        (!pInfo.id.isNullOrBlank() && it.paciente.id == pInfo.id)
                }
                val paciente = fichaExp?.paciente ?: Paciente(
                    id = pInfo.id,
                    nombreCompleto = pInfo.nombreCompleto,
                    numeroExpediente = expCodigo,
                    dui = pInfo.dui,
                    sexo = pInfo.sexo,
                    fechaNacimiento = pInfo.fechaNacimiento,
                    telefono = pInfo.telefono,
                    alergias = pInfo.alergias,
                    tipoSangre = pInfo.tipoSangre,
                    protegido = pInfo.protegido
                )
                val alergia = (fichaExp?.alergiasCriticas.orEmpty() + listOfNotNull(paciente.alergias))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" • ")
                    .ifBlank { null }

                mapaPorClave[clave] = PacienteMedicoResumen(
                    paciente = paciente,
                    ubicacionActual = fichaExp?.cama
                        ?: (cita.consultorio?.codigo ?: "Consulta Externa"),
                    estadoUltimaCita = cita.estado,
                    ultimoMotivo = cita.motivo,
                    alertaAlergia = alergia
                )
            }
        }

        // 2. Pacientes adicionales con expediente clínico registrado
        expedientes.forEach { exp ->
            val expCodigo = exp.paciente.numeroExpediente
                ?: Paciente.generarNumeroExpediente(exp.paciente.nombreCompleto)
            val clave = expCodigo.uppercase()
            if (!mapaPorClave.containsKey(clave)) {
                val alergia = (exp.alergiasCriticas + listOfNotNull(exp.paciente.alergias))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" • ")
                    .ifBlank { null }
                mapaPorClave[clave] = PacienteMedicoResumen(
                    paciente = exp.paciente.copy(numeroExpediente = expCodigo),
                    ubicacionActual = exp.cama ?: "Hospitalización / HCE",
                    estadoUltimaCita = null,
                    ultimoMotivo = exp.atencionesPrevias.firstOrNull()?.diagnostico,
                    alertaAlergia = alergia
                )
            }
        }

        val todos = mapaPorClave.values.toList()
        val q = query.trim()
        if (q.isBlank()) return todos

        return todos.filter { item ->
            item.paciente.nombreCompleto.contains(q, ignoreCase = true) ||
                (item.paciente.numeroExpediente?.contains(q, ignoreCase = true) == true) ||
                (item.paciente.dui?.contains(q, ignoreCase = true) == true)
        }
    }
}
