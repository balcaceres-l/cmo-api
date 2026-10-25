//
//  APIError.swift
//  appcmo
//

import Foundation

/// Errores de la API traducidos a texto legible. La API responde siempre
/// `{ success, message, errors }` (ver backend/docs/API.md §1).
nonisolated enum APIError: LocalizedError, Equatable {
    case sinConexion
    case tiempoAgotado
    case noAutorizado(String)                          // 401
    case prohibido(String)                             // 403
    case noEncontrado(String)                          // 404
    case conflicto(String)                             // 409
    case validacion(String, [String: [String]])        // 422
    case demasiadosIntentos(segundos: Int?)            // 429
    case servidor(String)                              // 5xx
    case respuestaInvalida

    var errorDescription: String? {
        switch self {
        case .sinConexion:
            "No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo."
        case .tiempoAgotado:
            "El servidor tardó demasiado en responder. Inténtalo de nuevo."
        case .noAutorizado(let mensaje), .prohibido(let mensaje), .noEncontrado(let mensaje), .conflicto(let mensaje), .servidor(let mensaje):
            mensaje
        case .validacion(let mensaje, _):
            mensaje
        case .demasiadosIntentos(let segundos):
            if let segundos {
                "Demasiados intentos. Espera \(segundos) segundos antes de volver a intentar."
            } else {
                "Demasiados intentos. Espera un momento antes de volver a intentar."
            }
        case .respuestaInvalida:
            "La respuesta del servidor no es válida."
        }
    }

    /// Mensajes por campo de un 422 (`errors`), con el primer mensaje de cada campo.
    var erroresPorCampo: [String: String] {
        guard case .validacion(_, let campos) = self else { return [:] }
        return campos.compactMapValues(\.first)
    }
}
