//
//  Pagina.swift
//  appcmo
//

import Foundation

/// Listado paginado de la API: el paginador de Laravel dentro de `data`.
nonisolated struct Pagina<Elemento: Decodable & Sendable>: Decodable, Sendable {
    let data: [Elemento]
    let paginaActual: Int
    let ultimaPagina: Int
    let total: Int

    enum CodingKeys: String, CodingKey {
        case data
        case paginaActual = "current_page"
        case ultimaPagina = "last_page"
        case total
    }
}
