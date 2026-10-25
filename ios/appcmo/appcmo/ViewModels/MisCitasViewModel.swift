//
//  MisCitasViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Mis citas y solicitud de atención del paciente (HU-37). El paciente no reprograma ni cancela.
@Observable
final class MisCitasViewModel {
    private(set) var carga: Carga<[Cita]> = .inicial
    var motivo = ""
    private(set) var enviando = false
    var aviso: AvisoMensaje?
    var avisoSolicitud: AvisoMensaje?

    @ObservationIgnored private let repositorio: CitasRepository

    init(repositorio: CitasRepository = CitasRepository()) {
        self.repositorio = repositorio
    }

    private var citas: [Cita] { carga.valor ?? [] }

    /// Solicitudes que recepción todavía no confirma.
    var pendientes: [Cita] { citas.filter { $0.estado == .solicitada } }

    var proximas: [Cita] {
        citas
            .filter { [.agendada, .enEspera, .enAtencion].contains($0.estado) }
            .sorted { ($0.fecha ?? "", $0.horaInicio ?? "") < ($1.fecha ?? "", $1.horaInicio ?? "") }
    }

    var anteriores: [Cita] {
        citas.filter { [.atendida, .cancelada, .noAsistio].contains($0.estado) }
    }

    var puedeSolicitar: Bool {
        !motivo.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !enviando
    }

    func cargar() async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.delPaciente())
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }

    /// Crea una cita SOLICITADA sin médico ni hora; recepción la confirma.
    func solicitar() async -> Bool {
        enviando = true
        avisoSolicitud = nil
        defer { enviando = false }
        do {
            _ = try await repositorio.solicitarAtencion(motivo: motivo.trimmingCharacters(in: .whitespacesAndNewlines))
            motivo = ""
            aviso = .exito("Solicitud enviada. Aparece como pendiente de confirmación hasta que recepción te asigne fecha y hora.")
            await cargar()
            return true
        } catch {
            avisoSolicitud = mensajeDeError(error).map(AvisoMensaje.error)
            return false
        }
    }
}
