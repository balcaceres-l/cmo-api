//
//  Rol.swift
//  appcmo
//

import Foundation

/// Roles de la API (backend/docs/API.md §2). Cada rol tiene su plataforma:
/// ADMINISTRADOR → web · MEDICO → iOS y Android · RECEPCIONISTA → Android · PACIENTE → iOS.
nonisolated enum Rol: String, Decodable, Sendable {
    case administrador = "ADMINISTRADOR"
    case medico = "MEDICO"
    case recepcionista = "RECEPCIONISTA"
    case paciente = "PACIENTE"

    /// Solo médicos y pacientes usan la app iOS.
    var puedeUsarApp: Bool {
        self == .medico || self == .paciente
    }

    var nombre: String {
        switch self {
        case .administrador: "Administrador"
        case .medico: "Médico"
        case .recepcionista: "Recepción"
        case .paciente: "Paciente"
        }
    }
}
