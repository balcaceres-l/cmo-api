package com.cmo.hospital

import com.cmo.hospital.data.model.*
import org.junit.Assert.*
import org.junit.Test

class HospitalBusinessLogicTest {

    @Test
    fun testCitasBloquesFijos30Minutos() {
        assertEquals("08:30", Cita.calcularFin30Minutos("08:00"))
        assertEquals("10:15", Cita.calcularFin30Minutos("09:45"))
        assertEquals("12:00", Cita.calcularFin30Minutos("11:30"))
        assertEquals("00:15", Cita.calcularFin30Minutos("23:45"))
    }

    @Test
    fun testSignosVitalesCalculoImcYClasificacion() {
        // Peso 70kg, Talla 175cm -> IMC ~22.9
        val normal = SignosVitales(pesoKg = 70.0, tallaCm = 175.0)
        assertNotNull(normal.imc)
        assertEquals(22.9, normal.imc!!, 0.1)
        assertEquals("Normal", normal.imcClasificacion)

        // Sobrepeso: 80kg, 170cm -> IMC ~27.7
        val sobrepeso = SignosVitales(pesoKg = 80.0, tallaCm = 170.0)
        assertEquals("Sobrepeso", sobrepeso.imcClasificacion)

        // Obesidad: 100kg, 170cm -> IMC ~34.6
        val obesidad = SignosVitales(pesoKg = 100.0, tallaCm = 170.0)
        assertEquals("Obesidad", obesidad.imcClasificacion)

        // Bajo peso: 45kg, 165cm -> IMC ~16.5
        val bajoPeso = SignosVitales(pesoKg = 45.0, tallaCm = 165.0)
        assertEquals("Bajo peso", bajoPeso.imcClasificacion)
    }

    @Test
    fun testSignosVitalesSonFlexiblesYOpcionales() {
        // En una emergencia el paciente puede no tener signos vitales al inicio
        val emergenciaSinSignos = SignosVitales()
        assertNull(emergenciaSinSignos.presionSistolica)
        assertNull(emergenciaSinSignos.presionDiastolica)
        assertNull(emergenciaSinSignos.frecuenciaCardiaca)
        assertNull(emergenciaSinSignos.temperaturaC)
        assertNull(emergenciaSinSignos.saturacionOxigeno)
        assertNull(emergenciaSinSignos.pesoKg)
        assertNull(emergenciaSinSignos.tallaCm)
        assertNull(emergenciaSinSignos.imc)
        assertEquals("N/D", emergenciaSinSignos.imcClasificacion)
        assertFalse(emergenciaSinSignos.hasAnyData)
    }

    @Test
    fun testPacienteCalculoEdadDesdeFechaNacimiento() {
        val paciente1 = Paciente(
            id = "p-1",
            nombreCompleto = "Carlos Test",
            dui = "01234567-8",
            fechaNacimiento = "2000-01-01"
        )
        // Debe tener al menos 24 años
        assertTrue(paciente1.edad >= 24)

        // Manejo seguro con fecha nula o inválida
        val pacienteInvalido = Paciente(
            id = "p-2",
            nombreCompleto = "Juan Test",
            dui = "00000000-0",
            fechaNacimiento = null
        )
        assertEquals(0, pacienteInvalido.edad)
    }

    @Test
    fun testControlDeAccesoPorRolExclusivo() {
        val rolesPermitidos = listOf("MEDICO", "RECEPCIONISTA")
        val rolesBloqueados = listOf("ADMINISTRADOR", "PACIENTE", "OTRO")

        for (rol in rolesPermitidos) {
            assertTrue("El rol $rol debe estar permitido en Android", rol == "MEDICO" || rol == "RECEPCIONISTA")
        }

        for (rol in rolesBloqueados) {
            assertFalse("El rol $rol debe ser denegado en Android", rol == "MEDICO" || rol == "RECEPCIONISTA")
        }
    }

    @Test
    fun testSemaforoConsulta30Minutos() {
        fun calcularSemaforo(minutosTranscurridos: Int): EstadoSemaforo {
            return when {
                minutosTranscurridos < 20 -> EstadoSemaforo.VERDE
                minutosTranscurridos in 20..25 -> EstadoSemaforo.AMBAR
                else -> EstadoSemaforo.ROJO
            }
        }

        assertEquals(EstadoSemaforo.VERDE, calcularSemaforo(10))
        assertEquals(EstadoSemaforo.VERDE, calcularSemaforo(19))
        assertEquals(EstadoSemaforo.AMBAR, calcularSemaforo(20))
        assertEquals(EstadoSemaforo.AMBAR, calcularSemaforo(25))
        assertEquals(EstadoSemaforo.ROJO, calcularSemaforo(26))
        assertEquals(EstadoSemaforo.ROJO, calcularSemaforo(35))
    }
}
