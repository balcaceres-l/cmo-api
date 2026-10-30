package com.cmo.hospital

import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.features.recepcionista.laboratorio.domain.repository.LaboratorioRecepcionRepository
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.AvanzarOrdenLaboratorioUseCase
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.FiltroLaboratorio
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.ListarOrdenesLaboratorioUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaboratorioRecepcionTest {

    private class FakeRepo : LaboratorioRecepcionRepository {
        val llamadas = mutableListOf<String>()
        override suspend fun listar(estado: String?): Result<List<OrdenLaboratorioItem>> {
            llamadas += "listar $estado"
            return Result.success(emptyList())
        }
        override suspend fun iniciar(ordenId: String): Result<OrdenLaboratorioItem> {
            llamadas += "iniciar $ordenId"
            return Result.success(OrdenLaboratorioItem(id = ordenId, examenNombre = "X", estado = OrdenLaboratorioItem.EN_PROCESO))
        }
        override suspend fun completar(ordenId: String): Result<OrdenLaboratorioItem> {
            llamadas += "completar $ordenId"
            return Result.success(OrdenLaboratorioItem(id = ordenId, examenNombre = "X", estado = OrdenLaboratorioItem.COMPLETADO))
        }
    }

    private fun orden(estado: String) = OrdenLaboratorioItem(id = "o1", examenNombre = "Hemograma completo", estado = estado)

    @Test
    fun testCadaOrdenAvanzaUnSoloPasoHaciaAdelante() = runTest {
        val repo = FakeRepo()
        val avanzar = AvanzarOrdenLaboratorioUseCase(repo)

        assertEquals(OrdenLaboratorioItem.EN_PROCESO, avanzar(orden(OrdenLaboratorioItem.PENDIENTE)).getOrThrow().estado)
        assertEquals(OrdenLaboratorioItem.COMPLETADO, avanzar(orden(OrdenLaboratorioItem.EN_PROCESO)).getOrThrow().estado)
        assertTrue(avanzar(orden(OrdenLaboratorioItem.COMPLETADO)).isFailure)
        assertTrue(avanzar(orden(OrdenLaboratorioItem.CANCELADA)).isFailure)
        assertEquals(listOf("iniciar o1", "completar o1"), repo.llamadas)

        assertEquals("Iniciar", AvanzarOrdenLaboratorioUseCase.accionPara(orden(OrdenLaboratorioItem.PENDIENTE)))
        assertEquals("Completar", AvanzarOrdenLaboratorioUseCase.accionPara(orden(OrdenLaboratorioItem.EN_PROCESO)))
        assertNull(AvanzarOrdenLaboratorioUseCase.accionPara(orden(OrdenLaboratorioItem.COMPLETADO)))
    }

    @Test
    fun testLosFiltrosConsultanElEstadoDeLaApi() = runTest {
        val repo = FakeRepo()
        val listar = ListarOrdenesLaboratorioUseCase(repo)
        FiltroLaboratorio.values().forEach { listar(it) }
        assertEquals(listOf("listar PENDIENTE", "listar EN_PROCESO", "listar COMPLETADO", "listar null"), repo.llamadas)
    }
}
