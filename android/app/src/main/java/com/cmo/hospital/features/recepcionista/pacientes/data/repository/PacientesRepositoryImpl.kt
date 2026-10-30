package com.cmo.hospital.features.recepcionista.pacientes.data.repository

import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.dto.CodigoActivacion
import com.cmo.hospital.core.network.llamarApi
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacientesRepository

/**
 * CRUD de pacientes contra la API (sin datos locales de respaldo): si la API
 * rechaza la operación, el error llega a la pantalla para que recepción lo vea.
 */
class PacientesRepositoryImpl(
    private val apiService: ApiService
) : PacientesRepository {

    override suspend fun buscar(query: String?): Result<List<Paciente>> =
        llamarApi { apiService.getPacientes(query?.trim()?.ifBlank { null }, perPage = 50) }
            .map { it.data }

    override suspend fun crear(form: PacienteForm): Result<Paciente> =
        llamarApi { apiService.registrarPaciente(form.toBody(incluirClinicos = true)) }

    override suspend fun actualizar(id: String, form: PacienteForm): Result<Paciente> =
        llamarApi { apiService.actualizarPaciente(id, form.toBody(incluirClinicos = false)) }

    override suspend fun generarCodigoActivacion(id: String): Result<CodigoActivacion> =
        llamarApi { apiService.generarCodigoActivacion(id) }

    private fun PacienteForm.toBody(incluirClinicos: Boolean): Map<String, Any?> = buildMap {
        put("nombre_completo", nombreCompleto)
        put("fecha_nacimiento", fechaNacimiento)
        put("sexo", sexo)
        put("dui", dui)
        put("telefono", telefono)
        put("direccion", direccion)
        if (incluirClinicos) {
            put("tipo_sangre", tipoSangre)
            put("alergias", alergias)
        }
    }
}
