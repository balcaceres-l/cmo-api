//
//  Aviso.swift
//  appcmo
//

import Foundation

/// Aviso del hospital (`GET /avisos`). Para el personal llegan los públicos y los internos.
nonisolated struct Aviso: Decodable, Sendable, Equatable, Hashable, Identifiable {
    let idAviso: String
    let titulo: String
    let contenido: String
    let publico: Bool
    let vigenteDesde: String
    let vigenteHasta: String?

    var id: String { idAviso }

    /// Un aviso vencido (o que todavía no empieza) no se muestra.
    func estaVigente(en ahora: Date = Date()) -> Bool {
        if let desde = Formato.instante(vigenteDesde), desde > ahora { return false }
        if let hasta = Formato.instante(vigenteHasta), hasta < ahora { return false }
        return true
    }

    enum CodingKeys: String, CodingKey {
        case idAviso = "id_aviso"
        case titulo, contenido, publico
        case vigenteDesde = "vigente_desde"
        case vigenteHasta = "vigente_hasta"
    }
}

extension Aviso {
    /// Un aviso de `GET /publico/avisos` (sin id ni bandera: todos son públicos).
    init(publico aviso: AvisoPublico) {
        self.init(
            idAviso: aviso.id,
            titulo: aviso.titulo,
            contenido: aviso.contenido,
            publico: true,
            vigenteDesde: aviso.vigenteDesde ?? "",
            vigenteHasta: aviso.vigenteHasta
        )
    }
}
