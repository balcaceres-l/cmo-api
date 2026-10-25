//
//  Hospitalizacion.swift
//  appcmo
//

import Foundation

nonisolated struct HabitacionResumen: Decodable, Sendable, Equatable, Hashable {
    let codigo: String
    let planta: Int?
    let tipo: String?

    var tipoNombre: String { tipo == "VIP" ? "VIP" : "Semiprivada" }
}

nonisolated struct CamaResumen: Decodable, Sendable, Equatable, Hashable {
    let codigo: String
    let habitacion: HabitacionResumen?

    /// `HAB-203-B` → `HAB-203 · Cama B`; una VIP (`VIP-301`) se queda como está.
    var ubicacion: String {
        let partes = codigo.split(separator: "-")
        guard partes.count == 3, let letra = partes.last else { return codigo }
        return "\(partes[0])-\(partes[1]) · Cama \(letra)"
    }
}

/// Ingreso hospitalario (`GET /ingresos`). El médico solo recibe los suyos.
nonisolated struct Ingreso: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idIngreso: String
    let idPaciente: String
    let idMedico: String
    let motivo: String?
    let fechaIngreso: String
    let fechaAlta: String?
    let paciente: Paciente?
    let cama: CamaResumen?

    var id: String { idIngreso }

    enum CodingKeys: String, CodingKey {
        case idIngreso = "id_ingreso"
        case idPaciente = "id_paciente"
        case idMedico = "id_medico"
        case motivo
        case fechaIngreso = "fecha_ingreso"
        case fechaAlta = "fecha_alta"
        case paciente, cama
    }
}

// MARK: - Mapa de camas (`GET /habitaciones`)

nonisolated struct PacienteEnCama: Decodable, Sendable, Equatable, Hashable {
    let idPaciente: String
    let nombreCompleto: String
    let numeroExpediente: String

    enum CodingKeys: String, CodingKey {
        case idPaciente = "id_paciente"
        case nombreCompleto = "nombre_completo"
        case numeroExpediente = "numero_expediente"
    }
}

nonisolated struct CamaMapa: Decodable, Sendable, Equatable, Hashable {
    let codigo: String
    let estado: String
    /// Solo viene para el médico tratante; para cualquier otro es `nil` (privacidad).
    let paciente: PacienteEnCama?

    var ocupada: Bool { estado == "OCUPADA" }
}

nonisolated struct HabitacionMapa: Decodable, Sendable, Equatable, Hashable {
    let codigo: String
    let tipo: String
    let capacidad: Int
    let camas: [CamaMapa]
}

nonisolated struct PlantaMapa: Sendable, Equatable, Identifiable {
    let planta: Int
    let habitaciones: [HabitacionMapa]

    var id: Int { planta }
    var camasLibres: Int { habitaciones.flatMap(\.camas).filter { !$0.ocupada }.count }
    var camasTotales: Int { habitaciones.flatMap(\.camas).count }
}

/// La API entrega un objeto por planta (`{"2": [...], "3": [...]}`); la documentación habla de una lista de listas.
/// Se aceptan las dos formas.
nonisolated struct MapaHabitaciones: Decodable, Sendable, Equatable {
    let plantas: [PlantaMapa]

    init(from decoder: Decoder) throws {
        let contenedor = try decoder.singleValueContainer()
        if let porPlanta = try? contenedor.decode([String: [HabitacionMapa]].self) {
            plantas = porPlanta
                .compactMap { llave, habitaciones in Int(llave).map { PlantaMapa(planta: $0, habitaciones: habitaciones) } }
                .sorted { $0.planta < $1.planta }
        } else {
            let listas = try contenedor.decode([[HabitacionMapa]].self)
            plantas = listas.compactMap { habitaciones in
                // Sin la llave, la planta sale del código: HAB-2xx → 2, VIP-3xx → 3.
                let digito = habitaciones.first?.codigo.split(separator: "-").last?.first.flatMap { Int(String($0)) }
                return digito.map { PlantaMapa(planta: $0, habitaciones: habitaciones) }
            }
            .sorted { $0.planta < $1.planta }
        }
    }
}
