package com.cmo.hospital.features.recepcionista.pacientes.domain.repository

import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.core.network.dto.CodigoActivacion

/**
 * Datos de filiación que recepción registra o corrige.
 * Los datos clínicos (tipo de sangre, alergias) solo se envían al crear:
 * recepción no puede leerlos de vuelta, así que no los edita.
 */
data class PacienteForm(
    val nombreCompleto: String,
    val fechaNacimiento: String,
    val sexo: String,
    val dui: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val tipoSangre: String? = null,
    val alergias: String? = null
)

/** Dato del paciente rechazado por la validación local o por la API (422). */
typealias PacienteInvalidoException = ApiValidationException

interface PacientesRepository {
    suspend fun buscar(query: String?): Result<List<Paciente>>
    suspend fun crear(form: PacienteForm): Result<Paciente>
    suspend fun actualizar(id: String, form: PacienteForm): Result<Paciente>
    suspend fun generarCodigoActivacion(id: String): Result<CodigoActivacion>
}
