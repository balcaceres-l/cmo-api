//
//  AvisosViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Avisos del hospital vigentes (HU-72). Los internos solo los recibe el personal.
/// Se comparte entre todas las pantallas para que la campana muestre cuántos avisos no se han visto.
@Observable
final class AvisosViewModel {
    private(set) var carga: Carga<[Aviso]> = .inicial
    private(set) var vistos: Set<String> = []

    @ObservationIgnored private var idUsuario = ""
    @ObservationIgnored private let repositorio: AvisosRepository
    @ObservationIgnored private let defaults: UserDefaults

    init(repositorio: AvisosRepository = AvisosRepository(), defaults: UserDefaults = .standard) {
        self.repositorio = repositorio
        self.defaults = defaults
    }

    private var llave: String { "avisosVistos.\(idUsuario)" }

    /// Avisos que la persona todavía no abrió desde la campana.
    var sinLeer: Int {
        (carga.valor ?? []).filter { !vistos.contains($0.id) }.count
    }

    func esNuevo(_ aviso: Aviso) -> Bool { !vistos.contains(aviso.id) }

    /// Cada usuario lleva su propia lista de avisos vistos en este iPhone.
    func configurar(idUsuario: String) {
        guard self.idUsuario != idUsuario else { return }
        self.idUsuario = idUsuario
        vistos = Set(defaults.stringArray(forKey: llave) ?? [])
    }

    func marcarVistos() {
        guard let lista = carga.valor else { return }
        vistos.formUnion(lista.map(\.id))
        // Solo se guardan los que siguen vigentes, para que la lista no crezca sin fin.
        defaults.set(Array(vistos.intersection(lista.map(\.id))), forKey: llave)
    }

    func cargar() async {
        carga = carga.alRecargar
        do {
            let ahora = Date()
            let vigentes = try await repositorio.todos().filter { $0.estaVigente(en: ahora) }
            // Primero los internos (solo para el personal), luego por fecha de inicio, el más reciente arriba.
            carga = .listo(vigentes.sorted {
                if $0.publico != $1.publico { return !$0.publico }
                return $0.vigenteDesde > $1.vigenteDesde
            })
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            // Si ya había avisos, un fallo de red pasajero no los borra.
            if carga.valor == nil { carga = .fallo(mensaje) }
        }
    }
}
