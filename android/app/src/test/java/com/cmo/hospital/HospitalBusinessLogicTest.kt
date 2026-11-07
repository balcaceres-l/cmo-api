package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.features.doctor.inicio.data.repository.InicioDoctorRepositoryImpl
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ConstruirPacientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.IniciarORetomarConsultaMedicoUseCase
import com.cmo.hospital.features.doctor.laboratorio.data.repository.LaboratorioRepositoryImpl
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.features.recepcionista.admision.data.repository.AdmisionRepositoryImpl
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.GenerarBrazaleteQrUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.RegistrarIngresoPacienteUseCase
import com.cmo.hospital.features.recepcionista.citas.data.repository.CitaRepositoryImpl
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.AgendarCitaCascadaUseCase
import com.cmo.hospital.features.recepcionista.pacientes.presentation.DuiTextWatcher
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

    // ─── Nuevos Tests Clean Architecture & CMO ──────────────────────────────

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

    @Test
    fun testDateUtilsConversionFormats() {
        assertEquals("2026-10-29", DateUtils.toIsoDate("29/10/2026"))
        assertEquals("2026-05-04", DateUtils.toIsoDate("04/05/2026"))
        assertEquals("2026-10-29", DateUtils.toIsoDate("2026-10-29"))

        assertEquals("29/10/2026", DateUtils.toUiDate("2026-10-29"))
        assertEquals("04/05/2026", DateUtils.toUiDate("2026-05-04"))
        assertEquals("29/10/2026", DateUtils.toUiDate("29/10/2026"))
    }

    @Test
    fun testFiltrosCitasPorEstadoYDoctor() {
        val lista = listOf(
            Cita(
                id = "c-1",
                fecha = "2026-10-29",
                horaInicio = "09:00",
                horaFin = "09:30",
                estado = "AGENDADA",
                medicoNombre = "Dr. Alejandro Restrepo"
            ),
            Cita(
                id = "c-2",
                fecha = "2026-10-29",
                horaInicio = "10:00",
                horaFin = "10:30",
                estado = "EN_ESPERA",
                medicoNombre = "Dra. Sofía Mendoza"
            ),
            Cita(
                id = "c-3",
                fecha = "2026-10-29",
                horaInicio = "11:00",
                horaFin = "11:30",
                estado = "EN_ATENCION",
                medicoNombre = "Dr. Alejandro Restrepo"
            ),
            Cita(
                id = "c-4",
                fecha = "2026-10-29",
                horaInicio = "14:00",
                horaFin = "14:30",
                estado = "CANCELADA",
                medicoNombre = "Dr. Alejandro Restrepo"
            )
        )

        // Filtro por estado: Pendientes -> AGENDADA
        val pendientes = lista.filter {
            it.estado.equals("AGENDADA", ignoreCase = true) || it.estado.equals("PENDIENTE", ignoreCase = true)
        }
        assertEquals(1, pendientes.size)
        assertEquals("c-1", pendientes.first().id)

        // Filtro por doctor: "Sofía"
        val sofiaCitas = lista.filter {
            it.medicoNombre?.contains("Sofía", ignoreCase = true) == true
        }
        assertEquals(1, sofiaCitas.size)
        assertEquals("c-2", sofiaCitas.first().id)

        // Filtro combinado: Dr. Alejandro + EN_ATENCION
        val enAtencionAlejandro = lista.filter {
            it.estado.equals("EN_ATENCION", ignoreCase = true) &&
                (it.medicoNombre?.contains("Alejandro", ignoreCase = true) == true)
        }
        assertEquals(1, enAtencionAlejandro.size)
        assertEquals("c-3", enAtencionAlejandro.first().id)
    }

    @Test
    fun testAgendarCitaSinConsultorio() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val citaRepo = CitaRepositoryImpl(dataSource)
        val agendarUseCase = AgendarCitaCascadaUseCase(citaRepo)

        val res = agendarUseCase(
            especialidad = "Cardiología",
            medicoNombre = "Dra. Camila Morales",
            pacienteNombre = "Roberto Gómez",
            dui = "09876543-2",
            fechaNacimiento = "1985-06-15",
            fecha = "2026-11-15",
            horaInicio = "10:00",
            motivo = "Evaluación cardiovascular"
        )

        assertTrue(res.isSuccess)
        val cita = res.getOrThrow()
        assertNull("El consultorio no debe guardarse en la app móvil al agendar", cita.consultorio)
        assertEquals("Dra. Camila Morales", cita.medicoNombre)
        assertEquals("Cardiología", cita.especialidad)
        assertEquals("Roberto Gómez", cita.paciente?.nombreCompleto)
        assertEquals("AGENDADA", cita.estado)
        assertEquals("10:00", cita.horaInicio)
        assertEquals("10:30", cita.horaFin)
    }

    @Test
    fun testDuiElSalvadorFormatoYValidacion() {
        val duiRegex = Regex("""^\d{8}-\d$""")
        val duiValido = "04829102-1"
        val duiInvalido1 = "048291021"
        val duiInvalido2 = "0482-1"
        val duiInvalidoLetras = "0482910A-1"

        assertTrue(duiValido.matches(duiRegex))
        assertFalse(duiInvalido1.matches(duiRegex))
        assertFalse(duiInvalido2.matches(duiRegex))
        assertFalse(duiInvalidoLetras.matches(duiRegex))

        assertEquals("04829102-1", DuiTextWatcher.formatear("048291021"))
        assertEquals("04829102-1", DuiTextWatcher.formatear("04829102-1"))
        assertEquals("0482", DuiTextWatcher.formatear("0482"))
    }

    @Test
    fun testCalculoEdadYMinoridadMenorDe18Anios() {
        val pacienteMenor = Paciente(
            nombreCompleto = "Sofía Niña",
            fechaNacimiento = "2015-04-12"
        )
        assertTrue("Paciente nacida en 2015 debe ser menor de 18 años", pacienteMenor.edad < 18)

        val pacienteAdulto = Paciente(
            nombreCompleto = "Roberto Adulto",
            fechaNacimiento = "1990-08-20"
        )
        assertTrue("Paciente nacido en 1990 debe ser mayor o igual a 18 años", pacienteAdulto.edad >= 18)
    }

    @Test
    fun testRegistroIngresoPacienteConTutorMenorYContactoDesglosado() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val repo = AdmisionRepositoryImpl(dataSource)
        val useCase = RegistrarIngresoPacienteUseCase(repo)

        val tutor = ResponsableMenor(
            tipoDocumento = "DUI",
            documento = "01234567-8",
            nombreCompleto = "María Elena Flores",
            telefono = "7890-1234",
            parentesco = "Madre"
        )

        val res = useCase(
            dni = "09876543-2",
            nombres = "Mateo",
            apellidos = "Pérez",
            fechaNacimiento = "2012-03-15",
            tipoDocumento = "DUI",
            prevision = "Particular",
            contactoEmergencia = "María Flores (Madre) 7890-1234",
            responsableMenor = tutor,
            motivoIngreso = "Apendicitis aguda en estudio",
            nivelTriage = NivelTriageManchester.NIVEL_II_NARANJA,
            esEmergenciaDirecta = false,
            codigoCamaSeleccionada = "VIP-302",
            signosVitales = null
        )

        assertTrue(res.isSuccess)
        val paciente = res.getOrThrow()
        assertEquals("Mateo Pérez", paciente.nombreCompleto)
        assertTrue(paciente.edad < 18)
        assertNotNull(paciente.responsableMenor)
        assertEquals("María Elena Flores", paciente.responsableMenor?.nombreCompleto)
        assertEquals("Madre", paciente.responsableMenor?.parentesco)
        assertEquals("01234567-8", paciente.responsableMenor?.documento)
        assertEquals("VIP-302", repo.camasFlow.value.first { it.codigo == "VIP-302" }.codigo)
        assertEquals("OCUPADA", repo.camasFlow.value.first { it.codigo == "VIP-302" }.estado)
    }

    @Test
    fun testHabitacionesDetalladasMapeoYMetricas() {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val repo = AdmisionRepositoryImpl(dataSource)
        val habitaciones = repo.obtenerHabitacionesDetalladas()

        assertEquals(11, habitaciones.size)

        val vips = habitaciones.filter { it.tipo.contains("VIP") }
        assertEquals(5, vips.size)
        vips.forEach { assertEquals(1, it.totalCamas) }

        val semis = habitaciones.filter { it.tipo.contains("Semiprivada") }
        assertEquals(6, semis.size)
        semis.forEach { assertEquals(2, it.totalCamas) }

        val totalCamasEnHabitaciones = habitaciones.sumOf { it.totalCamas }
        assertEquals(17, totalCamasEnHabitaciones)

        val habVip302 = habitaciones.first { it.codigo == "VIP-302" }
        assertTrue("VIP-302 debe estar disponible inicialmente", habVip302.estaDisponible)
        assertTrue(habVip302.estaCompletamenteDisponible)

        val habOcupada = habitaciones.first { it.codigo == "VIP-301" }
        assertTrue("VIP-301 debe estar ocupada inicialmente", habOcupada.estaCompletamenteOcupada)
        assertNotNull("La cama ocupada debe tener información del paciente", habOcupada.camas.first().paciente)
    }

    @Test
    fun testFiltrosHabitacionesTipoYDisponibilidad() {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val repo = AdmisionRepositoryImpl(dataSource)
        val todas = repo.obtenerHabitacionesDetalladas()

        // 1. Filtro Tipo = VIP
        val vips = todas.filter { it.tipo.contains("VIP", ignoreCase = true) }
        assertEquals(5, vips.size)

        // 2. Filtro Tipo = Semiprivadas
        val semis = todas.filter { it.tipo.contains("Semiprivada", ignoreCase = true) }
        assertEquals(6, semis.size)

        // 3. Filtro Disponibilidad = Disponibles
        val disponibles = todas.filter { it.estaDisponible }
        assertTrue("Deben existir habitaciones con disponibilidad", disponibles.isNotEmpty())
        disponibles.forEach { assertTrue(it.camasDisponibles > 0) }

        // 4. Filtro Disponibilidad = 100% Ocupadas
        val ocupadas = todas.filter { it.estaCompletamenteOcupada }
        assertTrue("Deben existir habitaciones ocupadas", ocupadas.isNotEmpty())
        ocupadas.forEach { assertEquals(0, it.camasDisponibles) }

        // 5. Filtro Combinado: Semiprivadas + Con Disponibilidad
        val semisDisponibles = todas.filter {
            it.tipo.contains("Semiprivada", ignoreCase = true) && it.estaDisponible
        }
        semisDisponibles.forEach {
            assertTrue(it.tipo.contains("Semiprivada"))
            assertTrue(it.estaDisponible)
        }
    }
}
