//
//  SolicitudAtencionViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Pedir atención (HU-37): el paciente indica el motivo y la cita queda "Pendiente" hasta que recepción la confirme.
/// El paciente no reprograma ni cancela.
@Observable
final class SolicitudAtencionViewModel {
    static let limite = 500

    var motivo = "" {
        didSet { if motivo.count > Self.limite { motivo = String(motivo.prefix(Self.limite)) } }
    }
    private(set) var enviando = false
    var aviso: AvisoMensaje?

    @ObservationIgnored private let repositorio: CitasRepository

    init(repositorio: CitasRepository = CitasRepository()) {
        self.repositorio = repositorio
    }

    var puedeEnviar: Bool {
        !motivo.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !enviando
    }

    /// Crea una cita SOLICITADA sin médico ni hora; recepción la confirma.
    func enviar() async -> Bool {
        enviando = true
        aviso = nil
        defer { enviando = false }
        do {
            _ = try await repositorio.solicitarAtencion(motivo: motivo.trimmingCharacters(in: .whitespacesAndNewlines))
            motivo = ""
            aviso = .exito("Solicitud enviada. Recepción te asignará médico, fecha y hora.")
            return true
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
            return false
        }
    }
}
