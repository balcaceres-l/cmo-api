package com.cmo.hospital.features.recepcionista.citas.domain.repository

import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.SignosVitales
import kotlinx.coroutines.flow.StateFlow

interface CitaRepository {
    val citasFlow: StateFlow<List<Cita>>
    val turnoEnBoxFlow: StateFlow<Cita?>

    suspend fun refreshCitas(): List<Cita>

    suspend fun agendarCita(
        especialidad: String,
        medicoNombre: String,
        pacienteNombre: String,
        dui: String,
        fechaNacimiento: String?,
        fecha: String,
        horaInicio: String,
        motivo: String,
        box: String
    ): Result<Cita>

    suspend fun marcarLlegada(citaId: String)
    suspend fun cancelarCita(citaId: String, motivo: String)

    suspend fun registrarTriajeOPasarAConsulta(
        citaId: String,
        signosVitales: SignosVitales?,
        pasarAConsulta: Boolean
    ): Result<String>

    fun reLlamarTurno(): Cita?
    fun siguienteTurno(): Cita?
}
