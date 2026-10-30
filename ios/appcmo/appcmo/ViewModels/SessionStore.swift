//
//  SessionStore.swift
//  appcmo
//

import Foundation
import Observation

/// Resultado de un intento de login que la API aceptó.
enum ResultadoLogin: Equatable {
    case ingreso
    /// Credenciales válidas, pero el rol no usa esta app. No se guarda ninguna sesión.
    case rolNoPermitido
}

/// Estado de sesión de toda la app: decide qué pantalla ve cada rol.
@Observable
final class SessionStore {
    enum Estado: Equatable {
        case restaurando
        case noAutenticado
        case autenticado(Sesion)
    }

    private(set) var estado: Estado = .restaurando
    /// Mensaje para mostrar en el login (por ejemplo "tu sesión venció").
    private(set) var avisoLogin: String?

    @ObservationIgnored private let auth: AuthRepository
    @ObservationIgnored private let keychain: KeychainManager

    init(auth: AuthRepository = AuthRepository(), keychain: KeychainManager = .shared) {
        self.auth = auth
        self.keychain = keychain
    }

    // MARK: - Inicio de la app

    /// Revisa si hay un token guardado y lo valida contra `GET /auth/me`.
    func restaurarSesion() async {
        guard keychain.leerToken() != nil else {
            estado = .noAutenticado
            return
        }
        do {
            let yo = try await auth.me()
            if yo.usuario.rol.puedeUsarApp {
                estado = .autenticado(Sesion(usuario: yo.usuario, idPaciente: yo.idPaciente))
            } else {
                descartarSesion(aviso: nil)
            }
        } catch is CancellationError {
            return
        } catch APIError.noAutorizado {
            descartarSesion(aviso: "Tu sesión venció. Inicia sesión de nuevo.")
        } catch {
            // Sin red no se puede confirmar la sesión: se conserva el token y se pide entrar de nuevo.
            estado = .noAutenticado
            avisoLogin = "No se pudo verificar tu sesión. Revisa tu conexión e inicia sesión."
        }
    }

    // MARK: - Login y logout

    func iniciarSesion(usuario: String, password: String) async throws -> ResultadoLogin {
        avisoLogin = nil
        let respuesta = try await auth.login(usuario: usuario, password: password)

        guard respuesta.usuario.rol.puedeUsarApp else {
            // La API ya emitió un token: se revoca y nunca se guarda.
            try? await auth.logout(token: respuesta.token)
            return .rolNoPermitido
        }

        try keychain.guardarToken(respuesta.token)
        estado = .autenticado(Sesion(usuario: respuesta.usuario, idPaciente: respuesta.idPaciente))
        return .ingreso
    }

    /// Crea la cuenta de un paciente: la API responde como un login y la sesión queda iniciada.
    func registrar(_ solicitud: SolicitudRegistro) async throws {
        let respuesta = try await auth.registro(solicitud)
        try keychain.guardarToken(respuesta.token)
        avisoLogin = nil
        estado = .autenticado(Sesion(usuario: respuesta.usuario, idPaciente: respuesta.idPaciente))
    }

    func cambiarPassword(_ solicitud: SolicitudCambioPassword) async throws {
        try await auth.cambiarPassword(solicitud)
        // La API ya revocó todos los tokens: no hace falta llamar a logout.
        descartarSesion(aviso: "Contraseña actualizada. Inicia sesión de nuevo.")
    }

    var sesion: Sesion? {
        if case .autenticado(let sesion) = estado { sesion } else { nil }
    }

    func cerrarSesion() async {
        // Aunque la API no responda, la sesión local se cierra.
        try? await auth.logout()
        descartarSesion(aviso: nil)
    }

    /// Una petición autenticada recibió 401: el token venció o el usuario quedó inactivo.
    func sesionExpirada() {
        guard case .autenticado = estado else { return }
        descartarSesion(aviso: "Tu sesión venció. Inicia sesión de nuevo.")
    }

    private func descartarSesion(aviso: String?) {
        keychain.borrarToken()
        avisoLogin = aviso
        estado = .noAutenticado
    }
}
