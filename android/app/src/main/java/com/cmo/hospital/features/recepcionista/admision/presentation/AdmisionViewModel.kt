package com.cmo.hospital.features.recepcionista.admision.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    observeHabitacionesDetalladasUseCase: ObserveHabitacionesDetalladasUseCase,
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

    val habitaciones: StateFlow<List<HabitacionDetalleUi>> = observeHabitacionesDetalladasUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _filtroTipoHabitacion = MutableStateFlow("Todas")
    val filtroTipoHabitacion: StateFlow<String> = _filtroTipoHabitacion.asStateFlow()

    private val _filtroOcupacionHabitacion = MutableStateFlow("Todas")
    val filtroOcupacionHabitacion: StateFlow<String> = _filtroOcupacionHabitacion.asStateFlow()

    private val _queryBusquedaHabitacion = MutableStateFlow("")
    val queryBusquedaHabitacion: StateFlow<String> = _queryBusquedaHabitacion.asStateFlow()

    val habitacionesFiltradas: StateFlow<List<HabitacionDetalleUi>> = combine(
        habitaciones,
        _filtroTipoHabitacion,
        _filtroOcupacionHabitacion,
        _queryBusquedaHabitacion
    ) { lista, tipo, ocupacion, query ->
        lista.filter { hab ->
            val matchTipo = when (tipo) {
                "Todas" -> true
                "VIP" -> hab.tipo.contains("VIP", ignoreCase = true)
                "Semiprivadas" -> hab.tipo.contains("Semiprivada", ignoreCase = true)
                else -> true
            }
            val matchOcupacion = when (ocupacion) {
                "Todas" -> true
                "Disponibles" -> hab.estaDisponible
                "Ocupadas" -> hab.estaCompletamenteOcupada
                else -> true
            }
            val matchQuery = query.isBlank() ||
                hab.codigo.contains(query, ignoreCase = true) ||
                hab.tipo.contains(query, ignoreCase = true) ||
                hab.planta.contains(query, ignoreCase = true) ||
                hab.camas.any { cama ->
                    cama.codigo.contains(query, ignoreCase = true) ||
                    cama.paciente?.nombreCompleto?.contains(query, ignoreCase = true) == true ||
                    cama.paciente?.dui?.contains(query, ignoreCase = true) == true ||
                    cama.paciente?.numeroExpediente?.contains(query, ignoreCase = true) == true
                }
            matchTipo && matchOcupacion && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uiState = MutableStateFlow(AdmisionFormUiState())
    val uiState: StateFlow<AdmisionFormUiState> = _uiState.asStateFlow()

    fun setFiltroTipoHabitacion(tipo: String) {
        _filtroTipoHabitacion.value = tipo
    }

    fun setFiltroOcupacionHabitacion(ocupacion: String) {
        _filtroOcupacionHabitacion.value = ocupacion
    }

    fun setQueryBusquedaHabitacion(query: String) {
        _queryBusquedaHabitacion.value = query
    }

    fun resetFiltrosHabitaciones() {
        _filtroTipoHabitacion.value = "Todas"
        _filtroOcupacionHabitacion.value = "Todas"
        _queryBusquedaHabitacion.value = ""
    }

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
        tipoDocumento: String,
        documento: String,
        nombres: String,
        apellidos: String,
        fechaNacimiento: String,
        contactoNombre: String,
        contactoTelefono: String,
        contactoParentesco: String,
        responsableMenor: ResponsableMenor?,
        motivoIngreso: String,
        signosVitales: SignosVitales?,
        onFormReset: () -> Unit
    ) {
        viewModelScope.launch {
            val st = _uiState.value

            val contactoFormateado = if (contactoNombre.isNotBlank()) {
                val partes = listOfNotNull(
                    contactoNombre.trim().ifBlank { null },
                    contactoParentesco.trim().takeIf { it.isNotBlank() }?.let { "($it)" },
                    contactoTelefono.trim().takeIf { it.isNotBlank() }
                )
                partes.joinToString(" ")
            } else ""

            val result = registrarIngresoPacienteUseCase(
                dni = documento,
                nombres = nombres,
                apellidos = apellidos,
                fechaNacimiento = fechaNacimiento,
                tipoDocumento = tipoDocumento,
                prevision = "Particular",
                contactoEmergencia = contactoFormateado,
                responsableMenor = responsableMenor,
                protegidoLey19628 = false,
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
        documento: String,
        nombres: String,
        apellidos: String
    ) {
        val st = _uiState.value
        val result = generarBrazaleteQrUseCase(
            dni = documento,
            nombres = nombres,
            apellidos = apellidos,
            codigoCama = st.codigoCamaSeleccionada,
            esEmergenciaDirecta = st.esEmergenciaDirecta,
            nivelTriage = st.nivelTriage,
            protegidoLey19628 = false
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
