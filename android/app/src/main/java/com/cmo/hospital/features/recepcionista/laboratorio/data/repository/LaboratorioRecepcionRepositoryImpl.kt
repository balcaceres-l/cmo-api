package com.cmo.hospital.features.recepcionista.laboratorio.data.repository

import com.cmo.hospital.core.data.mapper.aOrdenLaboratorio
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.llamarApi
import com.cmo.hospital.features.recepcionista.laboratorio.domain.repository.LaboratorioRecepcionRepository

class LaboratorioRecepcionRepositoryImpl(
    private val apiService: ApiService
) : LaboratorioRecepcionRepository {

    override suspend fun listar(estado: String?): Result<List<OrdenLaboratorioItem>> =
        llamarApi { apiService.getOrdenesLaboratorio(estado = estado) }
            .map { pagina -> pagina.data.map { it.aOrdenLaboratorio() } }

    override suspend fun iniciar(ordenId: String): Result<OrdenLaboratorioItem> =
        llamarApi { apiService.iniciarOrdenLaboratorio(ordenId) }.map { it.aOrdenLaboratorio() }

    override suspend fun completar(ordenId: String): Result<OrdenLaboratorioItem> =
        llamarApi { apiService.completarOrdenLaboratorio(ordenId) }.map { it.aOrdenLaboratorio() }
}
