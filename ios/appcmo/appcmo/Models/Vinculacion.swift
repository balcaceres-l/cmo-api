//
//  Vinculacion.swift
//  appcmo
//

import Foundation

/// Respuesta tras solicitar un código OTP para vincular un expediente por correo.
nonisolated struct SolicitudOtpRespuesta: Decodable, Sendable {
    let emailEnmascarado: String
    let venceEnMinutos: Int?

    enum CodingKeys: String, CodingKey {
        case emailEnmascarado = "email_enmascarado"
        case venceEnMinutos = "vence_en_minutos"
    }
}

/// Respuesta al confirmar el código OTP de vinculación.
nonisolated struct ConfirmarOtpRespuesta: Decodable, Sendable {
    let idPaciente: String
    let numeroExpediente: String
    let nombreCompleto: String?

    enum CodingKeys: String, CodingKey {
        case idPaciente = "id_paciente"
        case numeroExpediente = "numero_expediente"
        case nombreCompleto = "nombre_completo"
    }
}
