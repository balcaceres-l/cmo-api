package com.cmo.hospital

import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.dto.CodigoActivacion
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteInvalidoException
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacientesRepository
import com.cmo.hospital.features.recepcionista.pacientes.presentation.DuiTextWatcher
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GuardarPacienteUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PacientesCrudTest {

    private class FakeRepo : PacientesRepository {
        var creados = mutableListOf<PacienteForm>()
        var actualizados = mutableListOf<Pair<String, PacienteForm>>()

        override suspend fun buscar(query: String?) = Result.success(emptyList<Paciente>())
        override suspend fun crear(form: PacienteForm): Result<Paciente> {
            creados += form
            return Result.success(Paciente(id = "p1", nombreCompleto = form.nombreCompleto, numeroExpediente = "EXP-2026-000001"))
        }
        override suspend fun actualizar(id: String, form: PacienteForm): Result<Paciente> {
            actualizados += id to form
            return Result.success(Paciente(id = id, nombreCompleto = form.nombreCompleto))
        }
        override suspend fun generarCodigoActivacion(id: String) = Result.success(CodigoActivacion("123456", null))
    }

    private val valido = PacienteForm(nombreCompleto = "Ana López", fechaNacimiento = "1985-04-12", sexo = "F", dui = "01234567-8")

    @Test
    fun testValidacionFormularioPaciente() {
        val hoy = LocalDate.of(2026, 10, 30)
        assertNull(GuardarPacienteUseCase.validar(valido, hoy))
        assertNull(GuardarPacienteUseCase.validar(valido.copy(dui = null), hoy))
        assertEquals("Ingrese el nombre completo del paciente.", GuardarPacienteUseCase.validar(valido.copy(nombreCompleto = " "), hoy))
        assertTrue(GuardarPacienteUseCase.validar(valido.copy(fechaNacimiento = "12/04/1985"), hoy)!!.contains("AAAA-MM-DD"))
        assertTrue(GuardarPacienteUseCase.validar(valido.copy(fechaNacimiento = "2026-10-30"), hoy)!!.contains("anterior a hoy"))
        assertTrue(GuardarPacienteUseCase.validar(valido.copy(sexo = "X"), hoy)!!.contains("sexo"))
        assertTrue(GuardarPacienteUseCase.validar(valido.copy(dui = "1234567-8"), hoy)!!.contains("DUI"))
    }

    @Test
    fun testGuardarCreaOActualizaYNormaliza() = runTest {
        val repo = FakeRepo()
        val guardar = GuardarPacienteUseCase(repo)

        val creado = guardar(null, valido.copy(nombreCompleto = "  Ana López ", telefono = " ", tipoSangre = "o+"))
        assertTrue(creado.isSuccess)
        assertEquals("Ana López", repo.creados.single().nombreCompleto)
        assertNull(repo.creados.single().telefono)
        assertEquals("O+", repo.creados.single().tipoSangre)

        assertTrue(guardar("p1", valido.copy(telefono = "7777-0000")).isSuccess)
        assertEquals("p1", repo.actualizados.single().first)

        // Un formulario inválido no llega a la API y dice qué campo marcar.
        val invalido = guardar(null, valido.copy(dui = "0123"))
        assertEquals("dui", (invalido.exceptionOrNull() as PacienteInvalidoException).campo)
        assertEquals(1, repo.creados.size)
    }

    @Test
    fun testCampoInvalidoParaMarcarEnFormulario() {
        val hoy = LocalDate.of(2026, 10, 30)
        assertEquals("nombre_completo", GuardarPacienteUseCase.validarCampo(valido.copy(nombreCompleto = ""), hoy)?.first)
        assertEquals("fecha_nacimiento", GuardarPacienteUseCase.validarCampo(valido.copy(fechaNacimiento = ""), hoy)?.first)
        assertEquals("sexo", GuardarPacienteUseCase.validarCampo(valido.copy(sexo = "X"), hoy)?.first)
        assertNull(GuardarPacienteUseCase.validarCampo(valido, hoy))
    }

    @Test
    fun testDuiSeFormateaSinEscribirElGuion() {
        assertEquals("0123", DuiTextWatcher.formatear("0123"))
        assertEquals("01234567", DuiTextWatcher.formatear("01234567"))
        assertEquals("01234567-8", DuiTextWatcher.formatear("012345678"))
        assertEquals("01234567-8", DuiTextWatcher.formatear("01234567-8"))
        // Ignora caracteres extra y dígitos de más.
        assertEquals("01234567-8", DuiTextWatcher.formatear("0123 4567-89"))
    }

    @Test
    fun testMenorDeEdadDeshabilitaDuiYExigeDatosDelResponsable() {
        val hoy = LocalDate.of(2026, 11, 11)
        val menorBase = PacienteForm(
            nombreCompleto = "Mateo Pérez",
            fechaNacimiento = "2016-05-10", // 10 años (menor de edad)
            sexo = "M",
            dui = null,
            telefono = "7123-4567",
            direccion = "Col. Escalón, San Salvador",
            responsableNombre = "Claudia Pérez",
            responsableFechaNacimiento = "1988-03-15",
            responsableTipoDocumento = "DUI",
            responsableDocumento = "01234567-8",
            responsableTelefono = "7123-4567",
            responsableParentesco = "Madre",
            exigirDatosCompletos = true
        )

        // Válido con datos de responsable y sin DUI del menor
        assertNull(GuardarPacienteUseCase.validarCampo(menorBase, hoy))

        // Si se intenta enviar DUI en el menor, falla en el campo "dui"
        assertEquals(
            "dui",
            GuardarPacienteUseCase.validarCampo(menorBase.copy(dui = "01234567-8"), hoy)?.first
        )

        // Si falta cualquiera de los datos del responsable, marca el campo correspondiente
        assertEquals(
            "responsable_nombre",
            GuardarPacienteUseCase.validarCampo(menorBase.copy(responsableNombre = ""), hoy)?.first
        )
        assertEquals(
            "responsable_fecha_nacimiento",
            GuardarPacienteUseCase.validarCampo(menorBase.copy(responsableFechaNacimiento = "2012-01-01"), hoy)?.first
        )
        assertEquals(
            "responsable_documento",
            GuardarPacienteUseCase.validarCampo(menorBase.copy(responsableDocumento = "123"), hoy)?.first
        )
        assertEquals(
            "responsable_telefono",
            GuardarPacienteUseCase.validarCampo(menorBase.copy(responsableTelefono = "123"), hoy)?.first
        )
        assertEquals(
            "responsable_parentesco",
            GuardarPacienteUseCase.validarCampo(menorBase.copy(responsableParentesco = ""), hoy)?.first
        )

        // Soporta pasaporte tanto en adulto como en responsable
        assertNull(
            GuardarPacienteUseCase.validarCampo(
                menorBase.copy(responsableTipoDocumento = "PASAPORTE", responsableDocumento = "A0987654"),
                hoy
            )
        )
    }
}
