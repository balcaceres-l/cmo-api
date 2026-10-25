//
//  Usuario.swift
//  appcmo
//

import Foundation

nonisolated struct Usuario: Decodable, Sendable, Equatable {
    let id: String
    let nombreCompleto: String
    let usuario: String
    let email: String?
    let rol: Rol
    let cargo: String?
    let estado: String

    enum CodingKeys: String, CodingKey {
        case id
        case nombreCompleto = "nombre_completo"
        case usuario
        case email
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

/// Cuerpo de `POST /auth/registro` (paciente).
nonisolated struct SolicitudRegistro: Encodable, Sendable {
    let nombreCompleto: String
    let usuario: String
    let email: String?
    let password: String
    let passwordConfirmation: String
    let dui: String
    let fechaNacimiento: String
    let sexo: String
    let telefono: String?
    let direccion: String?
    let codigoActivacion: String?

    enum CodingKeys: String, CodingKey {
        case nombreCompleto = "nombre_completo"
        case usuario, email, password
        case passwordConfirmation = "password_confirmation"
        case dui
        case fechaNacimiento = "fecha_nacimiento"
        case sexo, telefono, direccion
        case codigoActivacion = "codigo_activacion"
    }
}

/// Cuerpo de `PATCH /auth/password`.
nonisolated struct SolicitudCambioPassword: Encodable, Sendable {
    let passwordActual: String
    let password: String
    let passwordConfirmation: String

    enum CodingKeys: String, CodingKey {
        case passwordActual = "password_actual"
        case password
        case passwordConfirmation = "password_confirmation"
    }
}
