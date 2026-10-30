package com.cmo.hospital.features.doctor.consulta.domain.repository

import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.RecetaItem
import com.cmo.hospital.core.domain.model.SignosVitales
import kotlinx.coroutines.flow.StateFlow

interface ConsultaRepository {
    val consultaActivaFlow: StateFlow<ConsultaActiva>
    val citasFlow: StateFlow<List<Cita>>

    fun actualizarSegundos(segundos: Int)
    fun actualizarSignos(signos: SignosVitales)
    fun agregarCie10(codigo: String, descripcion: String)
    fun eliminarCie10(codigo: String)
    fun actualizarAnamnesis(texto: String)
    suspend fun agregarReceta(receta: RecetaItem): Result<RecetaItem>
    suspend fun eliminarReceta(recetaId: String): Result<Unit>
    suspend fun guardarCambios(): Result<Unit>
    suspend fun finalizarConsulta(): Result<String>
    suspend fun agendarCitaSeguimiento(cita: Cita)
}
