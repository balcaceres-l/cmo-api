//
//  TurnosViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Turnos de la semana del médico, con el turno actual resaltado (HU-66).
@Observable
final class TurnosViewModel {
    private(set) var inicioSemana: Date
    private(set) var carga: Carga<[TurnoPersonal]> = .inicial

    @ObservationIgnored private let repositorio: TurnosRepository

    init(repositorio: TurnosRepository = TurnosRepository()) {
        self.repositorio = repositorio
        self.inicioSemana = Self.inicioDeSemana(Date())
    }

    private static func inicioDeSemana(_ fecha: Date) -> Date {
        let calendario = Formato.calendario
        return calendario.dateInterval(of: .weekOfYear, for: fecha)?.start ?? calendario.startOfDay(for: fecha)
    }

    var finSemana: Date {
        Formato.calendario.date(byAdding: .day, value: 6, to: inicioSemana) ?? inicioSemana
    }

    var esSemanaActual: Bool { inicioSemana == Self.inicioDeSemana(Date()) }

    var titulo: String {
        "\(Formato.fechaCorta(Formato.fechaAPI(inicioSemana))) – \(Formato.fechaCorta(Formato.fechaAPI(finSemana)))"
    }

    func moverSemana(_ semanas: Int) {
        inicioSemana = Formato.calendario.date(byAdding: .weekOfYear, value: semanas, to: inicioSemana) ?? inicioSemana
    }

    func irASemanaActual() {
        inicioSemana = Self.inicioDeSemana(Date())
    }

    var turnosDeLaSemana: [TurnoPersonal] {
        guard case .listo(let turnos) = carga else { return [] }
        let desde = Formato.fechaAPI(inicioSemana), hasta = Formato.fechaAPI(finSemana)
        return turnos
            .filter { $0.fecha >= desde && $0.fecha <= hasta }
            .sorted { ($0.fecha, $0.horaInicio) < ($1.fecha, $1.horaInicio) }
    }

    func esHoy(_ turno: TurnoPersonal) -> Bool { turno.fecha == Formato.hoyAPI }

    /// El turno que cubre la hora actual.
    func esActual(_ turno: TurnoPersonal) -> Bool {
        esHoy(turno) && Formato.horaActualEntre(turno.horaInicio, turno.horaFin)
    }

    /// Cumplido, turno actual, próximo (hoy, todavía no empieza) o programado (otro día).
    func etiqueta(_ turno: TurnoPersonal) -> (texto: String, tono: TonoEstado) {
        let hoy = Formato.hoyAPI
        if turno.fecha < hoy { return ("Cumplido", .exito) }
        if turno.fecha > hoy { return ("Programado", .neutro) }
        if esActual(turno) { return ("Turno actual", .info) }
        return Formato.hora(turno.horaInicio) > Formato.ahoraHHmm ? ("Próximo", .advertencia) : ("Cumplido", .exito)
    }

    func cargar() async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.mios())
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }
}
