//
//  IngresadosViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Pacientes ingresados: "Míos" (los que están a cargo del médico) y "Todos" (mapa de camas).
/// En "Todos" solo se ve el paciente de las camas propias; las demás salen como ocupadas, sin identidad (HU-53/HU-79).
@Observable
final class IngresadosViewModel {
    enum Vista: String, CaseIterable, Identifiable {
        case mios = "Míos", todos = "Todos"
        var id: String { rawValue }
    }

    var vista: Vista = .mios
    private(set) var mios: Carga<[Ingreso]> = .inicial
    private(set) var mapa: Carga<MapaHabitaciones> = .inicial

    @ObservationIgnored private let repositorio: HospitalizacionRepository

    init(repositorio: HospitalizacionRepository = HospitalizacionRepository()) {
        self.repositorio = repositorio
    }

    var camasLibres: Int { mapa.valor?.plantas.reduce(0) { $0 + $1.camasLibres } ?? 0 }
    var camasTotales: Int { mapa.valor?.plantas.reduce(0) { $0 + $1.camasTotales } ?? 0 }

    func cargar() async {
        mios = mios.alRecargar
        mapa = mapa.alRecargar
        async let propios = repositorio.ingresosActivos()
        async let camas = repositorio.mapa()
        do {
            mios = .listo(try await propios)
        } catch {
            if let mensaje = mensajeDeError(error) { mios = .fallo(mensaje) }
        }
        do {
            mapa = .listo(try await camas)
        } catch {
            if let mensaje = mensajeDeError(error) { mapa = .fallo(mensaje) }
        }
    }
}
