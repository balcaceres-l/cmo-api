package com.cmo.hospital.features.doctor.inicio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.AmbulanciaRecurso
import com.cmo.hospital.core.realtime.EventoTiempoReal
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.CargarAmbulanciaMedicoUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.EnviarReporteAmbulanciaUseCase
import com.cmo.hospital.features.recepcionista.ambulancia.domain.usecase.ObservarAmbulanciaEnVivoUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AmbulanciaMedicoUiState(
    val ambulancia: AmbulanciaRecurso? = null,
    val error: String? = null,
    val enVivo: Boolean = false,
    val reportesEnVivo: Boolean = false,
    val enviando: Boolean = false,
    /** Mensaje del último envío (éxito o error) para el diálogo de reporte. */
    val resultadoReporte: Result<String>? = null
)

/**
 * Tarjeta de ambulancia en el inicio del médico: estado, prioridad y último
 * reporte clínico del traslado (la API no le muestra paciente ni dirección).
 * Se actualiza por el canal público `ambulancia` y el privado `personal`.
 */
class AmbulanciaMedicoViewModel(
    private val cargarAmbulanciaMedicoUseCase: CargarAmbulanciaMedicoUseCase,
    private val enviarReporteAmbulanciaUseCase: EnviarReporteAmbulanciaUseCase,
    private val observarAmbulanciaEnVivoUseCase: ObservarAmbulanciaEnVivoUseCase,
    private val reintentoEnVivoMs: Long = 5_000
) : ViewModel() {

    private val _estado = MutableStateFlow(AmbulanciaMedicoUiState())
    val estado: StateFlow<AmbulanciaMedicoUiState> = _estado.asStateFlow()

    init {
        refrescar()
        escuchar({ observarAmbulanciaEnVivoUseCase() }) { conectado -> _estado.update { it.copy(enVivo = conectado) } }
        escuchar({ observarAmbulanciaEnVivoUseCase.reportes() }) { conectado -> _estado.update { it.copy(reportesEnVivo = conectado) } }
    }

    fun refrescar() {
        viewModelScope.launch {
            val res = cargarAmbulanciaMedicoUseCase()
            _estado.update {
                it.copy(ambulancia = res.getOrNull() ?: it.ambulancia, error = res.exceptionOrNull()?.message)
            }
        }
    }

    private fun escuchar(canal: () -> Flow<EventoTiempoReal>, alCambiarConexion: (Boolean) -> Unit) {
        viewModelScope.launch {
            while (isActive) {
                try {
                    canal().collect { ev ->
                        when (ev) {
                            EventoTiempoReal.Conectado -> alCambiarConexion(true)
                            is EventoTiempoReal.Mensaje -> refrescar()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
                alCambiarConexion(false)
                delay(reintentoEnVivoMs)
            }
        }
    }

    /** Reporte del médico de emergencia que va en la ambulancia. */
    fun enviarReporte(idServicio: String, estadoPaciente: String?, observaciones: String?, minutos: String) {
        if (_estado.value.enviando) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, resultadoReporte = null) }
            val res = enviarReporteAmbulanciaUseCase(idServicio, estadoPaciente, observaciones, minutos)
            _estado.update {
                it.copy(enviando = false, resultadoReporte = res.map { "Reporte enviado a emergencia." })
            }
            if (res.isSuccess) refrescar()
        }
    }

    fun consumirResultado() {
        _estado.update { it.copy(resultadoReporte = null) }
    }
}
