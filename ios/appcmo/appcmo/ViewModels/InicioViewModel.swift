//
//  InicioViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Dashboard del médico: resumen de su día y la consulta que sigue.
@Observable
final class InicioViewModel {
    private(set) var carga: Carga<[Cita]> = .inicial

    @ObservationIgnored private let repositorio: CitasRepository

    init(repositorio: CitasRepository = CitasRepository()) {
        self.repositorio = repositorio
    }

    /// Sus citas de hoy, por hora.
    var citasDeHoy: [Cita] {
        guard case .listo(let citas) = carga else { return [] }
        let hoy = Formato.hoyAPI
        return citas
            .filter { $0.fecha == hoy }
            .sorted { ($0.horaInicio ?? "") < ($1.horaInicio ?? "") }
    }

    var atendidas: Int { citasDeHoy.filter { $0.estado == .atendida }.count }
    var enSala: Int { citasDeHoy.filter { $0.estado == .enEspera || $0.estado == .enAtencion }.count }
    var porVenir: Int { citasDeHoy.filter { $0.estado == .agendada }.count }

    /// La consulta en curso o, si no hay, la siguiente por atender.
    var destacada: Cita? {
        let hoy = citasDeHoy
        return hoy.first { $0.estado == .enAtencion } ?? hoy.first { $0.estado == .enEspera || $0.estado == .agendada }
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
