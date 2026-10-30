package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.features.doctor.inicio.data.repository.InicioDoctorRepositoryImpl
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ConstruirPacientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.IniciarORetomarConsultaMedicoUseCase
import com.cmo.hospital.features.doctor.laboratorio.data.repository.LaboratorioRepositoryImpl
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.features.recepcionista.admision.data.repository.AdmisionRepositoryImpl
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.GenerarBrazaleteQrUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.RegistrarIngresoPacienteUseCase
import kotlinx.coroutines.test.runTest
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
        val normal = SignosVitales(pesoKg = 70.0, tallaCm = 175.0)
        assertNotNull(normal.imc)
        assertEquals(22.9, normal.imc!!, 0.1)
        assertEquals("Normal", normal.imcClasificacion)

        val sobrepeso = SignosVitales(pesoKg = 80.0, tallaCm = 170.0)
        assertEquals("Sobrepeso", sobrepeso.imcClasificacion)

        val obesidad = SignosVitales(pesoKg = 100.0, tallaCm = 170.0)
        assertEquals("Obesidad", obesidad.imcClasificacion)

        val bajoPeso = SignosVitales(pesoKg = 45.0, tallaCm = 165.0)
        assertEquals("Bajo peso", bajoPeso.imcClasificacion)
    }

    @Test
    fun testSignosVitalesSonFlexiblesYOpcionales() {
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
        assertTrue(paciente1.edad >= 24)

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
            assertTrue(
                "El rol $rol debe estar permitido en Android",
                RoleAccessPolicy.isAllowedInMobileApp(rol)
            )
        }

        for (rol in rolesBloqueados) {
            assertFalse(
                "El rol $rol debe ser denegado en Android",
                RoleAccessPolicy.isAllowedInMobileApp(rol)
            )
        }
    }

    @Test
    fun testSemaforoConsulta30Minutos() {
        assertEquals(EstadoSemaforo.VERDE, EstadoSemaforo.calcularPorMinutos(10))
        assertEquals(EstadoSemaforo.VERDE, EstadoSemaforo.calcularPorMinutos(19))
        assertEquals(EstadoSemaforo.AMBAR, EstadoSemaforo.calcularPorMinutos(20))
        assertEquals(EstadoSemaforo.AMBAR, EstadoSemaforo.calcularPorMinutos(25))
        assertEquals(EstadoSemaforo.ROJO, EstadoSemaforo.calcularPorMinutos(26))
        assertEquals(EstadoSemaforo.ROJO, EstadoSemaforo.calcularPorMinutos(35))
    }

    @Test
    fun testFormatoNumeroExpedienteIniciales() {
        assertEquals("LB-2026-001", Paciente.generarNumeroExpediente("Luis Barillas", 2026, 1))
        assertEquals("CM-2026-002", Paciente.generarNumeroExpediente("Carlos Mendoza", 2026, 2))
        assertEquals("MS-2026-003", Paciente.generarNumeroExpediente("Mariana Silva Rivas", 2026, 3))
        assertEquals("LM-2026-004", Paciente.generarNumeroExpediente("Lucía Morales Castillo", 2026, 4))
    }

    @Test
    fun testEstadosLaboratorioPermitidosYTransicion() {
        // Estados de la rúbrica, traducidos desde los de la API.
        assertEquals(listOf("Pendiente", "En Proceso", "Completado", "Cancelada"), OrdenLaboratorioItem.ESTADOS_VALIDOS)
        assertEquals("Pendiente", OrdenLaboratorioItem.estadoDesdeApi("PENDIENTE"))
        assertEquals("En Proceso", OrdenLaboratorioItem.estadoDesdeApi("EN_PROCESO"))
        assertEquals("Completado", OrdenLaboratorioItem.estadoDesdeApi("COMPLETADO"))
        assertEquals("Cancelada", OrdenLaboratorioItem.estadoDesdeApi("CANCELADA"))

        // El médico solo puede cancelar mientras el laboratorio no la inició.
        val orden = OrdenLaboratorioItem(examenNombre = "Hemograma completo")
        assertTrue(orden.cancelable)
        assertFalse(orden.copy(estado = OrdenLaboratorioItem.EN_PROCESO).cancelable)
        assertFalse(orden.copy(estado = OrdenLaboratorioItem.COMPLETADO).cancelable)
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

        assertTrue(Cita.hayConflictoHorario(citas, "Dr. Alejandro Restrepo", "2026-10-28", "09:00"))
        assertFalse(Cita.hayConflictoHorario(citas, "Dr. Alejandro Restrepo", "2026-10-28", "09:30"))
        assertFalse(Cita.hayConflictoHorario(citas, "Dra. Sofía Valenzuela", "2026-10-28", "09:00"))
        assertFalse(Cita.hayConflictoHorario(citas, "Dr. Alejandro Restrepo", "2026-10-28", "10:00"))
    }

    @Test
    fun testCatalogoEspecialidadesYMedicosCascada() {
        assertTrue(CatalogoClinico.CATALOGO_ESPECIALIDADES.containsKey("Medicina General"))
        assertTrue(CatalogoClinico.CATALOGO_ESPECIALIDADES.containsKey("Medicina Interna"))
        assertTrue(CatalogoClinico.CATALOGO_ESPECIALIDADES.containsKey("Cardiología"))
        assertTrue(CatalogoClinico.CATALOGO_ESPECIALIDADES.containsKey("Pediatría"))

        CatalogoClinico.CATALOGO_ESPECIALIDADES.forEach { (esp, doctores) ->
            assertTrue("Especialidad $esp debe tener al menos un doctor", doctores.isNotEmpty())
        }

        assertTrue(CatalogoClinico.BLOQUES_30_MINUTOS.contains("08:00"))
        assertTrue(CatalogoClinico.BLOQUES_30_MINUTOS.contains("08:30"))
        assertTrue(CatalogoClinico.BLOQUES_30_MINUTOS.contains("17:30"))
    }

    @Test
    fun testTriajeOpcionalYFlexibilidad() {
        val signosVacios = SignosVitales()
        assertFalse(signosVacios.hasAnyData)
        assertEquals("N/D", signosVacios.presionTexto)

        val signosCompletos = SignosVitales(
            presionSistolica = 120,
            presionDiastolica = 80,
            frecuenciaCardiaca = 72,
            temperaturaC = 36.6
        )
        assertTrue(signosCompletos.hasAnyData)
        assertEquals("120/80 mmHg", signosCompletos.presionTexto)
    }

    // ─── Nuevos Tests Clean Architecture & Prototipo HUC Móvil ───────────────

    @Test
    fun testTriageManchester4NivelesYAnonimizacionLey19628() {
        assertEquals(4, NivelTriageManchester.entries.size)
        assertEquals(NivelTriageManchester.NIVEL_I_ROJO, NivelTriageManchester.fromNivel(1))
        assertEquals("Inmediato (0 min)", NivelTriageManchester.NIVEL_I_ROJO.tiempoRespuesta)
        assertEquals("Estándar (120 min)", NivelTriageManchester.NIVEL_IV_VERDE.tiempoRespuesta)

        val anonimo = Paciente.anonimizarNombre("Carlos Mendoza")
        assertEquals("C. M**** (Protegido)", anonimo)

        val generarQr = GenerarBrazaleteQrUseCase()
        val qrProtegido = generarQr(
            dni = "17.489.201-3",
            nombres = "Francisca",
            apellidos = "Navarro",
            codigoCama = "VIP-302",
            esEmergenciaDirecta = false,
            nivelTriage = NivelTriageManchester.NIVEL_II_NARANJA,
            protegidoLey19628 = true
        )
        assertTrue(qrProtegido.isSuccess)
        assertEquals("F. N**** (Protegido)", qrProtegido.getOrThrow().pacienteNombre)
        assertEquals("VIP-302", qrProtegido.getOrThrow().codigoCama)
    }

    @Test
    fun testCapacidadEstricta17CamasYBloqueoSobrecupo() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val repo = AdmisionRepositoryImpl(dataSource)
        val useCase = RegistrarIngresoPacienteUseCase(repo)

        val capInicial = repo.obtenerCapacidadActual()
        assertEquals(17, capInicial.totalCamas)
        assertEquals(14, capInicial.ocupadas)
        assertEquals(3, capInicial.disponibles)
        assertEquals(5, capInicial.vipCamas.size)
        assertEquals(6, capInicial.salasSemiprivadas.size) // 6 salas * 2 camas (A/B) = 12 camas

        // Ingreso válido ocupando VIP-302
        val ingreso1 = useCase(
            dni = "17.489.201-3",
            nombres = "Francisca",
            apellidos = "Navarro",
            edadTexto = "34",
            prevision = "Fonasa Tramo D",
            contactoEmergencia = "+56 9 7654 3210",
            protegidoLey19628 = false,
            motivoIngreso = "Síndrome coronario en observación",
            nivelTriage = NivelTriageManchester.NIVEL_II_NARANJA,
            esEmergenciaDirecta = false,
            codigoCamaSeleccionada = "VIP-302",
            signosVitales = null
        )
        assertTrue(ingreso1.isSuccess)

        val capDespues = repo.obtenerCapacidadActual()
        assertEquals(15, capDespues.ocupadas)
        assertEquals(2, capDespues.disponibles)

        // Intentar reasignar la misma cama VIP-302 debe bloquearse por sobrecupo estricto
        val intentoDuplicado = useCase(
            dni = "12.111.222-3",
            nombres = "Pedro",
            apellidos = "Rojas",
            edadTexto = "50",
            prevision = "Isapre Colmena",
            contactoEmergencia = "",
            protegidoLey19628 = false,
            motivoIngreso = "Observación",
            nivelTriage = NivelTriageManchester.NIVEL_III_AMARILLO,
            esEmergenciaDirecta = false,
            codigoCamaSeleccionada = "VIP-302",
            signosVitales = null
        )
        assertTrue(intentoDuplicado.isFailure)
    }

    @Test
    fun testEmergenciaDirectaIniciaConsultaActivaDoctorEnCeroSegundos() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val repo = AdmisionRepositoryImpl(dataSource)
        val useCase = RegistrarIngresoPacienteUseCase(repo)

        val res = useCase(
            dni = "19.888.777-K",
            nombres = "Mateo",
            apellidos = "Contreras",
            edadTexto = "61",
            prevision = "Fonasa Tramo D",
            contactoEmergencia = "",
            protegidoLey19628 = true,
            motivoIngreso = "Dolor precordial agudo",
            nivelTriage = NivelTriageManchester.NIVEL_I_ROJO,
            esEmergenciaDirecta = true,
            codigoCamaSeleccionada = null,
            signosVitales = null
        )
        assertTrue(res.isSuccess)

        val consultaDoctor = dataSource.consultaActiva.value
        assertEquals("Mateo Contreras", consultaDoctor.paciente.nombreCompleto)
        assertEquals(0, consultaDoctor.segundosTranscurridos)
        assertEquals("00:00", consultaDoctor.relojFormateado)
        assertEquals(EstadoSemaforo.VERDE, consultaDoctor.estadoSemaforo)
        assertFalse(consultaDoctor.signosVitales.hasAnyData)
    }

    @Test
    fun testSinConsultaAbiertaNoSeEmitenOrdenes() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val emitirUseCase = EmitirOrdenLabUseCase(LaboratorioRepositoryImpl(dataSource))

        assertTrue(emitirUseCase(emptyList(), "RUTINA", "").isFailure)
        val res = emitirUseCase(listOf("Hemograma completo"), "RUTINA", "", dataSource.expedientes.value.first().paciente)
        assertTrue(res.exceptionOrNull()!!.message!!.contains("consulta abierta"))
    }

    @Test
    fun testFlujoPaginaPrincipalMedicoIniciarVolverYFinalizarConsulta() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val inicioRepo = InicioDoctorRepositoryImpl(dataSource)
        val iniciarUseCase = IniciarORetomarConsultaMedicoUseCase(inicioRepo)
        val construirPacientesUseCase = ConstruirPacientesMedicoUseCase()

        // 1. La página principal construye la lista completa de pacientes del médico (citas + expedientes)
        val todosPacientes = construirPacientesUseCase(
            citas = dataSource.citasState.value,
            expedientes = dataSource.expedientes.value,
            query = ""
        )
        assertTrue(todosPacientes.size >= 5)
        val filtradoLucia = construirPacientesUseCase(
            citas = dataSource.citasState.value,
            expedientes = dataSource.expedientes.value,
            query = "Lucía"
        )
        assertEquals(1, filtradoLucia.size)
        assertEquals("LM-2026-002", filtradoLucia.first().paciente.numeroExpediente)

        // 2. Una cita de ejemplo (solo en el teléfono) no admite consulta: la consulta se abre en la API.
        val resInicio = iniciarUseCase("c-102")
        assertTrue(resInicio.isFailure)
        assertTrue(dataSource.consultaActiva.value.finalizada)
    }
}
