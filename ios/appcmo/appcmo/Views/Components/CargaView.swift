//
//  CargaView.swift
//  appcmo
//

import SwiftUI

/// Estado de una pantalla que carga datos de la API.
enum Carga<Valor> {
    case inicial
    case cargando
    case listo(Valor)
    case fallo(String)

    var valor: Valor? {
        if case .listo(let valor) = self { valor } else { nil }
    }

    /// Si ya hay datos se conservan mientras se vuelve a pedir (no parpadea la pantalla).
    var alRecargar: Carga<Valor> {
        if case .listo = self { self } else { .cargando }
    }
}

/// Muestra el indicador de carga, el error con "Reintentar" o el contenido.
struct EstadoCarga<Valor, Contenido: View>: View {
    let carga: Carga<Valor>
    let reintentar: () -> Void
    @ViewBuilder let contenido: (Valor) -> Contenido

    var body: some View {
        switch carga {
        case .inicial, .cargando:
            ProgressView("Cargando…")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .fallo(let mensaje):
            VStack(spacing: 12) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 36))
                    .foregroundStyle(Color.cmoTintaSuave)
                    .accessibilityHidden(true)
                Text(mensaje)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(Color.cmoTinta)
                Button("Reintentar", action: reintentar)
                    .buttonStyle(.borderedProminent)
                    .tint(Color.cmoMarca)
                    .foregroundStyle(Color.cmoSobreMarca)
            }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .listo(let valor):
            contenido(valor)
        }
    }
}

/// Texto de un error para mostrarlo al usuario (`nil` si fue una cancelación).
func mensajeDeError(_ error: Error) -> String? {
    if error is CancellationError { return nil }
    return error.localizedDescription
}
