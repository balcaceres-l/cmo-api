package com.cmo.hospital.features.recepcionista.ambulancia.domain.repository

import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.MedicoDeTurno
import com.cmo.hospital.core.domain.model.ReporteAmbulancia
import com.cmo.hospital.core.domain.model.ServicioAmbulanciaDetalle
import com.cmo.hospital.core.realtime.EventoTiempoReal
import kotlinx.coroutines.flow.Flow

/** Ambulancia del hospital contra la API (y avisos en vivo por Reverb). */
interface AmbulanciaGuardiaRepository {
    suspend fun ambulancia(): Result<AmbulanciaRecurso>
    suspend fun bitacora(): Result<List<ServicioAmbulanciaDetalle>>
    suspend fun medicosDeTurno(): Result<List<MedicoDeTurno>>

    suspend fun despachar(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String
    ): Result<ServicioAmbulanciaDetalle>

    suspend fun finalizar(idServicio: String, notas: String?): Result<ServicioAmbulanciaDetalle>

    suspend fun reportar(
        idServicio: String,
        estadoPaciente: String,
        observaciones: String?,
        minutosLlegada: Int?
    ): Result<ReporteAmbulancia>

    /** Eventos del canal público `ambulancia`; termina si se cae la conexión. */
    fun cambiosEnVivo(): Flow<EventoTiempoReal>
}
