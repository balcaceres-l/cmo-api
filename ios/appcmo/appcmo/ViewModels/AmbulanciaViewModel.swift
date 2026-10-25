//
//  AmbulanciaViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Estado de la ambulancia y del traslado en curso. La pantalla lo refresca sola cada 10 segundos.
@Observable
final class AmbulanciaViewModel {
    static let intervalo: Duration = .seconds(10)

    private(set) var carga: Carga<AmbulanciaMedico> = .inicial
    private(set) var ultimaActualizacion: Date?

    @ObservationIgnored private let repositorio: AmbulanciaRepository

    init(repositorio: AmbulanciaRepository = AmbulanciaRepository()) {
        self.repositorio = repositorio
    }

    func cargar() async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.estado())
            ultimaActualizacion = Date()
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            // Si ya había datos, un fallo de red pasajero no los borra.
            if carga.valor == nil { carga = .fallo(mensaje) }
        }
    }

    /// Refresca periódicamente mientras la pantalla está visible (se cancela al salir).
    func seguir() async {
        await cargar()
        while !Task.isCancelled {
            try? await Task.sleep(for: Self.intervalo)
            if Task.isCancelled { return }
            await cargar()
        }
    }
}
