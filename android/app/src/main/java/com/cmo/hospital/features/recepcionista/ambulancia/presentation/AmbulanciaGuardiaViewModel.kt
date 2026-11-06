package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.network.ApiValidationException
import com.cmo.hospital.core.realtime.EventoTiempoReal
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AmbulanciaUiState(
    val panel: PanelAmbulancia? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val enVivo: Boolean = false,
    val enviando: Boolean = false
)

/** Diálogo donde se muestra un error de validación o de la API. */
enum class DialogoAmbulancia { DESPACHO, REPORTE }

data class ErrorDialogo(val dialogo: DialogoAmbulancia, val campo: String?, val mensaje: String)

data class AvisoAmbulancia(val titulo: String, val mensaje: String, val esError: Boolean = false)

class AmbulanciaGuardiaViewModel(
    private val cargarPanelAmbulanciaUseCase: CargarPanelAmbulanciaUseCase,
    private val solicitarDespachoAmbulanciaUseCase: SolicitarDespachoAmbulanciaUseCase,
    private val finalizarServicioAmbulanciaUseCase: FinalizarServicioAmbulanciaUseCase,
    private val enviarReporteAmbulanciaUseCase: EnviarReporteAmbulanciaUseCase,
    private val observarAmbulanciaEnVivoUseCase: ObservarAmbulanciaEnVivoUseCase,
    private val reintentoEnVivoMs: Long = 5_000
) : ViewModel() {

    private val _estado = MutableStateFlow(AmbulanciaUiState())
    val estado: StateFlow<AmbulanciaUiState> = _estado.asStateFlow()

    private val _errorDialogo = MutableStateFlow<ErrorDialogo?>(null)
    val errorDialogo: StateFlow<ErrorDialogo?> = _errorDialogo.asStateFlow()

    private val _aviso = MutableStateFlow<AvisoAmbulancia?>(null)
    val aviso: StateFlow<AvisoAmbulancia?> = _aviso.asStateFlow()

    init {
        refrescar()
        escucharEnVivo()
    }

    fun refrescar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val res = cargarPanelAmbulanciaUseCase()
            _estado.update {
                it.copy(
                    cargando = false,
                    panel = res.getOrNull() ?: it.panel,
                    error = res.exceptionOrNull()?.message
                )
            }
        }
    }

    /**
     * Canal público `ambulancia` de Reverb: cada evento (despacho, cierre,
     * cambio de estado desde el panel web) recarga el panel. Si la conexión
     * se cae, se reintenta; mientras tanto la pantalla se refresca al volver.
     */
    private fun escucharEnVivo() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    observarAmbulanciaEnVivoUseCase().collect { ev ->
                        when (ev) {
                            EventoTiempoReal.Conectado -> _estado.update { it.copy(enVivo = true) }
                            is EventoTiempoReal.Mensaje -> refrescar()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
                _estado.update { it.copy(enVivo = false) }
                delay(reintentoEnVivoMs)
            }
        }
    }

    fun despachar(direccion: String, paciente: String, descripcion: String, prioridad: String?, onSuccess: () -> Unit) =
        enviar(DialogoAmbulancia.DESPACHO, onSuccess) {
            solicitarDespachoAmbulanciaUseCase(direccion, paciente, descripcion, prioridad).map {
                AvisoAmbulancia("Ambulancia despachada", "Prioridad ${CatalogoAmbulancia.etiqueta(it.prioridad)} rumbo a ${it.direccion}.")
            }
        }

    fun enviarReporte(idServicio: String, estadoPaciente: String?, observaciones: String?, minutos: String, onSuccess: () -> Unit) =
        enviar(DialogoAmbulancia.REPORTE, onSuccess) {
            enviarReporteAmbulanciaUseCase(idServicio, estadoPaciente, observaciones, minutos).map {
                AvisoAmbulancia("Reporte enviado", "Emergencia recibió el estado ${CatalogoAmbulancia.etiqueta(it.estadoPaciente)}.")
            }
        }

    fun finalizar(idServicio: String, notas: String?) {
        viewModelScope.launch {
            finalizarServicioAmbulanciaUseCase(idServicio, notas).fold(
                onSuccess = {
                    _aviso.value = AvisoAmbulancia("Servicio finalizado", "La ambulancia quedó disponible.")
                    refrescar()
                },
                onFailure = { _aviso.value = AvisoAmbulancia("No se finalizó", it.message ?: "Intente de nuevo.", true) }
            )
        }
    }

    private fun enviar(dialogo: DialogoAmbulancia, onSuccess: () -> Unit, accion: suspend () -> Result<AvisoAmbulancia>) {
        if (_estado.value.enviando) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true) }
            _errorDialogo.value = null
            accion().fold(
                onSuccess = {
                    _aviso.value = it
                    onSuccess()
                    refrescar()
                },
                onFailure = { err ->
                    _errorDialogo.value = ErrorDialogo(dialogo, (err as? ApiValidationException)?.campo, err.message ?: "Revise los datos.")
                }
            )
            _estado.update { it.copy(enviando = false) }
        }
    }

    fun limpiarErrorDialogo() {
        _errorDialogo.value = null
    }

    fun consumirAviso() {
        _aviso.value = null
    }
}
