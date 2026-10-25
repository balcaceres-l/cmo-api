//
//  ConsultasRepository.swift
//  appcmo
//

import Foundation

/// Consulta general y recetas del médico (`/consultas`, `/recetas`).
nonisolated struct ConsultasRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    func ver(id: String) async throws -> Consulta {
        try await cliente.request(.get, "consultas/\(id)")
    }

    func guardar(id: String, cambios: CambiosConsulta) async throws -> Consulta {
        try await cliente.request(.put, "consultas/\(id)", cuerpo: cambios)
    }

    func agregarReceta(idConsulta: String, receta: SolicitudReceta) async throws -> Receta {
        try await cliente.request(.post, "consultas/\(idConsulta)/recetas", cuerpo: receta)
    }

    func eliminarReceta(id: String) async throws {
        try await cliente.requestSinRespuesta(.delete, "recetas/\(id)")
    }

    func finalizar(id: String) async throws -> Consulta {
        try await cliente.request(.patch, "consultas/\(id)/finalizar")
    }

    /// La lista de citas no trae el id de la consulta; el expediente sí (`citas[].consulta`).
    func idDeConsulta(cita: Cita) async throws -> String? {
        let expediente: ExpedienteResumen = try await cliente.request(.get, "pacientes/\(cita.idPaciente)/expediente")
        return expediente.citas.first { $0.idCita == cita.idCita }?.consulta?.idConsulta
    }
}
