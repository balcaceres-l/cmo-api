//
//  HospitalizacionRepository.swift
//  appcmo
//

import Foundation

/// Ingresos del médico y mapa de camas.
nonisolated struct HospitalizacionRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    /// Ingresos activos a cargo del médico (la API solo le devuelve los suyos).
    func ingresosActivos() async throws -> [Ingreso] {
        let pagina: Pagina<Ingreso> = try await cliente.request(.get, "ingresos", consulta: [
            URLQueryItem(name: "activos", value: "1"),
            URLQueryItem(name: "per_page", value: "100"),
        ])
        return pagina.data
    }

    /// Mapa de habitaciones y camas. El paciente de cada cama solo viene si es del médico.
    func mapa() async throws -> MapaHabitaciones {
        try await cliente.request(.get, "habitaciones")
    }
}
