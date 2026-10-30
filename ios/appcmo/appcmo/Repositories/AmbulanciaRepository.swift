//
//  AmbulanciaRepository.swift
//  appcmo
//

import Foundation

nonisolated struct AmbulanciaRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    func estado() async throws -> AmbulanciaMedico {
        try await cliente.request(.get, "ambulancia")
    }
}
