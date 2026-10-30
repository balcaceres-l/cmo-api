//
//  Ambulancia.swift
//  appcmo
//

import Foundation

nonisolated enum EstadoAmbulancia: String, Decodable, Sendable {
    case disponible = "DISPONIBLE"
    case enServicio = "EN_SERVICIO"
    case enMantenimiento = "EN_MANTENIMIENTO"
    case desconocido

    init(from decoder: Decoder) throws {
        let texto = try decoder.singleValueContainer().decode(String.self)
        self = EstadoAmbulancia(rawValue: texto) ?? .desconocido
    }

    var nombre: String {
        switch self {
        case .disponible: "Disponible"
        case .enServicio: "En servicio"
        case .enMantenimiento: "En mantenimiento"
        case .desconocido: "Sin estado"
        }
    }

    var tono: TonoEstado {
        switch self {
        case .disponible: .exito
        case .enServicio: .error
        case .enMantenimiento: .advertencia
        case .desconocido: .neutro
        }
    }
}

nonisolated enum PrioridadServicio: String, Decodable, Sendable {
    case baja = "BAJA", media = "MEDIA", alta = "ALTA", critica = "CRITICA", desconocida

    init(from decoder: Decoder) throws {
        let texto = try decoder.singleValueContainer().decode(String.self)
        self = PrioridadServicio(rawValue: texto) ?? .desconocida
    }

    var nombre: String {
        switch self {
        case .baja: "Baja"
        case .media: "Media"
        case .alta: "Alta"
        case .critica: "Crítica"
        case .desconocida: "—"
        }
    }

    var tono: TonoEstado {
        switch self {
        case .baja: .neutro
        case .media: .info
        case .alta: .advertencia
        case .critica: .error
        case .desconocida: .neutro
        }
    }
}

nonisolated enum EstadoPacienteTraslado: String, Decodable, Sendable {
    case estable = "ESTABLE", delicado = "DELICADO", critico = "CRITICO", desconocido

    init(from decoder: Decoder) throws {
        let texto = try decoder.singleValueContainer().decode(String.self)
        self = EstadoPacienteTraslado(rawValue: texto) ?? .desconocido
    }

    var nombre: String {
        switch self {
        case .estable: "Estable"
        case .delicado: "Delicado"
        case .critico: "Crítico"
        case .desconocido: "—"
        }
    }

    var tono: TonoEstado {
        switch self {
        case .estable: .exito
        case .delicado: .advertencia
        case .critico: .error
        case .desconocido: .neutro
        }
    }
}

/// Último reporte clínico del traslado. Sin nombre ni dirección de la persona atendida.
nonisolated struct ReporteTraslado: Decodable, Sendable, Equatable {
    let estadoPaciente: EstadoPacienteTraslado
    let observaciones: String?
    let llegadaEstimada: String?
    let fecha: String?

    enum CodingKeys: String, CodingKey {
        case estadoPaciente = "estado_paciente"
        case observaciones
        case llegadaEstimada = "llegada_estimada"
        case fecha
    }
}

nonisolated struct ServicioEnCurso: Decodable, Sendable, Equatable {
    let idServicio: String
    let prioridad: PrioridadServicio
    let horaSalida: String?
    let ultimoReporte: ReporteTraslado?

    enum CodingKeys: String, CodingKey {
        case idServicio = "id_servicio"
        case prioridad
        case horaSalida = "hora_salida"
        case ultimoReporte = "ultimo_reporte"
    }
}

/// `GET /ambulancia` para el médico: estado y servicio en curso.
nonisolated struct AmbulanciaMedico: Decodable, Sendable, Equatable {
    let estado: EstadoAmbulancia
    let servicioActivo: ServicioEnCurso?

    enum CodingKeys: String, CodingKey {
        case estado
        case servicioActivo = "servicio_activo"
    }
}
