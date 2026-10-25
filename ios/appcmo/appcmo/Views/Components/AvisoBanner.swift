//
//  AvisoBanner.swift
//  appcmo
//

import SwiftUI

/// Mensaje destacado sobre un formulario o una lista.
enum AvisoMensaje: Equatable {
    case error(String)
    case informacion(String)
    case exito(String)

    var texto: String {
        switch self {
        case .error(let texto), .informacion(let texto), .exito(let texto): texto
        }
    }
}

/// Mensaje destacado: error (rojo), información (azul) o éxito (verde).
struct AvisoBanner: View {
    let aviso: AvisoMensaje

    private var icono: String {
        switch aviso {
        case .error: "exclamationmark.triangle.fill"
        case .informacion: "info.circle.fill"
        case .exito: "checkmark.circle.fill"
        }
    }

    private var primerPlano: Color {
        switch aviso {
        case .error: .cmoError
        case .informacion: .cmoInfo
        case .exito: .cmoExito
        }
    }

    private var fondo: Color {
        switch aviso {
        case .error: .cmoErrorSuave
        case .informacion: .cmoInfoSuave
        case .exito: .cmoExitoSuave
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icono)
                .accessibilityHidden(true)
            Text(aviso.texto)
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .foregroundStyle(primerPlano)
        .padding(12)
        .background(fondo, in: .rect(cornerRadius: 10))
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier({
            switch aviso {
            case .error: "login.error"
            case .informacion: "login.info"
            case .exito: "aviso.exito"
            }
        }())
    }
}
