package com.cmo.hospital.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Avisa a la pantalla principal que la API rechazó el token (401): la sesión
 * venció o fue revocada y hay que volver al login.
 */
object EventosSesion {
    private val _sesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

    fun avisarSesionExpirada() {
        _sesionExpirada.tryEmit(Unit)
    }
}
