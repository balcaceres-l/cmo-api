//
//  LoginViewModel.swift
//  appcmo
//

import Foundation
import Observation

@Observable
final class LoginViewModel {
    enum Aviso: Equatable {
        case error(String)
        case informacion(String)

        var texto: String {
            switch self {
            case .error(let texto), .informacion(let texto): texto
            }
        }
    }

    var usuario = ""
    var password = ""

    private(set) var estaCargando = false
    private(set) var aviso: Aviso?
    private(set) var errorUsuario: String?
    private(set) var errorPassword: String?
    /// Tras un 429 el botón queda deshabilitado hasta que pase la espera.
    private(set) var enEspera = false

    @ObservationIgnored private let store: SessionStore
    @ObservationIgnored private var tareaEspera: Task<Void, Never>?

    init(store: SessionStore) {
        self.store = store
        if let mensaje = store.avisoLogin {
            aviso = .informacion(mensaje)
        }
    }

    deinit {
        tareaEspera?.cancel()
    }

    private var usuarioLimpio: String {
        usuario.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var puedeEnviar: Bool {
        !usuarioLimpio.isEmpty && !password.isEmpty && !estaCargando && !enEspera
    }

    func enviar() async {
        guard puedeEnviar else { return }
        estaCargando = true
        aviso = nil
        errorUsuario = nil
        errorPassword = nil
        defer { estaCargando = false }

        do {
            let resultado = try await store.iniciarSesion(usuario: usuarioLimpio, password: password)
            switch resultado {
            case .ingreso:
                password = ""
            case .rolNoPermitido:
                // Mismo texto que la API da por una clave incorrecta: no revela que la cuenta existe ni a qué plataforma pertenece.
                aviso = .error("Credenciales incorrectas.")
            }
        } catch is CancellationError {
            return
        } catch let error as APIError {
            mostrar(error)
        } catch {
            aviso = .error(error.localizedDescription)
        }
    }

    private func mostrar(_ error: APIError) {
        switch error {
        case .validacion:
            let campos = error.erroresPorCampo
            errorUsuario = campos["usuario"]
            errorPassword = campos["password"]
            if campos.isEmpty { aviso = .error(error.localizedDescription) }
        case .demasiadosIntentos(let segundos):
            aviso = .error(error.localizedDescription)
            iniciarEspera(segundos: segundos ?? 30)
        default:
            aviso = .error(error.localizedDescription)
        }
    }

    private func iniciarEspera(segundos: Int) {
        enEspera = true
        tareaEspera?.cancel()
        tareaEspera = Task { [weak self] in
            try? await Task.sleep(for: .seconds(max(segundos, 1)))
            guard !Task.isCancelled else { return }
            self?.enEspera = false
        }
    }
}
