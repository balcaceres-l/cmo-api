//
//  Turno.swift
//  appcmo
//

import Foundation

/// Turno del personal: `GET /mi/turnos`.
nonisolated struct TurnoPersonal: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idTurno: String
    let fecha: String
    let horaInicio: String
    let horaFin: String
    let area: String

    var id: String { idTurno }

    enum CodingKeys: String, CodingKey {
        case idTurno = "id_turno"
        case fecha
        case horaInicio = "hora_inicio"
        case horaFin = "hora_fin"
        case area
    }
}
