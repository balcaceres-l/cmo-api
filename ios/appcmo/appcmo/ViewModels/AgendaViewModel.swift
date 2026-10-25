//
//  AgendaViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Agenda del médico: sus citas de un día (HU-36).
@Observable
final class AgendaViewModel {
    var dia = Date()
    private(set) var carga: Carga<[Cita]> = .inicial

    @ObservationIgnored private let repositorio: CitasRepository

    init(repositorio: CitasRepository = CitasRepository()) {
        self.repositorio = repositorio
    }

    var esHoy: Bool { Formato.fechaAPI(dia) == Formato.hoyAPI }

    /// Solo las citas del día elegido, por hora.
    var citasDelDia: [Cita] {
        guard case .listo(let citas) = carga else { return [] }
        let fecha = Formato.fechaAPI(dia)
        return citas
            .filter { $0.fecha == fecha }
            .sorted { ($0.horaInicio ?? "") < ($1.horaInicio ?? "") }
    }

    /// Los siete días (lunes a domingo) de la semana del día elegido.
    var diasDeLaSemana: [Date] {
        let calendario = Formato.calendario
        guard let inicio = calendario.dateInterval(of: .weekOfYear, for: dia)?.start else { return [dia] }
        return (0..<7).compactMap { calendario.date(byAdding: .day, value: $0, to: inicio) }
    }

    var mes: String { Formato.mesYAnio(dia) }

    func moverSemana(_ semanas: Int) {
        dia = Formato.calendario.date(byAdding: .weekOfYear, value: semanas, to: dia) ?? dia
    }

    func moverDia(_ dias: Int) {
        dia = Formato.calendario.date(byAdding: .day, value: dias, to: dia) ?? dia
    }

    func irAHoy() {
        dia = Date()
    }

    func cargar() async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.delMedico())
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }
}
