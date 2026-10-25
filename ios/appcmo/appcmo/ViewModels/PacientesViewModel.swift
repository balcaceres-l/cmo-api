//
//  PacientesViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// "Mis pacientes": la API solo devuelve a los que el médico trata (HU-29).
@Observable
final class PacientesViewModel {
    var texto = ""
    private(set) var carga: Carga<[Paciente]> = .inicial

    @ObservationIgnored private let repositorio: PacientesRepository

    init(repositorio: PacientesRepository = PacientesRepository()) {
        self.repositorio = repositorio
    }

    func buscar() async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.buscar(texto))
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }
}
