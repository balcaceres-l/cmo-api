package com.cmo.hospital.features.recepcionista.admision.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.CapacidadHospitalaria
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.features.recepcionista.admision.domain.usecase.*
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GuardarPacienteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    observeHabitacionesDetalladasUseCase: ObserveHabitacionesDetalladasUseCase,
    private val refrescarCamasUseCase: RefrescarCamasUseCase,
    private val cargarMedicosTratantesUseCase: CargarMedicosTratantesUseCase,
    private val buscarPacientesIngresoUseCase: BuscarPacientesIngresoUseCase,
    private val ingresarPacienteUseCase: IngresarPacienteUseCase,
    private val darAltaUseCase: DarAltaUseCase,
    private val trasladarPacienteUseCase: TrasladarPacienteUseCase,
    private val guardarPacienteUseCase: GuardarPacienteUseCase? = null
) : ViewModel() {

    val capacidad: StateFlow<CapacidadHospitalaria> = observeCapacidadCamasUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CapacidadHospitalaria(0, 0, 0, emptyList(), emptyList())
        )

    // ── Pestaña "Vista de habitaciones" (filtros por tipo/planta, ocupación y búsqueda) ──
    val habitaciones: StateFlow<List<HabitacionDetalleUi>> = observeHabitacionesDetalladasUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filtroTipoHabitacion = MutableStateFlow("Todas")
    val filtroTipoHabitacion: StateFlow<String> = _filtroTipoHabitacion.asStateFlow()

    private val _filtroOcupacionHabitacion = MutableStateFlow("Todas")
    val filtroOcupacionHabitacion: StateFlow<String> = _filtroOcupacionHabitacion.asStateFlow()

    private val _queryBusquedaHabitacion = MutableStateFlow("")
    val queryBusquedaHabitacion: StateFlow<String> = _queryBusquedaHabitacion.asStateFlow()

    val habitacionesFiltradas: StateFlow<List<HabitacionDetalleUi>> = combine(
        habitaciones, _filtroTipoHabitacion, _filtroOcupacionHabitacion, _queryBusquedaHabitacion
    ) { lista, tipo, ocupacion, query ->
        filtrarHabitaciones(lista, tipo, ocupacion, query)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        cargarPacientesRegistrados()
    }

    fun refrescarCamas() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoCamas = true, errorCamas = null) }
            val res = refrescarCamasUseCase()
            _uiState.update {
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

    /** Carga la lista de todos los pacientes registrados para mostrarlos en el selector desplegable. */
    fun cargarPacientesRegistrados() {
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            _uiState.update { it.copy(buscandoPacientes = true) }
            val res = buscarPacientesIngresoUseCase("")
            _uiState.update {
                it.copy(
                    pacientesEncontrados = res.getOrDefault(emptyList()),
                    buscandoPacientes = false
                )
            }
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
            delay(250)
            _uiState.update { it.copy(buscandoPacientes = true) }
            val res = buscarPacientesIngresoUseCase(texto)
            _uiState.update { it.copy(pacientesEncontrados = res.getOrDefault(emptyList()), buscandoPacientes = false) }
            res.exceptionOrNull()?.let { avisarError("No se pudo buscar", it) }
        }
    }

    fun limpiarBusqueda() {
        cargarPacientesRegistrados()
    }

    /** Registra un paciente nuevo y lo agrega al listado disponible para el ingreso. */
    fun registrarNuevoPaciente(
        form: PacienteForm,
        onGuardado: (Paciente) -> Unit,
        onTerminado: (ErrorIngreso?) -> Unit
    ) {
        val useCase = guardarPacienteUseCase
        if (useCase == null) {
            onTerminado(ErrorIngreso(null, "Servicio de registro de pacientes no disponible."))
            return
        }
        viewModelScope.launch {
            useCase(null, form).fold(
                onSuccess = { creado ->
                    _uiState.update { st ->
                        st.copy(pacientesEncontrados = listOf(creado) + st.pacientesEncontrados.filter { it.id != creado.id })
                    }
                    _evento.value = AdmisionEvento(
                        "Paciente registrado",
                        "${creado.nombreCompleto} • Exp. ${creado.numeroExpediente ?: "asignado"}"
                    )
                    onGuardado(creado)
                    onTerminado(null)
                },
                onFailure = { err ->
                    onTerminado(
                        ErrorIngreso(
                            campo = (err as? ApiValidationException)?.campo,
                            mensaje = err.message ?: "Revise los datos del paciente."
                        )
                    )
                }
            )
        }
    }

    /**
     * Si el recepcionista eligió registrar un nuevo paciente directamente en la sección de
     * Ingreso a cama, primero valida y crea al paciente y luego ejecuta el ingreso en la cama elegida.
     */
    fun ingresarConNuevoPaciente(
        form: PacienteForm,
        motivo: String,
        onPacienteCreado: (Paciente) -> Unit,
        onSuccess: () -> Unit
    ) {
        val st = _uiState.value
        if (st.enviando) return
        val cama = capacidad.value.let { cap ->
            (cap.vipCamas + cap.salasSemiprivadas.flatMap { listOf(it.camaA, it.camaB) })
                .firstOrNull { it.codigo == st.codigoCamaSeleccionada }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(enviando = true) }
            _error.value = null

            // Validar primero los campos del paciente nuevo
            val errorValidacion = GuardarPacienteUseCase.validarCampo(form.copy(exigirDatosCompletos = true))
            if (errorValidacion != null) {
                _error.value = ErrorIngreso(errorValidacion.first, errorValidacion.second)
                _uiState.update { it.copy(enviando = false) }
                return@launch
            }
            if (st.idMedicoSeleccionado.isNullOrBlank()) {
                _error.value = ErrorIngreso("id_medico", "Seleccione el médico tratante.")
                _uiState.update { it.copy(enviando = false) }
                return@launch
            }
            if (motivo.isBlank()) {
                _error.value = ErrorIngreso("motivo", "Ingrese el motivo del ingreso.")
                _uiState.update { it.copy(enviando = false) }
                return@launch
            }
            if (cama == null) {
                _error.value = ErrorIngreso("id_cama", "Seleccione una cama libre en el mapa.")
                _uiState.update { it.copy(enviando = false) }
                return@launch
            }

            val useCase = guardarPacienteUseCase
            if (useCase == null) {
                _error.value = ErrorIngreso(null, "Servicio de registro de pacientes no disponible.")
                _uiState.update { it.copy(enviando = false) }
                return@launch
            }

            useCase(null, form.copy(exigirDatosCompletos = true)).fold(
                onSuccess = { nuevoPac ->
                    _uiState.update { curr ->
                        curr.copy(pacientesEncontrados = listOf(nuevoPac) + curr.pacientesEncontrados.filter { it.id != nuevoPac.id })
                    }
                    onPacienteCreado(nuevoPac)
                    ingresarPacienteUseCase(nuevoPac.id, cama, st.idMedicoSeleccionado, motivo).fold(
                        onSuccess = {
                            _evento.value = AdmisionEvento(
                                "Paciente creado e ingresado",
                                "${nuevoPac.nombreCompleto} asignado a ${cama.codigo}."
                            )
                            _uiState.update { it.copy(codigoCamaSeleccionada = null) }
                            onSuccess()
                        },
                        onFailure = { err ->
                            _error.value = ErrorIngreso((err as? ApiValidationException)?.campo, err.message ?: "Revise los datos del ingreso.")
                            refrescarCamas()
                        }
                    )
                },
                onFailure = { err ->
                    _error.value = ErrorIngreso((err as? ApiValidationException)?.campo, err.message ?: "Revise los datos del paciente.")
                }
            )
            _uiState.update { it.copy(enviando = false) }
        }
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

    companion object {
        fun filtrarHabitaciones(
            lista: List<HabitacionDetalleUi>,
            tipo: String,
            ocupacion: String,
            query: String
        ): List<HabitacionDetalleUi> = lista.filter { hab ->
            val matchTipo = when (tipo) {
                "VIP" -> hab.tipo.contains("VIP", ignoreCase = true)
                "Semiprivadas" -> hab.tipo.contains("Semiprivada", ignoreCase = true)
                else -> true
            }
            val matchOcupacion = when (ocupacion) {
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
                        cama.paciente?.numeroExpediente?.contains(query, ignoreCase = true) == true
                }
            matchTipo && matchOcupacion && matchQuery
        }
    }
}
