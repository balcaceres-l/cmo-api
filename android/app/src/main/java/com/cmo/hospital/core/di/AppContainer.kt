package com.cmo.hospital.core.di

import android.content.Context
import com.cmo.hospital.core.data.datasource.HospitalClinicalDataSource
import com.cmo.hospital.core.network.ApiService
import com.cmo.hospital.core.network.RetrofitClient
import com.cmo.hospital.core.session.SessionManager
import com.cmo.hospital.features.auth.data.repository.AuthRepositoryImpl
import com.cmo.hospital.features.auth.domain.repository.AuthRepository
import com.cmo.hospital.features.auth.domain.usecase.GetActiveUserUseCase
import com.cmo.hospital.features.auth.domain.usecase.LoginUseCase
import com.cmo.hospital.features.auth.domain.usecase.LogoutUseCase
import com.cmo.hospital.features.auth.presentation.LoginViewModel
import com.cmo.hospital.features.doctor.consulta.data.repository.ConsultaRepositoryImpl
import com.cmo.hospital.features.doctor.consulta.domain.repository.ConsultaRepository
import com.cmo.hospital.features.doctor.consulta.domain.usecase.ActualizarSignosConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.AgendarCitaSeguimientoDoctorUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.FinalizarConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GestionarCie10ConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GestionarRecetaConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GetConsultaActivaFlowUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GuardarAnamnesisUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GuardarCambiosConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.TickCronometroConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.presentation.ConsultaViewModel
import com.cmo.hospital.features.doctor.expedientes.data.repository.ExpedientesRepositoryImpl
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.BuscarPacientesTratadosUseCase
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.CargarExpedienteUseCase
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.GuardarAntecedentesUseCase
import com.cmo.hospital.features.doctor.expedientes.presentation.ExpedientesViewModel
import com.cmo.hospital.features.doctor.inicio.data.repository.InicioDoctorRepositoryImpl
import com.cmo.hospital.features.doctor.inicio.domain.repository.InicioDoctorRepository
import com.cmo.hospital.features.doctor.inicio.domain.usecase.AbrirExpedientePacienteUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ConstruirPacientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.IniciarORetomarConsultaMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ObserveCitasMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ResumirMisTurnosUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ObserveConsultaEnCursoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ObserveExpedientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.SincronizarPanelMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.presentation.InicioDoctorViewModel
import com.cmo.hospital.features.doctor.laboratorio.data.repository.LaboratorioRepositoryImpl
import com.cmo.hospital.features.doctor.laboratorio.domain.repository.LaboratorioRepository
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CancelarOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.RefrescarLaboratorioUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.GetOrdenesLabFlowUseCase
import com.cmo.hospital.features.doctor.laboratorio.presentation.LaboratorioViewModel
import com.cmo.hospital.features.recepcionista.admision.data.repository.AdmisionRepositoryImpl
import com.cmo.hospital.features.recepcionista.admision.domain.repository.AdmisionRepository
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.BuscarPacientesIngresoUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.CargarMedicosTratantesUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.DarAltaUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.IngresarPacienteUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.ObserveCapacidadCamasUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.ObserveHabitacionesDetalladasUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.RefrescarCamasUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.TrasladarPacienteUseCase
import com.cmo.hospital.features.recepcionista.admision.presentation.AdmisionViewModel
import com.cmo.hospital.features.recepcionista.ambulancia.data.repository.AmbulanciaGuardiaRepositoryImpl
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CargarPanelAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.EnviarReporteAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.FinalizarServicioAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.ObservarAmbulanciaEnVivoUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.SolicitarDespachoAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.presentation.AmbulanciaGuardiaViewModel
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CargarAmbulanciaMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.presentation.AmbulanciaMedicoViewModel
import com.cmo.hospital.core.realtime.ReverbClient
import com.cmo.hospital.features.recepcionista.citas.data.repository.CitaRepositoryImpl
import com.cmo.hospital.features.recepcionista.citas.domain.repository.CitaRepository
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.AgendarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.BuscarPacientesParaCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.CargarCatalogosAgendaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.ConfirmarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.GetDisponibilidadUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.ReprogramarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.CancelarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.ControlarLlamadorSalaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.GetCitasFlowUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.GetTurnoEnBoxFlowUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.MarcarLlegadaCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.RefreshCitasUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.RegistrarTriajeYPasarConsultaUseCase
import com.cmo.hospital.features.recepcionista.citas.presentation.CitasViewModel
import com.cmo.hospital.features.recepcionista.pacientes.data.repository.PacientesRepositoryImpl
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacientesRepository
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.BuscarPacientesUseCase
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GenerarCodigoActivacionUseCase
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GuardarPacienteUseCase
import com.cmo.hospital.features.recepcionista.pacientes.presentation.PacientesViewModel
import com.cmo.hospital.features.recepcionista.laboratorio.data.repository.LaboratorioRecepcionRepositoryImpl
import com.cmo.hospital.features.recepcionista.laboratorio.domain.repository.LaboratorioRecepcionRepository
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.AvanzarOrdenLaboratorioUseCase
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.ListarOrdenesLaboratorioUseCase
import com.cmo.hospital.features.recepcionista.laboratorio.presentation.LaboratorioRecepcionViewModel

/**
 * Contenedor de Inyección de Dependencias Clean Architecture (Domain <- Data <- Presentation).
 * Mantiene una instancia compartida de [HospitalClinicalDataSource] para que los eventos clínicos
 * entre Recepción y Cuerpo Médico se sincronicen instantáneamente en tiempo real.
 */
class AppContainer(context: Context) {

    val sessionManager: SessionManager = SessionManager(context.applicationContext)
    val apiService: ApiService = RetrofitClient.createApiService(sessionManager)
    val clinicalDataSource: HospitalClinicalDataSource = HospitalClinicalDataSource(apiService)

    // ─── Repositorios por Feature ────────────────────────────────────────────
    val authRepository: AuthRepository = AuthRepositoryImpl(apiService, sessionManager)
    val citaRepository: CitaRepository = CitaRepositoryImpl(clinicalDataSource)
    val pacientesRepository: PacientesRepository = PacientesRepositoryImpl(apiService)
    val laboratorioRecepcionRepository: LaboratorioRecepcionRepository =
        LaboratorioRecepcionRepositoryImpl(apiService)
    val admisionRepository: AdmisionRepository = AdmisionRepositoryImpl(clinicalDataSource)
    val ambulanciaRepository: AmbulanciaGuardiaRepository =
        AmbulanciaGuardiaRepositoryImpl(apiService, ReverbClient())
    val consultaRepository: ConsultaRepository = ConsultaRepositoryImpl(clinicalDataSource)
    val inicioDoctorRepository: InicioDoctorRepository = InicioDoctorRepositoryImpl(clinicalDataSource, apiService)
    val expedientesRepository: ExpedientesRepository = ExpedientesRepositoryImpl(apiService, clinicalDataSource)
    val laboratorioRepository: LaboratorioRepository = LaboratorioRepositoryImpl(clinicalDataSource)

    // ─── Casos de Uso transversales de Sesión & Seguimiento ──────────────────
    val loginUseCase = LoginUseCase(authRepository)
    val logoutUseCase = LogoutUseCase(authRepository)
    val getActiveUserUseCase = GetActiveUserUseCase(authRepository)
    val agendarCitaSeguimientoDoctorUseCase = AgendarCitaSeguimientoDoctorUseCase(consultaRepository)

    // ─── Factories MVVM por Feature ──────────────────────────────────────────

    fun createLoginViewModel(): LoginViewModel =
        LoginViewModel(loginUseCase = loginUseCase)

    fun createCitasViewModel(): CitasViewModel =
        CitasViewModel(
            getCitasFlowUseCase = GetCitasFlowUseCase(citaRepository),
            getTurnoEnBoxFlowUseCase = GetTurnoEnBoxFlowUseCase(citaRepository),
            refreshCitasUseCase = RefreshCitasUseCase(citaRepository),
            cargarCatalogosAgendaUseCase = CargarCatalogosAgendaUseCase(citaRepository),
            getDisponibilidadUseCase = GetDisponibilidadUseCase(citaRepository),
            buscarPacientesParaCitaUseCase = BuscarPacientesParaCitaUseCase(citaRepository),
            agendarCitaUseCase = AgendarCitaUseCase(citaRepository),
            confirmarCitaUseCase = ConfirmarCitaUseCase(citaRepository),
            reprogramarCitaUseCase = ReprogramarCitaUseCase(citaRepository),
            marcarLlegadaCitaUseCase = MarcarLlegadaCitaUseCase(citaRepository),
            cancelarCitaUseCase = CancelarCitaUseCase(citaRepository),
            registrarTriajeYPasarConsultaUseCase = RegistrarTriajeYPasarConsultaUseCase(citaRepository),
            controlarLlamadorSalaUseCase = ControlarLlamadorSalaUseCase(citaRepository)
        )

    fun createPacientesViewModel(): PacientesViewModel =
        PacientesViewModel(
            buscarPacientesUseCase = BuscarPacientesUseCase(pacientesRepository),
            guardarPacienteUseCase = GuardarPacienteUseCase(pacientesRepository),
            generarCodigoActivacionUseCase = GenerarCodigoActivacionUseCase(pacientesRepository)
        )

    fun createLaboratorioRecepcionViewModel(): LaboratorioRecepcionViewModel =
        LaboratorioRecepcionViewModel(
            listarOrdenesLaboratorioUseCase = ListarOrdenesLaboratorioUseCase(laboratorioRecepcionRepository),
            avanzarOrdenLaboratorioUseCase = AvanzarOrdenLaboratorioUseCase(laboratorioRecepcionRepository)
        )

    fun createAdmisionViewModel(): AdmisionViewModel =
        AdmisionViewModel(
            observeCapacidadCamasUseCase = ObserveCapacidadCamasUseCase(admisionRepository),
            observeHabitacionesDetalladasUseCase = ObserveHabitacionesDetalladasUseCase(admisionRepository),
            refrescarCamasUseCase = RefrescarCamasUseCase(admisionRepository),
            cargarMedicosTratantesUseCase = CargarMedicosTratantesUseCase(admisionRepository),
            buscarPacientesIngresoUseCase = BuscarPacientesIngresoUseCase(admisionRepository),
            ingresarPacienteUseCase = IngresarPacienteUseCase(admisionRepository),
            darAltaUseCase = DarAltaUseCase(admisionRepository),
            trasladarPacienteUseCase = TrasladarPacienteUseCase(admisionRepository),
            guardarPacienteUseCase = GuardarPacienteUseCase(pacientesRepository)
        )

    fun createAmbulanciaGuardiaViewModel(): AmbulanciaGuardiaViewModel =
        AmbulanciaGuardiaViewModel(
            cargarPanelAmbulanciaUseCase = CargarPanelAmbulanciaUseCase(ambulanciaRepository),
            solicitarDespachoAmbulanciaUseCase = SolicitarDespachoAmbulanciaUseCase(ambulanciaRepository),
            finalizarServicioAmbulanciaUseCase = FinalizarServicioAmbulanciaUseCase(ambulanciaRepository),
            enviarReporteAmbulanciaUseCase = EnviarReporteAmbulanciaUseCase(ambulanciaRepository),
            observarAmbulanciaEnVivoUseCase = ObservarAmbulanciaEnVivoUseCase(ambulanciaRepository)
        )

    fun createAmbulanciaMedicoViewModel(): AmbulanciaMedicoViewModel =
        AmbulanciaMedicoViewModel(
            cargarAmbulanciaMedicoUseCase = CargarAmbulanciaMedicoUseCase(ambulanciaRepository),
            enviarReporteAmbulanciaUseCase = EnviarReporteAmbulanciaUseCase(ambulanciaRepository),
            observarAmbulanciaEnVivoUseCase = ObservarAmbulanciaEnVivoUseCase(ambulanciaRepository)
        )

    fun createInicioDoctorViewModel(): InicioDoctorViewModel =
        InicioDoctorViewModel(
            observeCitasMedicoUseCase = ObserveCitasMedicoUseCase(inicioDoctorRepository),
            observeConsultaEnCursoUseCase = ObserveConsultaEnCursoUseCase(inicioDoctorRepository),
            observeExpedientesMedicoUseCase = ObserveExpedientesMedicoUseCase(inicioDoctorRepository),
            sincronizarPanelMedicoUseCase = SincronizarPanelMedicoUseCase(inicioDoctorRepository),
            iniciarORetomarConsultaMedicoUseCase = IniciarORetomarConsultaMedicoUseCase(inicioDoctorRepository),
            abrirExpedientePacienteUseCase = AbrirExpedientePacienteUseCase(inicioDoctorRepository),
            construirPacientesMedicoUseCase = ConstruirPacientesMedicoUseCase(),
            resumirMisTurnosUseCase = ResumirMisTurnosUseCase(inicioDoctorRepository)
        )

    fun createConsultaViewModel(): ConsultaViewModel =
        ConsultaViewModel(
            getConsultaActivaFlowUseCase = GetConsultaActivaFlowUseCase(consultaRepository),
            tickCronometroConsultaUseCase = TickCronometroConsultaUseCase(consultaRepository),
            actualizarSignosConsultaUseCase = ActualizarSignosConsultaUseCase(consultaRepository),
            gestionarCie10ConsultaUseCase = GestionarCie10ConsultaUseCase(consultaRepository),
            guardarAnamnesisUseCase = GuardarAnamnesisUseCase(consultaRepository),
            gestionarRecetaConsultaUseCase = GestionarRecetaConsultaUseCase(consultaRepository),
            finalizarConsultaUseCase = FinalizarConsultaUseCase(consultaRepository),
            guardarCambiosConsultaUseCase = GuardarCambiosConsultaUseCase(consultaRepository),
            emitirOrdenLabUseCase = EmitirOrdenLabUseCase(laboratorioRepository),
            cancelarOrdenLabUseCase = CancelarOrdenLabUseCase(laboratorioRepository)
        )

    fun createExpedientesViewModel(): ExpedientesViewModel =
        ExpedientesViewModel(
            expedientesRepository = expedientesRepository,
            buscarPacientesTratadosUseCase = BuscarPacientesTratadosUseCase(expedientesRepository),
            cargarExpedienteUseCase = CargarExpedienteUseCase(expedientesRepository),
            guardarAntecedentesUseCase = GuardarAntecedentesUseCase(expedientesRepository),
            emitirOrdenLabUseCase = EmitirOrdenLabUseCase(laboratorioRepository),
            cancelarOrdenLabUseCase = CancelarOrdenLabUseCase(laboratorioRepository)
        )

    fun createLaboratorioViewModel(): LaboratorioViewModel =
        LaboratorioViewModel(
            getOrdenesLabFlowUseCase = GetOrdenesLabFlowUseCase(laboratorioRepository),
            consultaActivaFlow = consultaRepository.consultaActivaFlow,
            emitirOrdenLabUseCase = EmitirOrdenLabUseCase(laboratorioRepository),
            cancelarOrdenLabUseCase = CancelarOrdenLabUseCase(laboratorioRepository),
            refrescarLaboratorioUseCase = RefrescarLaboratorioUseCase(laboratorioRepository)
        )
}
