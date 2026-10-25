//
//  LaboratorioViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Órdenes de laboratorio que solicitó el médico, con su estado y fechas (HU-47).
/// Se piden todas una vez y el filtro por estado se aplica en la app, así los conteos siempre cuadran.
@Observable
final class LaboratorioViewModel {
    var filtro: EstadoOrden?
    private(set) var carga: Carga<[OrdenLaboratorio]> = .inicial
    var error: String?

    @ObservationIgnored private let repositorio: LaboratorioRepository

    init(repositorio: LaboratorioRepository = LaboratorioRepository()) {
        self.repositorio = repositorio
    }

    private var todas: [OrdenLaboratorio] { carga.valor ?? [] }

    /// Las órdenes que se muestran según el filtro elegido.
    var ordenes: [OrdenLaboratorio] {
        guard let filtro else { return todas }
        return todas.filter { $0.estado == filtro }
    }

    func cuantas(_ estado: EstadoOrden) -> Int {
        todas.filter { $0.estado == estado }.count
    }

    /// Tocar el mismo estado otra vez quita el filtro.
    func alternar(_ estado: EstadoOrden) {
        filtro = (filtro == estado) ? nil : estado
    }

    func cargar() async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.ordenes(estado: nil))
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }

    /// Cancela una orden PENDIENTE. Si el laboratorio ya la inició, la API responde 409 y se vuelve a pedir la lista.
    func cancelar(_ orden: OrdenLaboratorio) async {
        error = nil
        do {
            _ = try await repositorio.cancelar(idOrden: orden.idOrden)
        } catch {
            self.error = mensajeDeError(error)
        }
        await cargar()
    }
}
