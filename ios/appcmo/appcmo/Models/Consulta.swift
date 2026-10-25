//
//  Consulta.swift
//  appcmo
//

import Foundation

nonisolated struct Receta: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idReceta: String
    let medicamento: String
    let dosis: String
    let frecuencia: String
    let duracion: String
    let indicaciones: String?

    var id: String { idReceta }

    enum CodingKeys: String, CodingKey {
        case idReceta = "id_receta"
        case medicamento, dosis, frecuencia, duracion, indicaciones
    }
}

nonisolated struct Consulta: Decodable, Sendable, Equatable, Identifiable {
    let idConsulta: String
    let idCita: String
    let idMedico: String
    let fechaHoraInicio: String
    let fechaHoraFin: String?
    let motivo: String?
    let diagnostico: String?
    let notas: String?
    /// Solo vienen en `GET /consultas/{id}`.
    let recetas: [Receta]?
    let ordenesLaboratorio: [OrdenLaboratorio]?
    let cita: Cita?

    var id: String { idConsulta }

    /// Una consulta cerrada es de solo lectura.
    var estaCerrada: Bool { fechaHoraFin != nil }

    enum CodingKeys: String, CodingKey {
        case idConsulta = "id_consulta"
        case idCita = "id_cita"
        case idMedico = "id_medico"
        case fechaHoraInicio = "fecha_hora_inicio"
        case fechaHoraFin = "fecha_hora_fin"
        case motivo, diagnostico, notas, recetas
        case ordenesLaboratorio = "ordenes_laboratorio"
        case cita
    }
}

/// Cuerpo de `PUT /consultas/{id}`.
nonisolated struct CambiosConsulta: Encodable, Sendable {
    let diagnostico: String
    let motivo: String
    let notas: String
}

/// Cuerpo de `POST /consultas/{id}/recetas`.
nonisolated struct SolicitudReceta: Encodable, Sendable {
    let medicamento: String
    let dosis: String
    let frecuencia: String
    let duracion: String
    let indicaciones: String?
}

/// Solo lo necesario del expediente para encontrar la consulta de una cita:
/// `GET /pacientes/{id}/expediente` trae `citas[].consulta`.
nonisolated struct ExpedienteResumen: Decodable, Sendable {
    let citas: [CitaConConsulta]

    struct CitaConConsulta: Decodable, Sendable {
        let idCita: String
        let consulta: ConsultaId?

        enum CodingKeys: String, CodingKey {
            case idCita = "id_cita"
            case consulta
        }
    }

    struct ConsultaId: Decodable, Sendable {
        let idConsulta: String

        enum CodingKeys: String, CodingKey {
            case idConsulta = "id_consulta"
        }
    }
}
