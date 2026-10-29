package com.cmo.hospital.features.recepcionista.admision.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdmisionFormUiState(
    val protegidoLey19628: Boolean = false,
    val isScanningDni: Boolean = false,
    val scanFeedbackVisible: Boolean = false,
    val scannedData: DatosDocumentoEscaneado? = null,
    val nivelTriage: NivelTriageManchester = NivelTriageManchester.NIVEL_III_AMARILLO,
    val esEmergenciaDirecta: Boolean = false,
    val codigoCamaSeleccionada: String? = null,
    val brazaleteEmitido: BrazaleteQrPayload? = null,
    val toastTitle: String? = null,
    val toastMessage: String? = null,
    val toastIsError: Boolean = false
)

class AdmisionViewModel(
    observeCapacidadCamasUseCase: ObserveCapacidadCamasUseCase,
    private val simularEscanerDocumentoUseCase: SimularEscanerDocumentoUseCase,
    private val registrarIngresoPacienteUseCase: RegistrarIngresoPacienteUseCase,
    private val generarBrazaleteQrUseCase: GenerarBrazaleteQrUseCase
) : ViewModel() {

    val capacidad: StateFlow<CapacidadHospitalaria> = observeCapacidadCamasUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CapacidadHospitalaria(0, 0, 0, emptyList(), emptyList())
        )

    private val _uiState = MutableStateFlow(AdmisionFormUiState())
    val uiState: StateFlow<AdmisionFormUiState> = _uiState.asStateFlow()

    fun setProtegidoLey19628(activo: Boolean) {
        _uiState.value = _uiState.value.copy(protegidoLey19628 = activo)
    }

    fun setNivelTriage(nivel: NivelTriageManchester) {
        _uiState.value = _uiState.value.copy(nivelTriage = nivel)
    }

    fun setEmergenciaDirecta(activo: Boolean) {
        _uiState.value = _uiState.value.copy(esEmergenciaDirecta = activo)
    }

    fun seleccionarCama(codigoCama: String) {
        val actual = _uiState.value.codigoCamaSeleccionada
        _uiState.value = _uiState.value.copy(
            codigoCamaSeleccionada = if (actual == codigoCama) null else codigoCama
        )
    }

    fun escanearDocumento() {
        if (_uiState.value.isScanningDni) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningDni = true)
            val datos = simularEscanerDocumentoUseCase()
            _uiState.value = _uiState.value.copy(
                isScanningDni = false,
                scanFeedbackVisible = true,
                scannedData = datos
            )
        }
    }

    fun consumeScannedData() {
        _uiState.value = _uiState.value.copy(scannedData = null)
    }

    fun confirmarIngreso(
        dni: String,
        nombres: String,
        apellidos: String,
        edadTexto: String,
        prevision: String,
        contactoEmergencia: String,
        motivoIngreso: String,
        signosVitales: SignosVitales?,
        onFormReset: () -> Unit
    ) {
        viewModelScope.launch {
            val st = _uiState.value
            val result = registrarIngresoPacienteUseCase(
                dni = dni,
                nombres = nombres,
                apellidos = apellidos,
                edadTexto = edadTexto,
                prevision = prevision,
                contactoEmergencia = contactoEmergencia,
                protegidoLey19628 = st.protegidoLey19628,
                motivoIngreso = motivoIngreso,
                nivelTriage = st.nivelTriage,
                esEmergenciaDirecta = st.esEmergenciaDirecta,
                codigoCamaSeleccionada = st.codigoCamaSeleccionada,
                signosVitales = signosVitales
            )
            result.fold(
                onSuccess = { pac ->
                    val msg = if (st.esEmergenciaDirecta) {
                        "${pac.nombreCompleto} derivado/a DIRECTO a Box 03 (Consulta Activa iniciada en 00:00)."
                    } else {
                        "${pac.nombreCompleto} asignado/a exitosamente a ${st.codigoCamaSeleccionada}. Exp: ${pac.numeroExpediente}."
                    }
                    _uiState.value = AdmisionFormUiState(
                        toastTitle = if (st.esEmergenciaDirecta) "Pase Directo a Box Confirmado" else "Ingreso Confirmado",
                        toastMessage = msg,
                        toastIsError = false
                    )
                    onFormReset()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        toastTitle = "Selección / Datos Requeridos",
                        toastMessage = err.message ?: "Verifique los datos de admisión.",
                        toastIsError = true
                    )
                }
            )
        }
    }

    fun generarBrazaleteQr(
        dni: String,
        nombres: String,
        apellidos: String
    ) {
        val st = _uiState.value
        val result = generarBrazaleteQrUseCase(
            dni = dni,
            nombres = nombres,
            apellidos = apellidos,
            codigoCama = st.codigoCamaSeleccionada,
            esEmergenciaDirecta = st.esEmergenciaDirecta,
            nivelTriage = st.nivelTriage,
            protegidoLey19628 = st.protegidoLey19628
        )
        result.fold(
            onSuccess = { payload ->
                _uiState.value = st.copy(
                    brazaleteEmitido = payload,
                    toastTitle = "Brazalete QR Emitido",
                    toastMessage = "Imprimiendo brazalete con UUID médico y clave criptográfica para ${payload.codigoCama}.",
                    toastIsError = false
                )
            },
            onFailure = { err ->
                _uiState.value = st.copy(
                    toastTitle = "Atención",
                    toastMessage = err.message ?: "Seleccione primero una cama.",
                    toastIsError = true
                )
            }
        )
    }

    fun consumeBrazaleteDialog() {
        _uiState.value = _uiState.value.copy(brazaleteEmitido = null)
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toastTitle = null, toastMessage = null)
    }
}
