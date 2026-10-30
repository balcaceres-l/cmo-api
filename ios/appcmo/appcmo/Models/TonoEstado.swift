//
//  TonoEstado.swift
//  appcmo
//

import Foundation

/// Intención de color de un estado (la vista decide el color exacto).
nonisolated enum TonoEstado: Sendable, Equatable {
    case neutro, info, exito, advertencia, error
}
