package com.cmo.hospital.features.recepcionista.ambulancia.domain.repository

import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.PersonalGuardia
import kotlinx.coroutines.flow.StateFlow

interface AmbulanciaGuardiaRepository {
    val ambulanciaFlow: StateFlow<AmbulanciaRecurso>
    fun getPersonalGuardia(): List<PersonalGuardia>
    fun cambiarEstadoAmbulancia(nuevoEstado: String)
    suspend fun solicitarDespacho(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String
    )
}
