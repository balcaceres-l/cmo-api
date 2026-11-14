package com.cmo.hospital.features.recepcionista.admision.data.repository

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.features.recepcionista.admision.domain.repository.AdmisionRepository
import kotlinx.coroutines.flow.StateFlow

class AdmisionRepositoryImpl(
    private val dataSource: HospitalClinicalDataSource
) : AdmisionRepository {

    override val camasFlow: StateFlow<List<Cama>> = dataSource.camasState

    override fun obtenerCapacidadActual(): CapacidadHospitalaria =
        dataSource.construirCapacidadHospitalaria()

    override fun obtenerHabitacionesDetalladas(): List<HabitacionDetalleUi> =
        dataSource.construirHabitacionesDetalladas()

    override suspend fun refrescarCamas(): Result<List<Cama>> = dataSource.syncCamasFromRemote()

    override suspend fun medicos(): Result<List<User>> = dataSource.getMedicosAgenda()

    override suspend fun buscarPacientes(texto: String): Result<List<Paciente>> =
        dataSource.buscarPacientes(texto)

    override suspend fun ingresar(idPaciente: String, cama: Cama, idMedico: String, motivo: String): Result<Unit> =
        dataSource.ingresarPaciente(idPaciente, cama, idMedico, motivo)

    override suspend fun darAlta(idIngreso: String): Result<Unit> = dataSource.darAlta(idIngreso)

    override suspend fun trasladar(idIngreso: String, idCamaDestino: String): Result<Unit> =
        dataSource.trasladarPaciente(idIngreso, idCamaDestino)
}
