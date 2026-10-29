package com.cmo.hospital.features.doctor.inicio.domain.repository

import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import kotlinx.coroutines.flow.StateFlow

interface InicioDoctorRepository {
    val citasFlow: StateFlow<List<Cita>>
    val consultaActivaFlow: StateFlow<ConsultaActiva>
    val expedientesFlow: StateFlow<List<ExpedienteDetalle>>

    suspend fun sincronizarPanelMedico()
    suspend fun iniciarORetomarConsulta(citaId: String): Result<ConsultaActiva>
    fun solicitarAperturaExpediente(numeroExpediente: String?)
}
