//
//  Usuario.swift
//  appcmo
//

import Foundation

nonisolated struct Usuario: Decodable, Sendable, Equatable {
    let id: String
    let nombreCompleto: String
    let usuario: String
    let rol: Rol
    let cargo: String?
    let estado: String

    enum CodingKeys: String, CodingKey {
        case id
        case nombreCompleto = "nombre_completo"
        case usuario
        case rol
        case cargo
        case estado
    }
}

/// Datos de `POST /auth/login`.
nonisolated struct LoginResponse: Decodable, Sendable {
    let usuario: Usuario
    let idPaciente: String?
    let token: String

    enum CodingKeys: String, CodingKey {
        case usuario
        case idPaciente = "id_paciente"
        case token
    }
}

/// Datos de `GET /auth/me`.
nonisolated struct MeResponse: Decodable, Sendable {
    let usuario: Usuario
    let idPaciente: String?

    enum CodingKeys: String, CodingKey {
        case usuario
        case idPaciente = "id_paciente"
    }
}

/// Usuario autenticado y, si es paciente, su expediente.
nonisolated struct Sesion: Sendable, Equatable {
    let usuario: Usuario
    let idPaciente: String?
}
