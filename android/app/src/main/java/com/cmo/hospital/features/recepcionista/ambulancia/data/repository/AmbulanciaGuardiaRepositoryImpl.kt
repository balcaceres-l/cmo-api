package com.cmo.hospital.features.recepcionista.ambulancia.data.repository

import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.domain.model.MedicoDeTurno
import com.cmo.hospital.core.domain.model.ReporteAmbulancia
import com.cmo.hospital.core.domain.model.ServicioAmbulanciaDetalle
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.llamarApi
import com.cmo.hospital.core.realtime.EventoTiempoReal
import com.cmo.hospital.core.realtime.ReverbClient
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import kotlinx.coroutines.flow.Flow

class AmbulanciaGuardiaRepositoryImpl(
    private val api: ApiService,
    private val reverb: ReverbClient
) : AmbulanciaGuardiaRepository {

    override suspend fun ambulancia(): Result<AmbulanciaRecurso> = llamarApi { api.getAmbulancia() }

    override suspend fun bitacora(): Result<List<ServicioAmbulanciaDetalle>> =
        llamarApi { api.getBitacoraAmbulancia() }.map { it.data }

    override suspend fun medicosDeTurno(): Result<List<MedicoDeTurno>> = llamarApi { api.getMedicosDeTurno() }

    override suspend fun despachar(
        direccion: String,
        paciente: String,
        descripcion: String,
        prioridad: String
    ): Result<ServicioAmbulanciaDetalle> = llamarApi {
        api.despacharAmbulancia(
            mapOf(
                "direccion" to direccion,
                "paciente_nombre" to paciente,
                "descripcion" to descripcion,
                "prioridad" to prioridad
            )
        )
    }

    override suspend fun finalizar(idServicio: String, notas: String?): Result<ServicioAmbulanciaDetalle> =
        llamarApi { api.finalizarServicioAmbulancia(idServicio, mapOf("notas" to notas)) }

    override suspend fun reportar(
        idServicio: String,
        estadoPaciente: String,
        observaciones: String?,
        minutosLlegada: Int?
    ): Result<ReporteAmbulancia> = llamarApi {
        api.reportarDesdeAmbulancia(
            idServicio,
            buildMap {
                put("estado_paciente", estadoPaciente)
                if (!observaciones.isNullOrBlank()) put("observaciones", observaciones)
                if (minutosLlegada != null) put("minutos_llegada", minutosLlegada)
            }
        )
    }

    override fun cambiosEnVivo(): Flow<EventoTiempoReal> = reverb.canal("ambulancia")

    override fun reportesEnVivo(): Flow<EventoTiempoReal> =
        reverb.canalPrivado("personal") { socketId, canal ->
            val res = api.autorizarCanal(mapOf("socket_id" to socketId, "channel_name" to canal))
            if (res.isSuccessful) res.body()?.get("auth") else null
        }
}
