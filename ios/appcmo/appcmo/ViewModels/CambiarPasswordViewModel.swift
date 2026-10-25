//
//  CambiarPasswordViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Cambio de contraseña (HU-23). Al terminar la API revoca todos los tokens y la app pide entrar de nuevo.
@Observable
final class CambiarPasswordViewModel {
    var actual = ""
    var nueva = ""
    var confirmacion = ""

    private(set) var errorActual: String?
    private(set) var errorNueva: String?
    private(set) var errorConfirmacion: String?
    private(set) var aviso: AvisoMensaje?
    private(set) var enviando = false

    @ObservationIgnored private let store: SessionStore

    init(store: SessionStore) {
        self.store = store
    }

    var puedeEnviar: Bool { !actual.isEmpty && !nueva.isEmpty && !confirmacion.isEmpty && !enviando }

    func cambiar() async {
        errorActual = nil; errorNueva = nil; errorConfirmacion = nil; aviso = nil
        if nueva.count < 8 { errorNueva = "La nueva contraseña debe tener al menos 8 caracteres." }
        if confirmacion != nueva { errorConfirmacion = "La confirmación no coincide." }
        guard errorNueva == nil, errorConfirmacion == nil else { return }

        enviando = true
        defer { enviando = false }
        do {
            try await store.cambiarPassword(SolicitudCambioPassword(passwordActual: actual, password: nueva, passwordConfirmation: confirmacion))
        } catch let error as APIError {
            mostrar(error)
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
        }
    }

    private func mostrar(_ error: APIError) {
        guard case .validacion(let mensaje, _) = error else {
            aviso = .error(error.localizedDescription)
            return
        }
        let campos = error.erroresPorCampo
        errorActual = campos["password_actual"]
        errorNueva = campos["password"]
        // "La contraseña actual es incorrecta." llega sin campo: va junto a la contraseña actual.
        if campos.isEmpty { errorActual = mensaje }
    }
}
