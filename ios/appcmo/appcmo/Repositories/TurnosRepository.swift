//
//  TurnosRepository.swift
//  appcmo
//

import Foundation

nonisolated struct TurnosRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    func mios() async throws -> [TurnoPersonal] {
        let pagina: Pagina<TurnoPersonal> = try await cliente.request(.get, "mi/turnos", consulta: [URLQueryItem(name: "per_page", value: "100")])
        return pagina.data
    }
}
