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
import com.cmo.hospital.features.doctor.consulta.domain.usecase.TickCronometroConsultaUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.CambiarEstadoOrdenLabUseCase
import com.cmo.hospital.features.doctor.laboratorio.domain.usecase.EmitirOrdenLabUseCase
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
    private val emitirOrdenLabUseCase: EmitirOrdenLabUseCase,
    private val cambiarEstadoOrdenLabUseCase: CambiarEstadoOrdenLabUseCase
) : ViewModel() {

    val consultaState: StateFlow<ConsultaActiva> = getConsultaActivaFlowUseCase()

    private val _isCronometroCorriendo = MutableStateFlow(true)
    val isCronometroCorriendo: StateFlow<Boolean> = _isCronometroCorriendo.asStateFlow()

    private val _toastEvent = MutableStateFlow<Triple<String, String, Boolean>?>(null)
    val toastEvent: StateFlow<Triple<String, String, Boolean>?> = _toastEvent.asStateFlow()

    init {
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

    fun guardarSignos(signos: SignosVitales) {
        actualizarSignosConsultaUseCase(signos)
        _toastEvent.value = Triple(
            "Signos Vitales Registrados",
            "Parámetros clínicos sincronizados en Consulta y Expediente.",
            false
        )
    }

    fun agregarCie10(codigo: String, descripcion: String) {
        gestionarCie10ConsultaUseCase.agregar(codigo, descripcion).fold(
            onSuccess = {
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
    }

    fun actualizarAnamnesis(texto: String) {
        if (texto != consultaState.value.anamnesis) {
            guardarAnamnesisUseCase(texto)
        }
    }

    fun agregarReceta(
        medicamento: String,
        dosis: String,
        frecuencia: String,
        duracion: String,
        indicaciones: String?,
        tipo: String
    ): Boolean {
        return gestionarRecetaConsultaUseCase.agregar(
            medicamento,
            dosis,
            frecuencia,
            duracion,
            indicaciones,
            tipo
        ).fold(
            onSuccess = {
                _toastEvent.value = Triple(
                    "Fármaco Prescrito",
                    "$medicamento agregado al recetario electrónico HUC.",
                    false
                )
                true
            },
            onFailure = { err ->
                _toastEvent.value = Triple(
                    "Campos Obligatorios",
                    err.message ?: "Complete medicamento, dosis y frecuencia.",
                    true
                )
                false
            }
        )
    }

    fun eliminarReceta(recetaId: String) {
        gestionarRecetaConsultaUseCase.eliminar(recetaId)
    }

    fun emitirOrdenLabEnConsulta(
        examenes: List<String>,
        prioridad: String,
        obs: String,
        estadoInicial: String
    ) {
        val pacienteActivo = consultaState.value.paciente
        emitirOrdenLabUseCase(examenes, prioridad, obs, pacienteActivo, estadoInicial).fold(
            onSuccess = { creadas ->
                _toastEvent.value = Triple(
                    "Orden LIS Emitida desde Box",
                    "${creadas.size} examen(es) enviados a LIS y vinculados a ${pacienteActivo.numeroExpediente}.",
                    false
                )
            },
            onFailure = { err ->
                _toastEvent.value = Triple(
                    "Orden LIS Incompleta",
                    err.message ?: "Seleccione al menos un examen.",
                    true
                )
            }
        )
    }

    fun cambiarEstadoOrdenLab(ordenId: String, nuevoEstado: String) {
        cambiarEstadoOrdenLabUseCase(ordenId, nuevoEstado).onSuccess {
            _toastEvent.value = Triple(
                "Estado LIS Actualizado",
                "Orden cambiada a '$nuevoEstado' en tiempo real.",
                false
            )
        }
    }

    fun finalizarConsulta(onFinalizada: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isCronometroCorriendo.value = false
            val msg = finalizarConsultaUseCase()
            _toastEvent.value = Triple("Consulta Cerrada y Firmada", msg, false)
            onFinalizada?.invoke()
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }
}
