//
//  PublicoRepository.swift
//  appcmo
//

import Foundation

/// Información pública del hospital (`/publico/*`). Nunca lleva token ni devuelve datos de pacientes.
nonisolated struct PublicoRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    func ocupacion() async throws -> [OcupacionItem] {
        try await cliente.request(.get, "publico/ocupacion", autenticada: false)
    }

    func medicosDeTurno() async throws -> [MedicoDeTurno] {
        try await cliente.request(.get, "publico/medicos-de-turno", autenticada: false)
    }

    func avisos() async throws -> [AvisoPublico] {
        try await cliente.request(.get, "publico/avisos", autenticada: false)
    }

    func ambulancia() async throws -> AmbulanciaPublica {
        try await cliente.request(.get, "publico/ambulancia", autenticada: false)
    }
}
