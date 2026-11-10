//
//  AmbulanciaViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Estado de la ambulancia y del traslado en curso. Se actualiza en vivo con Reverb (canales `ambulancia` y
/// `private-personal`, donde llegan los reportes del traslado); sin conexión en vivo, cada 10 segundos.
@Observable
final class AmbulanciaViewModel {
    static let intervalo: Duration = .seconds(10)

    private(set) var carga: Carga<AmbulanciaMedico> = .inicial
    private(set) var ultimaActualizacion: Date?
    /// Hay conexión en vivo con Reverb.
    private(set) var enVivo = false

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

    /// Mantiene la pantalla al día mientras está visible (se cancela al salir).
    func seguir() async {
        await cargar()
        await TiempoReal.vigilar(
            canales: ["ambulancia", "private-personal"],
            intervalo: Self.intervalo,
            enVivo: { [weak self] in self?.enVivo = $0 },
            refrescar: { [weak self] in await self?.cargar() }
        )
    }
}
