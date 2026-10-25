//
//  Paciente.swift
//  appcmo
//

import Foundation

nonisolated struct Paciente: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idPaciente: String
    let numeroExpediente: String
    let nombreCompleto: String
    let fechaNacimiento: String
    let dui: String?
    let sexo: String
    let telefono: String?
    let direccion: String?
    /// Solo llegan para quien tiene acceso clínico (el médico tratante).
    let tipoSangre: String?
    let alergias: String?

    var id: String { idPaciente }

    var sexoTexto: String { sexo == "F" ? "Femenino" : "Masculino" }

    enum CodingKeys: String, CodingKey {
        case idPaciente = "id_paciente"
        case numeroExpediente = "numero_expediente"
        case nombreCompleto = "nombre_completo"
        case fechaNacimiento = "fecha_nacimiento"
        case dui, sexo, telefono, direccion
        case tipoSangre = "tipo_sangre"
        case alergias
    }
}
