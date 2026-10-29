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
import com.cmo.hospital.features.doctor.consulta.domain.usecase.TickCronometroConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.presentation.ConsultaViewModel
import com.cmo.hospital.features.doctor.expedientes.data.repository.ExpedientesRepositoryImpl
import com.cmo.hospital.features.doctor.expedientes.domain.repository.ExpedientesRepository
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.FiltrarExpedientesUseCase
import com.cmo.hospital.features.doctor.expedientes.domain.usecase.GetExpedientesFlowUseCase
import com.cmo.hospital.features.doctor.expedientes.presentation.ExpedientesViewModel
import com.cmo.hospital.features.doctor.inicio.data.repository.InicioDoctorRepositoryImpl
import com.cmo.hospital.features.doctor.inicio.domain.repository.InicioDoctorRepository
import com.cmo.hospital.features.doctor.inicio.domain.usecase.AbrirExpedientePacienteUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ConstruirPacientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.IniciarORetomarConsultaMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ObserveCitasMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ObserveConsultaEnCursoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.ObserveExpedientesMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.domain.usecase.SincronizarPanelMedicoUseCase
import com.cmo.hospital.features.doctor.inicio.presentation.InicioDoctorViewModel
import com.cmo.hospital.features.doctor.laboratorio.data.repository.LaboratorioRepositoryImpl
import com.cmo.hospital.features.doctor.laboratorio.domain.repository.LaboratorioRepository
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CambiarEstadoOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.GetOrdenesLabFlowUseCase
import com.cmo.hospital.features.doctor.laboratorio.presentation.LaboratorioViewModel
import com.cmo.hospital.features.recepcionista.admision.data.repository.AdmisionRepositoryImpl
import com.cmo.hospital.features.recepcionista.admision.domain.repository.AdmisionRepository
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.GenerarBrazaleteQrUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.ObserveCapacidadCamasUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.RegistrarIngresoPacienteUseCase
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.SimularEscanerDocumentoUseCase
import com.cmo.hospital.features.recepcionista.admision.presentation.AdmisionViewModel
import com.cmo.hospital.features.recepcionista.ambulancia.data.repository.AmbulanciaGuardiaRepositoryImpl
import com.cmo.hospital.features.recepcionista.ambulancia.domain.repository.AmbulanciaGuardiaRepository
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CambiarEstadoAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.GetAmbulanciaFlowUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.GetPersonalGuardiaFiltradoUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.SolicitarDespachoAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.presentation.AmbulanciaGuardiaViewModel
import com.cmo.hospital.features.recepcionista.citas.data.repository.CitaRepositoryImpl
import com.cmo.hospital.features.recepcionista.citas.domain.repository.CitaRepository
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.AgendarCitaCascadaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.CancelarCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.ControlarLlamadorSalaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.GetCitasFlowUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.GetTurnoEnBoxFlowUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.MarcarLlegadaCitaUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.RefreshCitasUseCase
import com.cmo.hospital.features.recepcionista.citas.domain.usecase.RegistrarTriajeYPasarConsultaUseCase
import com.cmo.hospital.features.recepcionista.citas.presentation.CitasViewModel

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
    val admisionRepository: AdmisionRepository = AdmisionRepositoryImpl(clinicalDataSource)
    val ambulanciaRepository: AmbulanciaGuardiaRepository =
        AmbulanciaGuardiaRepositoryImpl(clinicalDataSource)
    val consultaRepository: ConsultaRepository = ConsultaRepositoryImpl(clinicalDataSource)
    val inicioDoctorRepository: InicioDoctorRepository = InicioDoctorRepositoryImpl(clinicalDataSource)
    val expedientesRepository: ExpedientesRepository = ExpedientesRepositoryImpl(clinicalDataSource)
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
            agendarCitaCascadaUseCase = AgendarCitaCascadaUseCase(citaRepository),
            marcarLlegadaCitaUseCase = MarcarLlegadaCitaUseCase(citaRepository),
            cancelarCitaUseCase = CancelarCitaUseCase(citaRepository),
            registrarTriajeYPasarConsultaUseCase = RegistrarTriajeYPasarConsultaUseCase(citaRepository),
            controlarLlamadorSalaUseCase = ControlarLlamadorSalaUseCase(citaRepository)
        )

    fun createAdmisionViewModel(): AdmisionViewModel =
        AdmisionViewModel(
            observeCapacidadCamasUseCase = ObserveCapacidadCamasUseCase(admisionRepository),
            simularEscanerDocumentoUseCase = SimularEscanerDocumentoUseCase(),
            registrarIngresoPacienteUseCase = RegistrarIngresoPacienteUseCase(admisionRepository),
            generarBrazaleteQrUseCase = GenerarBrazaleteQrUseCase()
        )

    fun createAmbulanciaGuardiaViewModel(): AmbulanciaGuardiaViewModel =
        AmbulanciaGuardiaViewModel(
            getAmbulanciaFlowUseCase = GetAmbulanciaFlowUseCase(ambulanciaRepository),
            cambiarEstadoAmbulanciaUseCase = CambiarEstadoAmbulanciaUseCase(ambulanciaRepository),
            solicitarDespachoAmbulanciaUseCase = SolicitarDespachoAmbulanciaUseCase(ambulanciaRepository),
            getPersonalGuardiaFiltradoUseCase = GetPersonalGuardiaFiltradoUseCase(ambulanciaRepository)
        )

    fun createInicioDoctorViewModel(): InicioDoctorViewModel =
        InicioDoctorViewModel(
            observeCitasMedicoUseCase = ObserveCitasMedicoUseCase(inicioDoctorRepository),
            observeConsultaEnCursoUseCase = ObserveConsultaEnCursoUseCase(inicioDoctorRepository),
            observeExpedientesMedicoUseCase = ObserveExpedientesMedicoUseCase(inicioDoctorRepository),
            sincronizarPanelMedicoUseCase = SincronizarPanelMedicoUseCase(inicioDoctorRepository),
            iniciarORetomarConsultaMedicoUseCase = IniciarORetomarConsultaMedicoUseCase(inicioDoctorRepository),
            abrirExpedientePacienteUseCase = AbrirExpedientePacienteUseCase(inicioDoctorRepository),
            construirPacientesMedicoUseCase = ConstruirPacientesMedicoUseCase()
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
            emitirOrdenLabUseCase = EmitirOrdenLabUseCase(laboratorioRepository),
            cambiarEstadoOrdenLabUseCase = CambiarEstadoOrdenLabUseCase(laboratorioRepository)
        )

    fun createExpedientesViewModel(): ExpedientesViewModel =
        ExpedientesViewModel(
            getExpedientesFlowUseCase = GetExpedientesFlowUseCase(expedientesRepository),
            expedientesRepository = expedientesRepository,
            filtrarExpedientesUseCase = FiltrarExpedientesUseCase(),
            emitirOrdenLabUseCase = EmitirOrdenLabUseCase(laboratorioRepository),
            cambiarEstadoOrdenLabUseCase = CambiarEstadoOrdenLabUseCase(laboratorioRepository)
        )

    fun createLaboratorioViewModel(): LaboratorioViewModel =
        LaboratorioViewModel(
            getOrdenesLabFlowUseCase = GetOrdenesLabFlowUseCase(laboratorioRepository),
            expedientesFlow = expedientesRepository.expedientesFlow,
            emitirOrdenLabUseCase = EmitirOrdenLabUseCase(laboratorioRepository),
            cambiarEstadoOrdenLabUseCase = CambiarEstadoOrdenLabUseCase(laboratorioRepository)
        )
}
