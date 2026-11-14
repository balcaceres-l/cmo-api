//
//  SolicitudAtencionViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Pedir atención (HU-37): el paciente indica el motivo y opcionalmente un rango de fechas con su disponibilidad.
/// La cita queda "Pendiente" (SOLICITADA) hasta que recepción la confirme y le asigne médico, fecha y hora.
/// El paciente no reprograma ni cancela.
@Observable
final class SolicitudAtencionViewModel {
    static let limite = 500
    static let limiteFechas = 150

    /// Rango de fechas o turnos de disponibilidad (opcional)
    var rangoFechas = "" {
        didSet {
            if rangoFechas.count > Self.limiteFechas {
                rangoFechas = String(rangoFechas.prefix(Self.limiteFechas))
            }
        }
    }

    /// Motivo de consulta o síntomas (obligatorio)
    var motivo = "" {
        didSet {
            if motivo.count > Self.limite {
                motivo = String(motivo.prefix(Self.limite))
            }
        }
    }

    private(set) var enviando = false
    var aviso: AvisoMensaje?

    @ObservationIgnored private let repositorio: CitasRepository

    init(repositorio: CitasRepository = CitasRepository()) {
        self.repositorio = repositorio
    }

    /// El campo de fecha es opcional; solo se exige motivo válido para poder enviar.
    var puedeEnviar: Bool {
        !motivo.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !enviando
    }

    /// Texto unificado que concatena la disponibilidad si fue provista junto con el motivo de la consulta.
    var textoConcatenado: String {
        let fechas = rangoFechas.trimmingCharacters(in: .whitespacesAndNewlines)
        let textoMotivo = motivo.trimmingCharacters(in: .whitespacesAndNewlines)
        let resultado: String
        if fechas.isEmpty {
            resultado = textoMotivo
        } else {
            resultado = "[Disponibilidad: \(fechas)] \(textoMotivo)"
        }
        if resultado.count > Self.limite {
            return String(resultado.prefix(Self.limite))
        }
        return resultado
    }

    /// Crea una cita SOLICITADA sin médico ni hora; recepción la confirma.
    func enviar() async -> Bool {
        enviando = true
        aviso = nil
        defer { enviando = false }
        do {
            _ = try await repositorio.solicitarAtencion(motivo: textoConcatenado)
            motivo = ""
            rangoFechas = ""
            aviso = .exito("Solicitud enviada. Recepción te asignará médico, fecha y hora.")
            return true
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
            return false
        }
    }
}
