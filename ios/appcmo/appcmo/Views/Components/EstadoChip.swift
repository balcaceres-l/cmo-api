//
//  EstadoChip.swift
//  appcmo
//

import SwiftUI

extension TonoEstado {
    var primerPlano: Color {
        switch self {
        case .neutro: .cmoTintaSuave
        case .info: .cmoInfo
        case .exito: .cmoExito
        case .advertencia: .cmoAdvertencia
        case .error: .cmoError
        }
    }

    var fondo: Color {
        switch self {
        case .neutro: .cmoMarcaSuave
        case .info: .cmoInfoSuave
        case .exito: .cmoExitoSuave
        case .advertencia: .cmoAdvertenciaSuave
        case .error: .cmoErrorSuave
        }
    }
}

/// Etiqueta de estado de citas y órdenes de laboratorio.
struct EstadoChip: View {
    typealias Tono = TonoEstado

    let texto: String
    let tono: TonoEstado

    var body: some View {
        Text(texto)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .foregroundStyle(tono.primerPlano)
            .background(tono.fondo, in: .capsule)
            .fixedSize()
    }
}

extension EstadoCita {
    /// Tono para las vistas del paciente.
    var tono: TonoEstado {
        switch self {
        case .solicitada: .advertencia
        case .agendada: .info
        case .enEspera: .advertencia
        case .enAtencion: .exito
        case .atendida: .neutro
        case .cancelada, .noAsistio: .error
        case .desconocido: .neutro
        }
    }
}

extension EstadoOrden {
    var tono: TonoEstado {
        switch self {
        case .pendiente: .advertencia
        case .enProceso: .info
        case .completado: .exito
        case .desconocido: .neutro
        }
    }
}
