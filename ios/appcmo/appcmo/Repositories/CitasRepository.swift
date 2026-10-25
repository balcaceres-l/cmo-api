//
//  CitasRepository.swift
//  appcmo
//

import Foundation

/// Citas del médico (`/citas`, `/agenda/disponibilidad`) y del paciente (`/mi/citas`, `/solicitudes-atencion`).
nonisolated struct CitasRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    private static let cien = [URLQueryItem(name: "per_page", value: "100")]

    /// Citas del médico. La API todavía no filtra por fecha: se filtra en la app.
    func delMedico() async throws -> [Cita] {
        let pagina: Pagina<Cita> = try await cliente.request(.get, "citas", consulta: Self.cien)
        return pagina.data
    }

    func disponibilidad(idMedico: String, fecha: String) async throws -> [BloqueDisponible] {
        try await cliente.request(.get, "agenda/disponibilidad", consulta: [
            URLQueryItem(name: "id_medico", value: idMedico),
            URLQueryItem(name: "fecha", value: fecha),
        ])
    }

    func consultorios() async throws -> [Consultorio] {
        try await cliente.request(.get, "consultorios")
    }

    func agendar(_ solicitud: SolicitudCita) async throws -> Cita {
        try await cliente.request(.post, "citas", cuerpo: solicitud)
    }

    func abrirConsulta(idCita: String) async throws -> Consulta {
        try await cliente.request(.post, "citas/\(idCita)/consulta")
    }

    func delPaciente() async throws -> [Cita] {
        let pagina: Pagina<Cita> = try await cliente.request(.get, "mi/citas", consulta: Self.cien)
        return pagina.data
    }

    private struct Solicitud: Encodable, Sendable {
        let motivo: String
    }

    func solicitarAtencion(motivo: String) async throws -> Cita {
        try await cliente.request(.post, "solicitudes-atencion", cuerpo: Solicitud(motivo: motivo))
    }
}
