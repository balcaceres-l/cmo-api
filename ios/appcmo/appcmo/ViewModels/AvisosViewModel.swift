//
//  AvisosViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Avisos del hospital vigentes (HU-72). Los internos solo los recibe el personal.
@Observable
final class AvisosViewModel {
    private(set) var carga: Carga<[Aviso]> = .inicial

    @ObservationIgnored private let repositorio: AvisosRepository

    init(repositorio: AvisosRepository = AvisosRepository()) {
        self.repositorio = repositorio
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
            carga = .fallo(mensaje)
        }
    }
}
