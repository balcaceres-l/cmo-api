package com.cmo.hospital.features.doctor.consulta.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmo.hospital.core.domain.model.ConsultaActiva
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.features.doctor.consulta.domain.usecase.ActualizarSignosConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.FinalizarConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GestionarCie10ConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GestionarRecetaConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GetConsultaActivaFlowUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GuardarAnamnesisUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.GuardarCambiosConsultaUseCase
import com.cmo.hospital.features.doctor.consulta.domain.usecase.TickCronometroConsultaUseCase
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CancelarOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsultaViewModel(
    getConsultaActivaFlowUseCase: GetConsultaActivaFlowUseCase,
    private val tickCronometroConsultaUseCase: TickCronometroConsultaUseCase,
    private val actualizarSignosConsultaUseCase: ActualizarSignosConsultaUseCase,
    private val gestionarCie10ConsultaUseCase: GestionarCie10ConsultaUseCase,
    private val guardarAnamnesisUseCase: GuardarAnamnesisUseCase,
    private val gestionarRecetaConsultaUseCase: GestionarRecetaConsultaUseCase,
    private val finalizarConsultaUseCase: FinalizarConsultaUseCase,
    private val guardarCambiosConsultaUseCase: GuardarCambiosConsultaUseCase,
    private val emitirOrdenLabUseCase: EmitirOrdenLabUseCase,
    private val cancelarOrdenLabUseCase: CancelarOrdenLabUseCase
) : ViewModel() {

    val consultaState: StateFlow<ConsultaActiva> = getConsultaActivaFlowUseCase()

    private val _isCronometroCorriendo = MutableStateFlow(true)
    val isCronometroCorriendo: StateFlow<Boolean> = _isCronometroCorriendo.asStateFlow()

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    init {
        // El catálogo de exámenes se carga al entrar para tenerlo listo al indicar una orden.
        viewModelScope.launch { emitirOrdenLabUseCase.cargarCatalogo() }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val actual = consultaState.value
                if (_isCronometroCorriendo.value && !actual.finalizada) {
                    tickCronometroConsultaUseCase(actual.segundosTranscurridos + 1)
                }
            }
        }
    }

    fun toggleCronometro() {
        _isCronometroCorriendo.value = !_isCronometroCorriendo.value
    }

    private var guardadoJob: Job? = null

    /**
     * Diagnóstico y anamnesis se guardan en la API poco después del último cambio,
     * para no enviar una petición por cada tecla.
     */
    private fun programarGuardado() {
        guardadoJob?.cancel()
        guardadoJob = viewModelScope.launch {
            delay(800L)
            guardarCambiosConsultaUseCase().onFailure { err ->
                _toastEvent.value = Triple(
                    "Cambios no guardados",
                    err.message ?: "No se pudo guardar la consulta en el servidor.",
                    true
                )
            }
        }
    }

    fun guardarSignos(signos: SignosVitales) {
        actualizarSignosConsultaUseCase(signos)
        _toastEvent.value = Triple(
            "Signos Vitales Registrados",
            "Parámetros clínicos actualizados en la consulta.",
            false
        )
    }

    fun agregarCie10(codigo: String, descripcion: String) {
        gestionarCie10ConsultaUseCase.agregar(codigo, descripcion).fold(
            onSuccess = {
                programarGuardado()
                _toastEvent.value = Triple(
                    "CIE-10 Añadido",
                    "Código $codigo vinculado a la hipótesis diagnóstica.",
                    false
                )
            },
            onFailure = { err ->
                _toastEvent.value = Triple("Error CIE-10", err.message ?: "Código inválido", true)
            }
        )
    }

    fun eliminarCie10(codigo: String) {
        gestionarCie10ConsultaUseCase.eliminar(codigo)
        programarGuardado()
    }

    fun actualizarAnamnesis(texto: String) {
        if (texto != consultaState.value.anamnesis) {
            guardarAnamnesisUseCase(texto)
            programarGuardado()
        }
    }

    /** Devuelve true si los datos son válidos (cierra el diálogo); el guardado sigue en segundo plano. */
    fun agregarReceta(
        medicamento: String,
        dosis: String,
        frecuencia: String,
        duracion: String,
        indicaciones: String?,
        tipo: String
    ): Boolean {
        val item = gestionarRecetaConsultaUseCase.validar(
            medicamento,
            dosis,
            frecuencia,
            duracion,
            indicaciones,
            tipo
        ).getOrElse { err ->
            _toastEvent.value = Triple(
                "Campos Obligatorios",
                err.message ?: "Complete medicamento, dosis y frecuencia.",
                true
            )
            return false
        }
        viewModelScope.launch {
            gestionarRecetaConsultaUseCase.guardar(item).fold(
                onSuccess = {
                    _toastEvent.value = Triple(
                        "Fármaco Prescrito",
                        "${it.medicamento} guardado en la receta de la consulta.",
                        false
                    )
                },
                onFailure = { err ->
                    _toastEvent.value = Triple(
                        "Receta no guardada",
                        err.message ?: "No se pudo guardar la receta.",
                        true
                    )
                }
            )
        }
        return true
    }

    fun eliminarReceta(recetaId: String) {
        viewModelScope.launch {
            gestionarRecetaConsultaUseCase.eliminar(recetaId).onFailure { err ->
                _toastEvent.value = Triple(
                    "Receta no eliminada",
                    err.message ?: "No se pudo eliminar la receta.",
                    true
                )
            }
        }
    }

    fun catalogoExamenes(): List<String> = emitirOrdenLabUseCase.catalogo()

    fun emitirOrdenLabEnConsulta(
        examenes: List<String>,
        prioridad: String,
        obs: String
    ) {
        val pacienteActivo = consultaState.value.paciente
        viewModelScope.launch {
            emitirOrdenLabUseCase(examenes, prioridad, obs, pacienteActivo).fold(
                onSuccess = { creadas ->
                    _toastEvent.value = Triple(
                        "Orden de Laboratorio Emitida",
                        "${creadas.size} examen(es) enviados al laboratorio como Pendiente.",
                        false
                    )
                },
                onFailure = { err ->
                    _toastEvent.value = Triple(
                        "Orden no emitida",
                        err.message ?: "Seleccione al menos un examen.",
                        true
                    )
                }
            )
        }
    }

    fun cancelarOrdenLab(orden: OrdenLaboratorioItem) {
        viewModelScope.launch {
            cancelarOrdenLabUseCase(orden).fold(
                onSuccess = {
                    _toastEvent.value = Triple("Orden Cancelada", "${it.examenNombre} ya no se realizará.", false)
                },
                onFailure = { err ->
                    _toastEvent.value = Triple("No se pudo cancelar", err.message ?: "Intente de nuevo.", true)
                }
            )
        }
    }

    fun finalizarConsulta(onFinalizada: (() -> Unit)? = null) {
        // El cierre guarda los últimos cambios; no hace falta el guardado pendiente.
        guardadoJob?.cancel()
        viewModelScope.launch {
            finalizarConsultaUseCase().fold(
                onSuccess = { msg ->
                    _isCronometroCorriendo.value = false
                    _toastEvent.value = Triple("Consulta Cerrada", msg, false)
                    onFinalizada?.invoke()
                },
                onFailure = { err ->
                    _toastEvent.value = Triple(
                        "No se pudo finalizar",
                        err.message ?: "La consulta sigue abierta.",
                        true
                    )
                }
            )
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
