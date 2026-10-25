//
//  AvisosRepository.swift
//  appcmo
//

import Foundation

nonisolated struct AvisosRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    func todos() async throws -> [Aviso] {
        let pagina: Pagina<Aviso> = try await cliente.request(.get, "avisos", consulta: [URLQueryItem(name: "per_page", value: "100")])
        return pagina.data
    }
}
