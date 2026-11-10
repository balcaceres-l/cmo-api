package com.cmo.hospital

import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.features.doctor.inicio.data.repository.InicioDoctorRepositoryImpl
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ConstruirPacientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.IniciarORetomarConsultaMedicoUseCase
import com.cmo.hospital.features.doctor.laboratorio.data.repository.LaboratorioRepositoryImpl
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.core.data.mapper.agruparHabitaciones
import com.cmo.hospital.features.recepcionista.admision.presentation.AdmisionViewModel
import com.cmo.hospital.features.recepcionista.citas.presentation.CitasViewModel
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
    fun testAnonimizacionDelNombreEnElLlamador() {
        assertEquals("C. M**** (Protegido)", Paciente.anonimizarNombre("Carlos Mendoza"))
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
        val pendientes = lista.filter { CitasViewModel.coincideEstado(it.estado, "Pendientes") }
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
            CitasViewModel.coincideEstado(it.estado, "En Atención") &&
                (it.medicoNombre?.contains("Alejandro", ignoreCase = true) == true)
        }
        assertEquals(1, enAtencionAlejandro.size)
        assertEquals("c-3", enAtencionAlejandro.first().id)
    }

    @Test
    fun testAgendarCitaSinConsultorio() {
        // Recepción no elige consultorio: la programación no lo envía a la API.
        val mapa = ProgramacionCita(idMedico = "u-med", fecha = "2026-11-15", horaInicio = "10:00").aMapa()
        assertEquals(mapOf("id_medico" to "u-med", "fecha" to "2026-11-15", "hora_inicio" to "10:00"), mapa)
        assertFalse(mapa.containsKey("id_consultorio"))
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

    /** Las 17 camas del hospital como las devuelve GET /habitaciones (VIP-301 ocupada, HAB-201 llena). */
    private fun camasDelHospital(): List<Cama> {
        val ocupadas = setOf("VIP-301", "HAB-201-A", "HAB-201-B", "HAB-202-A")
        val paciente = PacienteInfo("u-p1", "Ana López", "EXP-2026-000001", null, "F")
        val codigos = (301..305).map { "VIP-$it" } + (201..206).flatMap { listOf("HAB-$it-A", "HAB-$it-B") }
        return codigos.map { c ->
            if (c in ocupadas) Cama(id = "id-$c", codigo = c, estado = "OCUPADA", idIngreso = "i-$c", paciente = paciente)
            else Cama(id = "id-$c", codigo = c)
        }
    }

    @Test
    fun testHabitacionesDetalladasMapeoYMetricas() {
        val habitaciones = agruparHabitaciones(camasDelHospital())

        assertEquals(11, habitaciones.size)

        val vips = habitaciones.filter { it.tipo.contains("VIP") }
        assertEquals(5, vips.size)
        vips.forEach { assertEquals(1, it.totalCamas) }

        val semis = habitaciones.filter { it.tipo.contains("Semiprivada") }
        assertEquals(6, semis.size)
        semis.forEach { assertEquals(2, it.totalCamas) }

        assertEquals(17, habitaciones.sumOf { it.totalCamas })

        val habVip302 = habitaciones.first { it.codigo == "VIP-302" }
        assertTrue("VIP-302 debe estar disponible", habVip302.estaDisponible)
        assertTrue(habVip302.estaCompletamenteDisponible)

        val habOcupada = habitaciones.first { it.codigo == "VIP-301" }
        assertTrue("VIP-301 debe estar ocupada", habOcupada.estaCompletamenteOcupada)
        assertNotNull("La cama ocupada debe tener información del paciente", habOcupada.camas.first().paciente)

        val hab202 = habitaciones.first { it.codigo == "HAB-202" }
        assertTrue(hab202.estaParcialmenteOcupada)
    }

    @Test
    fun testFiltrosHabitacionesTipoYDisponibilidad() {
        val todas = agruparHabitaciones(camasDelHospital())
        fun filtrar(tipo: String = "Todas", ocup: String = "Todas", q: String = "") =
            AdmisionViewModel.filtrarHabitaciones(todas, tipo, ocup, q)

        assertEquals(5, filtrar(tipo = "VIP").size)
        assertEquals(6, filtrar(tipo = "Semiprivadas").size)
        filtrar(ocup = "Disponibles").forEach { assertTrue(it.camasDisponibles > 0) }
        assertEquals(listOf("VIP-301", "HAB-201"), filtrar(ocup = "Ocupadas").map { it.codigo })
        assertEquals(listOf("HAB-202", "HAB-203", "HAB-204", "HAB-205", "HAB-206"),
            filtrar(tipo = "Semiprivadas", ocup = "Disponibles").map { it.codigo })
        // La búsqueda encuentra la habitación por el paciente internado.
        assertEquals(listOf("VIP-301", "HAB-201", "HAB-202"), filtrar(q = "Ana López").map { it.codigo })
    }

    @Test
    fun testCitasAgendadasPorDefectoYSinFiltroSolicitadas() {
        val citaPorDefecto = Cita(
            id = "c-def-1",
            fecha = "2026-11-10",
            horaInicio = "09:00",
            horaFin = "09:30"
        )
        assertEquals(EstadoCita.AGENDADA, citaPorDefecto.estado)
        assertFalse("No debe existir el filtro Solicitadas en recepción", CitasViewModel.FILTROS_ESTADO.contains("Solicitadas"))
        assertTrue("Debe existir el filtro Agendadas", CitasViewModel.FILTROS_ESTADO.contains("Agendadas"))
    }

    @Test
    fun testSignosVitalesSoloHabilitadosElDiaDeLaCita() = runTest {
        val dataSource = HospitalClinicalDataSource(apiService = null)
        val repo = com.cmo.hospital.features.recepcionista.citas.data.repository.CitaRepositoryImpl(dataSource)
        val hoy = java.time.LocalDate.now()
        val manana = hoy.plusDays(1)
        val useCase = com.cmo.hospital.features.recepcionista.citas.domain.usecase.RegistrarTriajeYPasarConsultaUseCase(
            repository = repo,
            hoy = { hoy }
        )

        val citaHoy = Cita(
            id = "c-hoy-test",
            fecha = hoy.toString(),
            horaInicio = "10:00",
            horaFin = "10:30",
            estado = EstadoCita.AGENDADA
        )
        val citaManana = Cita(
            id = "c-manana-test",
            fecha = manana.toString(),
            horaInicio = "11:00",
            horaFin = "11:30",
            estado = EstadoCita.AGENDADA
        )
        dataSource.prependCita(citaHoy)
        dataSource.prependCita(citaManana)

        assertTrue(citaHoy.esDelDia(hoy.toString()))
        assertFalse(citaManana.esDelDia(hoy.toString()))

        val signos = SignosVitales(presionSistolica = 120, presionDiastolica = 80, temperaturaC = 36.7)
        val resHoy = useCase("c-hoy-test", signos, pasarAConsulta = false)
        assertTrue("El día de la cita debe permitir tomar signos vitales", resHoy.isSuccess)

        val resManana = useCase("c-manana-test", signos, pasarAConsulta = false)
        assertTrue("En una cita futura no se debe permitir tomar signos vitales", resManana.isFailure)
    }

    @Test
    fun testFiltrosCitasPorFechaYCalendario() {
        val lista = listOf(
            Cita(
                id = "c-10",
                fecha = "2026-11-10",
                horaInicio = "09:00",
                horaFin = "09:30",
                estado = EstadoCita.AGENDADA,
                medicoNombre = "Dr. Alejandro Restrepo"
            ),
            Cita(
                id = "c-11",
                fecha = "2026-11-11",
                horaInicio = "08:30",
                horaFin = "09:00",
                estado = EstadoCita.AGENDADA,
                medicoNombre = "Dra. Sofía Valenzuela"
            ),
            Cita(
                id = "c-12",
                fecha = "2026-11-10",
                horaInicio = "14:00",
                horaFin = "14:30",
                estado = EstadoCita.CANCELADA,
                medicoNombre = "Dr. Alejandro Restrepo"
            )
        )

        val porFechaUi = CitasViewModel.filtrarCitas(lista, fecha = "10/11/2026")
        assertEquals(2, porFechaUi.size)

        val agendadasFecha = CitasViewModel.filtrarCitas(lista, estado = "Agendadas", fecha = "2026-11-10")
        assertEquals(listOf("c-10"), agendadasFecha.map { it.id })

        // Calendario excluye citas canceladas y filtra por doctor y fecha
        val calHoy = CitasViewModel.filtrarCitasCalendario(lista, fecha = "2026-11-10")
        assertEquals(listOf("c-10"), calHoy.map { it.id })

        val calSofia = CitasViewModel.filtrarCitasCalendario(lista, doctor = "Sofía", fecha = "")
        assertEquals(listOf("c-11"), calSofia.map { it.id })
    }
}
