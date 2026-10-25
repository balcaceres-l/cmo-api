//
//  RegistroViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Registro de paciente (HU-22): valida antes de enviar y muestra los 422 junto a cada campo.
@Observable
final class RegistroViewModel {
    enum Campo: String, CaseIterable {
        case nombreCompleto = "nombre_completo"
        case usuario, email, dui
        case fechaNacimiento = "fecha_nacimiento"
        case sexo, telefono, direccion, password
        case codigoActivacion = "codigo_activacion"
        case confirmacion
    }

    var nombreCompleto = ""
    var usuario = ""
    var email = ""
    var dui = ""
    var fechaNacimiento = Calendar.current.date(byAdding: .year, value: -30, to: Date()) ?? Date()
    var sexo = "F"
    var telefono = ""
    var direccion = ""
    var password = ""
    var confirmacion = ""
    var codigoActivacion = ""

    private(set) var errores: [Campo: String] = [:]
    private(set) var aviso: AvisoMensaje?
    private(set) var enviando = false

    @ObservationIgnored private let store: SessionStore

    init(store: SessionStore) {
        self.store = store
    }

    private func limpio(_ texto: String) -> String {
        texto.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func opcional(_ texto: String) -> String? {
        let valor = limpio(texto)
        return valor.isEmpty ? nil : valor
    }

    /// Reglas que se revisan antes de llamar a la API.
    func validar() -> [Campo: String] {
        var faltas: [Campo: String] = [:]
        if limpio(nombreCompleto).isEmpty { faltas[.nombreCompleto] = "Escribe tu nombre completo." }
        if limpio(usuario).isEmpty { faltas[.usuario] = "Elige un usuario." }
        if limpio(dui).isEmpty { faltas[.dui] = "Escribe tu DUI." }
        if let correo = opcional(email), !correo.contains("@") { faltas[.email] = "Escribe un correo válido." }
        if password.count < 8 { faltas[.password] = "La contraseña debe tener al menos 8 caracteres." }
        if confirmacion != password { faltas[.confirmacion] = "La confirmación no coincide con la contraseña." }
        if let codigo = opcional(codigoActivacion), !(codigo.count == 6 && codigo.allSatisfy(\.isNumber)) {
            faltas[.codigoActivacion] = "El código de activación tiene 6 dígitos."
        }
        return faltas
    }

    func registrar() async {
        aviso = nil
        errores = validar()
        guard errores.isEmpty else { return }

        enviando = true
        defer { enviando = false }
        let solicitud = SolicitudRegistro(
            nombreCompleto: limpio(nombreCompleto),
            usuario: limpio(usuario),
            email: opcional(email),
            password: password,
            passwordConfirmation: confirmacion,
            dui: limpio(dui),
            fechaNacimiento: Formato.fechaAPI(fechaNacimiento),
            sexo: sexo,
            telefono: opcional(telefono),
            direccion: opcional(direccion),
            codigoActivacion: opcional(codigoActivacion)
        )
        do {
            try await store.registrar(solicitud)
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
        for (llave, texto) in error.erroresPorCampo {
            // La confirmación viaja dentro de `password` (regla `confirmed` de Laravel).
            if let campo = Campo(rawValue: llave) { errores[campo] = texto }
        }
        // Mensaje de la API (por ejemplo, pedir el código de activación en recepción).
        if mensaje != "Los datos enviados no son válidos." || errores.isEmpty {
            aviso = .error(mensaje)
        }
    }
}
