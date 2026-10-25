//
//  Publico.swift
//  appcmo
//

import Foundation

// Datos de `GET /publico/*`: se piden sin token y nunca traen datos de pacientes.

nonisolated struct OcupacionItem: Decodable, Sendable, Equatable {
    let planta: Int
    let tipo: String
    let estado: String
    let total: Int
}

nonisolated struct MedicoDeTurno: Decodable, Sendable, Equatable, Identifiable {
    let nombre: String
    let cargo: String?
    let area: String

    var id: String { "\(nombre)|\(area)" }
}

nonisolated struct AvisoPublico: Decodable, Sendable, Equatable, Identifiable {
    let titulo: String
    let contenido: String
    let vigenteDesde: String?
    let vigenteHasta: String?

    var id: String { "\(titulo)|\(vigenteDesde ?? "")" }

    enum CodingKeys: String, CodingKey {
        case titulo, contenido
        case vigenteDesde = "vigente_desde"
        case vigenteHasta = "vigente_hasta"
    }
}

nonisolated struct AmbulanciaPublica: Decodable, Sendable, Equatable {
    let estado: String

    var nombre: String {
        switch estado {
        case "DISPONIBLE": "Disponible"
        case "EN_SERVICIO": "En servicio"
        case "EN_MANTENIMIENTO": "En mantenimiento"
        default: estado
        }
    }
}

/// Resumen de camas por tipo de habitación.
nonisolated struct OcupacionResumen: Equatable, Identifiable {
    let planta: Int
    let tipo: String
    let disponibles: Int
    let total: Int

    var id: String { "\(planta)-\(tipo)" }

    var tipoNombre: String { tipo == "VIP" ? "Habitaciones VIP" : "Habitaciones semiprivadas" }

    /// Agrupa las filas (planta, tipo, estado, total) de la API por planta y tipo.
    static func resumir(_ items: [OcupacionItem]) -> [OcupacionResumen] {
        let grupos = Dictionary(grouping: items) { "\($0.planta)|\($0.tipo)" }
        return grupos.values.compactMap { filas in
            guard let primera = filas.first else { return nil }
            return OcupacionResumen(
                planta: primera.planta,
                tipo: primera.tipo,
                disponibles: filas.filter { $0.estado == "DISPONIBLE" }.reduce(0) { $0 + $1.total },
                total: filas.reduce(0) { $0 + $1.total }
            )
        }
        .sorted { ($0.planta, $0.tipo) < ($1.planta, $1.tipo) }
    }
}
