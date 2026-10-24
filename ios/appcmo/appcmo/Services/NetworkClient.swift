//
//  NetworkClient.swift
//  appcmo
//

import Foundation

nonisolated enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case patch = "PATCH"
    case delete = "DELETE"
}

extension Notification.Name {
    /// Se publica cuando una petición autenticada recibe 401 (token vencido o usuario inactivo).
    nonisolated static let apiSesionExpirada = Notification.Name("cmo.api.sesionExpirada")
}

/// Cliente HTTP con `URLSession` y `async/await`. Agrega el token Bearer del Keychain
/// y traduce las respuestas de error de la API a `APIError`.
nonisolated struct NetworkClient: Sendable {
    let baseURL: URL
    private let session: URLSession
    private let keychain: KeychainManager

    init(baseURL: URL = AppConfig.apiBaseURL, session: URLSession = .shared, keychain: KeychainManager = .shared) {
        self.baseURL = baseURL
        self.session = session
        self.keychain = keychain
    }

    /// Petición cuya respuesta trae `data`.
    func request<Respuesta: Decodable & Sendable>(
        _ metodo: HTTPMethod,
        _ ruta: String,
        cuerpo: (any Encodable & Sendable)? = nil,
        autenticada: Bool = true,
        token: String? = nil
    ) async throws -> Respuesta {
        let datos = try await ejecutar(metodo, ruta, cuerpo: cuerpo, autenticada: autenticada, token: token)
        do {
            let sobre = try JSONDecoder().decode(ApiEnvelope<Respuesta>.self, from: datos)
            guard let valor = sobre.data else { throw APIError.respuestaInvalida }
            return valor
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.respuestaInvalida
        }
    }

    /// Petición cuya respuesta no necesita `data` (por ejemplo logout).
    func requestSinRespuesta(
        _ metodo: HTTPMethod,
        _ ruta: String,
        cuerpo: (any Encodable & Sendable)? = nil,
        autenticada: Bool = true,
        token: String? = nil
    ) async throws {
        _ = try await ejecutar(metodo, ruta, cuerpo: cuerpo, autenticada: autenticada, token: token)
    }

    // MARK: - Interno

    private func ejecutar(
        _ metodo: HTTPMethod,
        _ ruta: String,
        cuerpo: (any Encodable & Sendable)?,
        autenticada: Bool,
        token: String?
    ) async throws -> Data {
        var peticion = URLRequest(url: baseURL.appending(path: ruta), timeoutInterval: 20)
        peticion.httpMethod = metodo.rawValue
        peticion.setValue("application/json", forHTTPHeaderField: "Accept")

        if let cuerpo {
            peticion.setValue("application/json", forHTTPHeaderField: "Content-Type")
            peticion.httpBody = try JSONEncoder().encode(cuerpo)
        }
        if autenticada, let credencial = token ?? keychain.leerToken() {
            peticion.setValue("Bearer \(credencial)", forHTTPHeaderField: "Authorization")
        }

        let datos: Data
        let respuesta: URLResponse
        do {
            (datos, respuesta) = try await session.data(for: peticion)
        } catch let error as URLError {
            switch error.code {
            case .cancelled: throw CancellationError()
            case .timedOut: throw APIError.tiempoAgotado
            default: throw APIError.sinConexion
            }
        }

        guard let http = respuesta as? HTTPURLResponse else { throw APIError.respuestaInvalida }
        if (200..<300).contains(http.statusCode) { return datos }

        let error = Self.traducirError(estado: http.statusCode, datos: datos, cabeceras: http)
        if autenticada, http.statusCode == 401 {
            NotificationCenter.default.post(name: .apiSesionExpirada, object: nil)
        }
        throw error
    }

    private static func traducirError(estado: Int, datos: Data, cabeceras: HTTPURLResponse) -> APIError {
        let cuerpo = try? JSONDecoder().decode(ApiErrorBody.self, from: datos)
        let mensaje = cuerpo?.message

        switch estado {
        case 401: return .noAutorizado(mensaje ?? "Tu sesión no es válida. Inicia sesión de nuevo.")
        case 403: return .prohibido(mensaje ?? "No tienes permisos para realizar esta acción.")
        case 409: return .conflicto(mensaje ?? "La acción no se puede completar en este momento.")
        case 422: return .validacion(mensaje ?? "Los datos enviados no son válidos.", cuerpo?.errors ?? [:])
        case 429: return .demasiadosIntentos(segundos: cabeceras.value(forHTTPHeaderField: "Retry-After").flatMap(Int.init))
        case 500...: return .servidor("El servidor tuvo un problema. Inténtalo más tarde.")
        default: return .respuestaInvalida
        }
    }
}
