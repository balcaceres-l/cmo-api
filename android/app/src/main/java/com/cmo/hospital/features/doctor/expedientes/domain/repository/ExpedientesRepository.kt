package com.cmo.hospital.features.doctor.expedientes.domain.repository

import com.cmo.hospital.core.domain.model.ExpedienteDetalle
import kotlinx.coroutines.flow.StateFlow

interface ExpedientesRepository {
    val expedientesFlow: StateFlow<List<ExpedienteDetalle>>
    val expedienteSolicitadoFlow: StateFlow<String?>
    fun consumirExpedienteSolicitado()
}
