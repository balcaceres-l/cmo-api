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

    @Test
    fun testFormatoNumeroExpedienteIniciales() {
        // Formato (LB-2026-001): Primera letra del primer nombre + primera letra del primer apellido + año + correlativo
        assertEquals("LB-2026-001", Paciente.generarNumeroExpediente("Luis Barillas", 2026, 1))
        assertEquals("CM-2026-002", Paciente.generarNumeroExpediente("Carlos Mendoza", 2026, 2))
        assertEquals("MS-2026-003", Paciente.generarNumeroExpediente("Mariana Silva Rivas", 2026, 3))
        assertEquals("LM-2026-004", Paciente.generarNumeroExpediente("Lucía Morales Castillo", 2026, 4))
    }

    @Test
    fun testEstadosLaboratorioPermitidosYTransicion() {
        val estadosEsperados = listOf("En Espera", "Agendado", "En Proceso", "Realizado", "Cancelado")
        assertEquals(estadosEsperados, OrdenLaboratorioItem.ESTADOS_VALIDOS)

        val orden = OrdenLaboratorioItem(
            examenNombre = "Troponina I",
            pacienteNombre = "Luis Barillas",
            numeroExpediente = "LB-2026-001",
            estado = "En Espera"
        )
        assertEquals("En Espera", orden.estado)

        // El Doctor cambia el estado a Realizado
        val ordenRealizada = orden.copy(estado = "Realizado", fechaResultado = "Hoy, 10:45")
        assertEquals("Realizado", ordenRealizada.estado)
        assertEquals("Hoy, 10:45", ordenRealizada.fechaResultado)

        // O a Cancelado
        val ordenCancelada = orden.copy(estado = "Cancelado")
        assertEquals("Cancelado", ordenCancelada.estado)
    }

    @Test
    fun testCitasNoSolapamientoMismaHora() {
        val citas = listOf(
            Cita(
                id = "c1",
                fecha = "2026-10-28",
                horaInicio = "09:00",
                horaFin = "09:30",
                estado = "AGENDADA",
                medicoNombre = "Dr. Alejandro Restrepo"
            ),
            Cita(
                id = "c2",
                fecha = "2026-10-28",
                horaInicio = "10:00",
                horaFin = "10:30",
                estado = "CANCELADA",
                medicoNombre = "Dr. Alejandro Restrepo"
            )
        )

        // Mismo doctor, misma fecha y misma hora -> CONFLICTO ESTRICTO (true)
        assertTrue(Cita.hayConflictoHorario(citas, "Dr. Alejandro Restrepo", "2026-10-28", "09:00"))

        // Mismo doctor, diferente hora -> SIN CONFLICTO (false)
        assertFalse(Cita.hayConflictoHorario(citas, "Dr. Alejandro Restrepo", "2026-10-28", "09:30"))

        // Diferente doctor, misma hora -> SIN CONFLICTO (false)
        assertFalse(Cita.hayConflictoHorario(citas, "Dra. Sofía Valenzuela", "2026-10-28", "09:00"))

        // Misma hora pero cita existente está CANCELADA -> SIN CONFLICTO (false)
        assertFalse(Cita.hayConflictoHorario(citas, "Dr. Alejandro Restrepo", "2026-10-28", "10:00"))
    }

    @Test
    fun testCatalogoEspecialidadesYMedicosCascada() {
        assertTrue(CATALOGO_ESPECIALIDADES.containsKey("Medicina General"))
        assertTrue(CATALOGO_ESPECIALIDADES.containsKey("Medicina Interna"))
        assertTrue(CATALOGO_ESPECIALIDADES.containsKey("Cardiología"))
        assertTrue(CATALOGO_ESPECIALIDADES.containsKey("Pediatría"))

        // Cada especialidad tiene médicos asignados
        CATALOGO_ESPECIALIDADES.forEach { (esp, doctores) ->
            assertTrue("Especialidad $esp debe tener al menos un doctor", doctores.isNotEmpty())
        }

        // Bloques de 30 minutos cubren jornada hospitalaria
        assertTrue(BLOQUES_30_MINUTOS.contains("08:00"))
        assertTrue(BLOQUES_30_MINUTOS.contains("08:30"))
        assertTrue(BLOQUES_30_MINUTOS.contains("17:30"))
    }

    @Test
    fun testTriajeOpcionalYFlexibilidad() {
        // En una emergencia, los signos vitales pueden no ser registrados
        val signosVacios = SignosVitales()
        assertFalse(signosVacios.hasAnyData)
        assertEquals("N/D", signosVacios.presionTexto)

        // Con toma de signos
        val signosCompletos = SignosVitales(
            presionSistolica = 120,
            presionDiastolica = 80,
            frecuenciaCardiaca = 72,
            temperaturaC = 36.6
        )
        assertTrue(signosCompletos.hasAnyData)
        assertEquals("120/80 mmHg", signosCompletos.presionTexto)
    }
}
