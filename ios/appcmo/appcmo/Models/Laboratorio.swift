//
//  Laboratorio.swift
//  appcmo
//

import Foundation

nonisolated struct Examen: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idExamen: String
    let nombre: String
    let descripcion: String?

    var id: String { idExamen }

    enum CodingKeys: String, CodingKey {
        case idExamen = "id_examen"
        case nombre, descripcion
    }
}

/// El laboratorio solo informa estados: PENDIENTE → EN_PROCESO → COMPLETADO. El médico puede cancelar una orden
/// que sigue PENDIENTE.
nonisolated enum EstadoOrden: String, Decodable, Sendable, CaseIterable {
    case pendiente = "PENDIENTE"
    case enProceso = "EN_PROCESO"
    case completado = "COMPLETADO"
    case cancelada = "CANCELADA"
    case desconocido

    /// Los estados del avance del laboratorio (los que cuenta y filtra la pantalla de Laboratorio).
    static var allCases: [EstadoOrden] { [.pendiente, .enProceso, .completado] }

    init(from decoder: Decoder) throws {
        let texto = try decoder.singleValueContainer().decode(String.self)
        self = EstadoOrden(rawValue: texto) ?? .desconocido
    }

    var nombre: String {
        switch self {
        case .pendiente: "Pendiente"
        case .enProceso: "En proceso"
        case .completado: "Completado"
        case .cancelada: "Cancelada"
        case .desconocido: "Sin estado"
        }
    }
}

nonisolated struct OrdenLaboratorio: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idOrden: String
    let idConsulta: String
    let idPaciente: String
    let idExamen: String
    let estado: EstadoOrden
    let observaciones: String?
    let fechaSolicitud: String?
    let fechaInicio: String?
    let fechaResultado: String?
    let motivoCancelacion: String?
    let fechaCancelacion: String?
    let paciente: Paciente?
    let examen: Examen?

    var id: String { idOrden }

    /// La misma orden con su nuevo estado de cancelación (el resto no cambia).
    init(copiando o: OrdenLaboratorio, estado: EstadoOrden, motivo: String?, fecha: String?) {
        idOrden = o.idOrden; idConsulta = o.idConsulta; idPaciente = o.idPaciente; idExamen = o.idExamen
        self.estado = estado; observaciones = o.observaciones; fechaSolicitud = o.fechaSolicitud
        fechaInicio = o.fechaInicio; fechaResultado = o.fechaResultado
        motivoCancelacion = motivo; fechaCancelacion = fecha
        paciente = o.paciente; examen = o.examen
    }

    enum CodingKeys: String, CodingKey {
        case idOrden = "id_orden"
        case idConsulta = "id_consulta"
        case idPaciente = "id_paciente"
        case idExamen = "id_examen"
        case estado, observaciones
        case fechaSolicitud = "fecha_solicitud"
        case fechaInicio = "fecha_inicio"
        case fechaResultado = "fecha_resultado"
        case motivoCancelacion = "motivo_cancelacion"
        case fechaCancelacion = "fecha_cancelacion"
        case paciente, examen
    }
}

/// Cuerpo de `POST /consultas/{id}/ordenes-laboratorio`.
nonisolated struct SolicitudOrden: Encodable, Sendable {
    let idExamen: String
    let observaciones: String?

    enum CodingKeys: String, CodingKey {
        case idExamen = "id_examen"
        case observaciones
    }
}

/// Cuerpo de `PATCH /ordenes-laboratorio/{id}/cancelar`.
nonisolated struct CancelacionOrden: Encodable, Sendable {
    let motivoCancelacion: String?

    enum CodingKeys: String, CodingKey {
        case motivoCancelacion = "motivo_cancelacion"
    }
}
