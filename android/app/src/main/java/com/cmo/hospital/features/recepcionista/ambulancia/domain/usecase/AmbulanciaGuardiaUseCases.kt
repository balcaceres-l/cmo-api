package com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase

import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.PersonalGuardia
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import kotlinx.coroutines.flow.StateFlow

class GetAmbulanciaFlowUseCase(private val repository: AmbulanciaGuardiaRepository) {
    operator fun invoke(): StateFlow<AmbulanciaRecurso> = repository.ambulanciaFlow
}

class CambiarEstadoAmbulanciaUseCase(private val repository: AmbulanciaGuardiaRepository) {
    operator fun invoke(nuevoEstado: String) {
        repository.cambiarEstadoAmbulancia(nuevoEstado)
    }
}

class SolicitarDespachoAmbulanciaUseCase(private val repository: AmbulanciaGuardiaRepository) {
    suspend operator fun invoke(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String = "CRITICA"
    ): Result<Unit> {
        if (direccion.isBlank()) {
            return Result.failure(
                IllegalArgumentException("La dirección exacta de emergencia es obligatoria para despachar Alfa-1.")
            )
        }
        repository.solicitarDespacho(
            direccion = direccion.trim(),
            paciente = paciente.trim(),
            descripcion = descripcion.trim(),
            prioridad = prioridad
        )
        return Result.success(Unit)
    }
}

class GetPersonalGuardiaFiltradoUseCase(private val repository: AmbulanciaGuardiaRepository) {
    operator fun invoke(filtroRol: String = "TODOS"): List<PersonalGuardia> {
        val todos = repository.getPersonalGuardia()
        return if (filtroRol == "TODOS") {
            todos
        } else {
            todos.filter { it.rol.equals(filtroRol, ignoreCase = true) }
        }
    }
}
