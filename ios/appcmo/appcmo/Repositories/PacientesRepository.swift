//
//  PacientesRepository.swift
//  appcmo
//

import Foundation

nonisolated struct PacientesRepository: Sendable {
    let cliente: NetworkClient

    init(cliente: NetworkClient = NetworkClient()) {
        self.cliente = cliente
    }

    /// Para un médico la API solo devuelve a quienes trata (cita activa o ingreso a su cargo).
    func buscar(_ texto: String) async throws -> [Paciente] {
        var consulta = [URLQueryItem(name: "per_page", value: "100")]
        let limpio = texto.trimmingCharacters(in: .whitespacesAndNewlines)
        if !limpio.isEmpty { consulta.append(URLQueryItem(name: "q", value: limpio)) }
        let pagina: Pagina<Paciente> = try await cliente.request(.get, "pacientes", consulta: consulta)
        return pagina.data
    }

    /// Expediente completo: solo para el médico tratante (403 para cualquier otro).
    func expediente(id: String) async throws -> Expediente {
        try await cliente.request(.get, "pacientes/\(id)/expediente")
    }

    func guardarHistorial(id: String, cambios: CambiosHistorial) async throws -> HistorialClinico {
        try await cliente.request(.put, "pacientes/\(id)/historial", cuerpo: cambios)
    }

    /// El expediente del propio paciente (solo de lectura).
    func miExpediente() async throws -> Expediente {
        try await cliente.request(.get, "mi/expediente")
    }

    private struct SolicitudOtpCuerpo: Encodable, Sendable {
        let dui: String
    }

    private struct ConfirmarOtpCuerpo: Encodable, Sendable {
        let dui: String
        let otp: String
    }

    /// Solicita un código OTP al correo del usuario para vincular un expediente existente.
    func solicitarOtpVinculacion(dui: String) async throws -> SolicitudOtpRespuesta {
        try await cliente.request(.post, "mi/expediente/solicitar-otp", cuerpo: SolicitudOtpCuerpo(dui: dui))
    }

    /// Valida el OTP y vincula el expediente con la cuenta actual.
    func confirmarOtpVinculacion(dui: String, otp: String) async throws -> ConfirmarOtpRespuesta {
        try await cliente.request(.post, "mi/expediente/confirmar-otp", cuerpo: ConfirmarOtpCuerpo(dui: dui, otp: otp))
    }
}
