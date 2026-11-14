package com.cmo.hospital.features.recepcionista.citas.domain.repository

import com.cmo.hospital.core.domain.model.BloqueAgenda
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultorioInfo
import com.cmo.hospital.core.domain.model.EspecialidadCatalogo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface CitaRepository {
    val citasFlow: StateFlow<List<Cita>>
    val turnoEnBoxFlow: StateFlow<Cita?>

    suspend fun refreshCitas(): List<Cita>

    // Catálogos de la agenda
    suspend fun especialidades(): Result<List<EspecialidadCatalogo>>
    suspend fun medicos(): Result<List<User>>
    suspend fun consultorios(): Result<List<ConsultorioInfo>>
    suspend fun disponibilidad(
        idMedico: String? = null,
        fecha: String,
        idEspecialidad: String? = null
    ): Result<List<BloqueAgenda>>
    suspend fun buscarPacientes(texto: String): Result<List<Paciente>>

    // Ciclo de la cita en la API
    suspend fun agendarCita(idPaciente: String, programacion: ProgramacionCita, motivo: String): Result<Cita>
    suspend fun confirmarCita(citaId: String, programacion: ProgramacionCita): Result<Cita>
    suspend fun reprogramarCita(citaId: String, programacion: ProgramacionCita): Result<Cita>
    suspend fun marcarLlegada(citaId: String): Result<Cita>
    suspend fun cancelarCita(citaId: String, motivo: String): Result<Cita>

    suspend fun registrarTriajeOPasarAConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ): Result<String>

    fun reLlamarTurno(): Cita?
    fun siguienteTurno(): Cita?
}
