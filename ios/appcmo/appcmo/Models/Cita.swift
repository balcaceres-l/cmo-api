//
//  Cita.swift
//  appcmo
//

import Foundation

nonisolated enum EstadoCita: String, Decodable, Sendable {
    case solicitada = "SOLICITADA"
    case agendada = "AGENDADA"
    case enEspera = "EN_ESPERA"
    case enAtencion = "EN_ATENCION"
    case atendida = "ATENDIDA"
    case cancelada = "CANCELADA"
    case noAsistio = "NO_ASISTIO"
    case desconocido

    init(from decoder: Decoder) throws {
        let texto = try decoder.singleValueContainer().decode(String.self)
        self = EstadoCita(rawValue: texto) ?? .desconocido
    }

    var nombre: String {
        switch self {
        case .solicitada: "Pendiente de confirmación"
        case .agendada: "Agendada"
        case .enEspera: "En espera"
        case .enAtencion: "En atención"
        case .atendida: "Atendida"
        case .cancelada: "Cancelada"
        case .noAsistio: "No asistió"
        case .desconocido: "Sin estado"
        }
    }

    /// El médico puede abrir la consulta solo en estas citas.
    var sePuedeAtender: Bool { self == .agendada || self == .enEspera }

    /// Canceladas y no asistidas se distinguen visualmente.
    var estaInactiva: Bool { self == .cancelada || self == .noAsistio }

    var tieneConsulta: Bool { self == .enAtencion || self == .atendida }

    /// Texto del estado para el médico (así lo nombra el diseño).
    var nombreMedico: String {
        switch self {
        case .agendada: "Por llegar"
        case .enEspera: "En sala"
        case .enAtencion: "En consulta"
        case .atendida: "Atendida"
        case .solicitada: "Solicitada"
        case .cancelada: "Cancelada"
        case .noAsistio: "No asistió"
        case .desconocido: "Sin estado"
        }
    }

    var tonoMedico: TonoEstado {
        switch self {
        case .agendada, .solicitada: .advertencia
        case .enEspera, .enAtencion: .info
        case .atendida: .exito
        case .cancelada, .noAsistio: .error
        case .desconocido: .neutro
        }
    }
}

/// Del médico solo se usa lo que el paciente puede ver: nombre y cargo.
nonisolated struct MedicoResumen: Decodable, Sendable, Equatable, Hashable {
    let id: String
    let nombreCompleto: String
    let cargo: String?

    enum CodingKeys: String, CodingKey {
        case id
        case nombreCompleto = "nombre_completo"
        case cargo
    }
}

nonisolated struct Consultorio: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idConsultorio: String
    let codigo: String
    let planta: Int?
    let nombre: String

    var id: String { idConsultorio }

    enum CodingKeys: String, CodingKey {
        case idConsultorio = "id_consultorio"
        case codigo, planta, nombre
    }
}

nonisolated struct Cita: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idCita: String
    let idPaciente: String
    let idMedico: String?
    let fecha: String?
    let horaInicio: String?
    let horaFin: String?
    let motivo: String
    let estado: EstadoCita
    let motivoCancelacion: String?
    let paciente: Paciente?
    let medico: MedicoResumen?
    let consultorio: Consultorio?

    var id: String { idCita }

    enum CodingKeys: String, CodingKey {
        case idCita = "id_cita"
        case idPaciente = "id_paciente"
        case idMedico = "id_medico"
        case fecha
        case horaInicio = "hora_inicio"
        case horaFin = "hora_fin"
        case motivo, estado
        case motivoCancelacion = "motivo_cancelacion"
        case paciente, medico, consultorio
    }
}

/// Bloque libre de la agenda de un médico: `GET /agenda/disponibilidad`.
nonisolated struct BloqueDisponible: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let horaInicio: String
    let horaFin: String

    var id: String { horaInicio }

    enum CodingKeys: String, CodingKey {
        case horaInicio = "hora_inicio"
        case horaFin = "hora_fin"
    }
}

/// Cuerpo de `POST /citas` para un seguimiento (el médico agenda para sí mismo).
nonisolated struct SolicitudCita: Encodable, Sendable {
    let idPaciente: String
    let fecha: String
    let horaInicio: String
    let motivo: String
    let idConsultorio: String?

    enum CodingKeys: String, CodingKey {
        case idPaciente = "id_paciente"
        case fecha
        case horaInicio = "hora_inicio"
        case motivo
        case idConsultorio = "id_consultorio"
    }
}
