//
//  LaboratorioRepository.swift
//  appcmo
//

import Foundation

nonisolated struct LaboratorioRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    func examenes() async throws -> [Examen] {
        let pagina: Pagina<Examen> = try await cliente.request(.get, "examenes", consulta: [URLQueryItem(name: "per_page", value: "100")])
        return pagina.data
    }

    func solicitar(idConsulta: String, orden: SolicitudOrden) async throws -> OrdenLaboratorio {
        try await cliente.request(.post, "consultas/\(idConsulta)/ordenes-laboratorio", cuerpo: orden)
    }

    /// Órdenes que solicitó el médico, con filtro opcional por estado.
    func ordenes(estado: EstadoOrden?) async throws -> [OrdenLaboratorio] {
        var consulta = [URLQueryItem(name: "per_page", value: "100")]
        if let estado { consulta.append(URLQueryItem(name: "estado", value: estado.rawValue)) }
        let pagina: Pagina<OrdenLaboratorio> = try await cliente.request(.get, "ordenes-laboratorio", consulta: consulta)
        return pagina.data
    }

    /// Cancela una orden que sigue PENDIENTE (solo el médico que la pidió).
    func cancelar(idOrden: String, motivo: String? = nil) async throws -> OrdenLaboratorio {
        try await cliente.request(.patch, "ordenes-laboratorio/\(idOrden)/cancelar", cuerpo: CancelacionOrden(motivoCancelacion: motivo))
    }

    /// Órdenes del propio paciente, con su estado.
    func mias() async throws -> [OrdenLaboratorio] {
        let pagina: Pagina<OrdenLaboratorio> = try await cliente.request(.get, "mi/laboratorio", consulta: [URLQueryItem(name: "per_page", value: "100")])
        return pagina.data
    }
}
