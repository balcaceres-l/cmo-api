package com.cmo.hospital.features.doctor.expedientes.domain.repository

import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import com.cmo.hospital.core.domain.model.Paciente
import kotlinx.coroutines.flow.StateFlow

/**
 * Expedientes del médico en la API. GET /pacientes ya devuelve solo los
 * pacientes que el médico atiende; el detalle sale de /pacientes/{id}/expediente.
 */
interface ExpedientesRepository {
    /** Número de expediente pedido desde otra pantalla (p. ej. Inicio). */
    val expedienteSolicitadoFlow: StateFlow<String?>
    fun consumirExpedienteSolicitado()

    suspend fun buscarPacientes(texto: String): Result<List<Paciente>>
    suspend fun expediente(idPaciente: String): Result<ExpedienteDetalle>
    suspend fun guardarAntecedentes(idPaciente: String, personales: String?, familiares: String?): Result<Unit>
}
