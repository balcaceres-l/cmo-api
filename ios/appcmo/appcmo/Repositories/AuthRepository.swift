//
//  AuthRepository.swift
//  appcmo
//

import Foundation

/// Endpoints de autenticación (`/auth/*`).
nonisolated struct AuthRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    private struct LoginRequest: Encodable, Sendable {
        let usuario: String
        let password: String
    }

    func login(usuario: String, password: String) async throws -> LoginResponse {
        try await cliente.request(
            .post, "auth/login",
            cuerpo: LoginRequest(usuario: usuario, password: password),
            autenticada: false
        )
    }

    func me() async throws -> MeResponse {
        try await cliente.request(.get, "auth/me")
    }

    /// Revoca el token indicado o, si es `nil`, el que está guardado en el Keychain.
    func logout(token: String? = nil) async throws {
        try await cliente.requestSinRespuesta(.post, "auth/logout", token: token)
    }
}
