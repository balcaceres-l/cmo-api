package com.cmo.hospital.features.recepcionista.admision.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdmisionUiState(
    val medicos: List<User> = emptyList(),
    val idMedicoSeleccionado: String? = null,
    val pacientesEncontrados: List<Paciente> = emptyList(),
    val buscandoPacientes: Boolean = false,
    val codigoCamaSeleccionada: String? = null,
    val cargandoCamas: Boolean = false,
    val errorCamas: String? = null,
    val enviando: Boolean = false
)

/** Error de validación del formulario; [campo] usa el nombre del campo en la API. */
data class ErrorIngreso(val campo: String?, val mensaje: String)

data class AdmisionEvento(val titulo: String, val mensaje: String, val esError: Boolean = false)

class AdmisionViewModel(
    observeCapacidadCamasUseCase: ObserveCapacidadCamasUseCase,
    private val refrescarCamasUseCase: RefrescarCamasUseCase,
    private val cargarMedicosTratantesUseCase: CargarMedicosTratantesUseCase,
    private val buscarPacientesIngresoUseCase: BuscarPacientesIngresoUseCase,
    private val ingresarPacienteUseCase: IngresarPacienteUseCase,
    private val darAltaUseCase: DarAltaUseCase,
    private val trasladarPacienteUseCase: TrasladarPacienteUseCase
) : ViewModel() {

    val capacidad: StateFlow<CapacidadHospitalaria> = observeCapacidadCamasUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CapacidadHospitalaria(0, 0, 0, emptyList(), emptyList())
        )

    private val _uiState = MutableStateFlow(AdmisionUiState())
    val uiState: StateFlow<AdmisionUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<ErrorIngreso?>(null)
    val error: StateFlow<ErrorIngreso?> = _error.asStateFlow()

    private val _evento = MutableStateFlow<AdmisionEvento?>(null)
    val evento: StateFlow<AdmisionEvento?> = _evento.asStateFlow()

    private var busquedaJob: Job? = null

    init {
        refrescarCamas()
        cargarMedicos()
    }

    fun refrescarCamas() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoCamas = true, errorCamas = null) }
            val res = refrescarCamasUseCase()
            _uiState.update {
                // Si la cama elegida dejó de estar libre, se descarta la selección.
                val seleccion = it.codigoCamaSeleccionada?.takeIf { cod ->
                    res.getOrNull()?.any { c -> c.codigo == cod && c.estaDisponible } ?: true
                }
                it.copy(
                    cargandoCamas = false,
                    errorCamas = res.exceptionOrNull()?.message,
                    codigoCamaSeleccionada = seleccion
                )
            }
        }
    }

    fun cargarMedicos() {
        viewModelScope.launch {
            cargarMedicosTratantesUseCase().fold(
                onSuccess = { medicos ->
                    _uiState.update { st ->
                        st.copy(
                            medicos = medicos,
                            idMedicoSeleccionado = st.idMedicoSeleccionado?.takeIf { id -> medicos.any { it.id == id } }
                        )
                    }
                },
                onFailure = { avisarError("No se cargaron los médicos", it) }
            )
        }
    }

    fun seleccionarMedico(idMedico: String) {
        _uiState.update { it.copy(idMedicoSeleccionado = idMedico) }
        limpiarErrorCampo("id_medico")
    }

    /** Una cama libre se elige (o se suelta) para el ingreso. */
    fun alternarCama(cama: Cama) {
        if (!cama.estaDisponible) return
        _uiState.update {
            it.copy(codigoCamaSeleccionada = if (it.codigoCamaSeleccionada == cama.codigo) null else cama.codigo)
        }
        limpiarErrorCampo("id_cama")
    }

    fun buscarPacientes(texto: String) {
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(buscandoPacientes = true) }
            val res = buscarPacientesIngresoUseCase(texto)
            _uiState.update { it.copy(pacientesEncontrados = res.getOrDefault(emptyList()), buscandoPacientes = false) }
            res.exceptionOrNull()?.let { avisarError("No se pudo buscar", it) }
        }
    }

    fun limpiarBusqueda() {
        busquedaJob?.cancel()
        _uiState.update { it.copy(pacientesEncontrados = emptyList(), buscandoPacientes = false) }
    }

    fun ingresar(idPaciente: String?, motivo: String, onSuccess: () -> Unit) {
        val st = _uiState.value
        if (st.enviando) return
        val cama = capacidad.value.let { cap ->
            (cap.vipCamas + cap.salasSemiprivadas.flatMap { listOf(it.camaA, it.camaB) })
                .firstOrNull { it.codigo == st.codigoCamaSeleccionada }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true) }
            _error.value = null
            ingresarPacienteUseCase(idPaciente, cama, st.idMedicoSeleccionado, motivo).fold(
                onSuccess = {
                    _evento.value = AdmisionEvento("Ingreso registrado", "Paciente asignado a ${cama?.codigo}.")
                    _uiState.update { it.copy(codigoCamaSeleccionada = null) }
                    onSuccess()
                },
                onFailure = { err ->
                    _error.value = ErrorIngreso((err as? ApiValidationException)?.campo, err.message ?: "Revise los datos.")
                    // Otro puesto pudo ocupar la cama: refrescar el mapa.
                    refrescarCamas()
                }
            )
            _uiState.update { it.copy(enviando = false) }
        }
    }

    fun darAlta(cama: Cama) = operarCama("Alta registrada", "${cama.codigo} quedó libre.") { darAltaUseCase(cama) }

    fun trasladar(origen: Cama, destino: Cama) =
        operarCama("Traslado realizado", "${origen.paciente?.nombreCompleto ?: "Paciente"}: ${origen.codigo} → ${destino.codigo}.") {
            trasladarPacienteUseCase(origen, destino)
        }

    private fun operarCama(titulo: String, mensaje: String, accion: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            accion().fold(
                onSuccess = { _evento.value = AdmisionEvento(titulo, mensaje) },
                onFailure = {
                    avisarError("No se completó la operación", it)
                    refrescarCamas()
                }
            )
        }
    }

    fun limpiarErrorCampo(campo: String) {
        if (_error.value?.campo == campo) _error.value = null
    }

    fun limpiarError() {
        _error.value = null
    }

    fun consumirEvento() {
        _evento.value = null
    }

    private fun avisarError(titulo: String, err: Throwable) {
        _evento.value = AdmisionEvento(titulo, err.message ?: "Intente de nuevo.", esError = true)
    }
}
