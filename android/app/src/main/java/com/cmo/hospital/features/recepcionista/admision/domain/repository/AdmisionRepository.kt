package com.cmo.hospital.features.recepcionista.admision.domain.repository

import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import kotlinx.coroutines.flow.StateFlow

/** Camas e ingresos hospitalarios de la API (GET /habitaciones, /ingresos). */
interface AdmisionRepository {
    val camasFlow: StateFlow<List<Cama>>

    fun obtenerCapacidadActual(): CapacidadHospitalaria
    fun obtenerHabitacionesDetalladas(): List<HabitacionDetalleUi>

    suspend fun refrescarCamas(): Result<List<Cama>>
    suspend fun medicos(): Result<List<User>>
    suspend fun buscarPacientes(texto: String): Result<List<Paciente>>

    suspend fun ingresar(idPaciente: String, cama: Cama, idMedico: String, motivo: String): Result<Unit>
    suspend fun darAlta(idIngreso: String): Result<Unit>
    suspend fun trasladar(idIngreso: String, idCamaDestino: String): Result<Unit>
}
