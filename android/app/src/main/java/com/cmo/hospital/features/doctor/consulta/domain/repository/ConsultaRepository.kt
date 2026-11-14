package com.cmo.hospital.features.doctor.consulta.domain.repository

import com.cmo.hospital.core.domain.model.BloqueAgenda
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.EspecialidadCatalogo
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.ProgramacionCita
import com.cmo.hospital.core.domain.model.RecetaItem
import com.cmo.hospital.core.domain.model.SignosVitales
import kotlinx.coroutines.flow.StateFlow

interface ConsultaRepository {
    val consultaActivaFlow: StateFlow<ConsultaActiva>
    val citasFlow: StateFlow<List<Cita>>

    fun actualizarSegundos(segundos: Int)
    /** Guarda los signos en la cita de la consulta abierta (PUT /citas/{id}/signos-vitales). */
    suspend fun guardarSignos(signos: SignosVitales): Result<Unit>
    fun agregarCie10(codigo: String, descripcion: String)
    fun eliminarCie10(codigo: String)
    fun actualizarAnamnesis(texto: String)
    suspend fun agregarReceta(receta: RecetaItem): Result<RecetaItem>
    suspend fun eliminarReceta(recetaId: String): Result<Unit>
    suspend fun guardarCambios(): Result<Unit>
    suspend fun finalizarConsulta(): Result<String>
    /** Pacientes que atiende el médico (GET /pacientes ya filtra por tratante). */
    suspend fun pacientesTratados(): Result<List<Paciente>>
    suspend fun especialidades(): Result<List<EspecialidadCatalogo>>
    suspend fun disponibilidad(idMedico: String?, fecha: String, idEspecialidad: String? = null): Result<List<BloqueAgenda>>
    /** POST /citas como médico: puede agendar seguimiento o referencia por especialidad. */
    suspend fun agendarSeguimiento(idPaciente: String, programacion: ProgramacionCita, motivo: String): Result<Cita>
}
