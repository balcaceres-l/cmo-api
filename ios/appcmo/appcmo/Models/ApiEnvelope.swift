//
//  ApiEnvelope.swift
//  appcmo
//

import Foundation

/// Respuesta correcta de la API: `{ success, message, data }`.
nonisolated struct ApiEnvelope<Datos: Decodable & Sendable>: Decodable, Sendable {
    let success: Bool
    let message: String?
    let data: Datos?
}

/// Respuesta de error de la API: `{ success: false, message, errors }`.
/// `errors` solo trae contenido en validaciones (422) y sus llaves se conservan tal cual (`codigo_activacion`).
nonisolated struct ApiErrorBody: Decodable, Sendable {
    let message: String?
    let errors: [String: [String]]?
}
