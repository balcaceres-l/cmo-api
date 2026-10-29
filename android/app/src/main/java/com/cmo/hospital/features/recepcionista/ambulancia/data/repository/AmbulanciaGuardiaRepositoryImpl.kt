package com.cmo.hospital.features.recepcionista.ambulancia.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.PersonalGuardia
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import kotlinx.coroutines.flow.StateFlow

class AmbulanciaGuardiaRepositoryImpl(
    private val dataSource: HospitalClinicalDataSource
) : AmbulanciaGuardiaRepository {

    override val ambulanciaFlow: StateFlow<AmbulanciaRecurso> = dataSource.ambulanciaState

    override fun getPersonalGuardia(): List<PersonalGuardia> = dataSource.getPersonalGuardia()

    override fun cambiarEstadoAmbulancia(nuevoEstado: String) {
        dataSource.cambiarEstadoAmbulancia(nuevoEstado)
    }

    override suspend fun solicitarDespacho(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String
    ) {
        dataSource.despacharAmbulancia(direccion, paciente, descripcion, prioridad)
    }
}
