//
//  AvisoTarjeta.swift
//  appcmo
//

import SwiftUI

/// Aviso del hospital. Los internos (solo para el personal) se distinguen con otro color y la etiqueta "Interno".
struct AvisoTarjeta: View {
    let aviso: Aviso

    private var tono: TonoEstado { aviso.publico ? .info : .advertencia }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            IconoCirculo(sistema: aviso.publico ? "megaphone" : "lock", tono: tono)
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline) {
                    Text(aviso.titulo)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.cmoTinta)
                    Spacer(minLength: 8)
                    if !aviso.publico {
                        EstadoChip(texto: "Interno", tono: .advertencia)
                    }
                }
                Text(aviso.contenido)
                    .font(.caption)
                    .foregroundStyle(Color.cmoTinta)
                if let hasta = aviso.vigenteHasta {
                    Text("Vigente hasta el \(Formato.fechaCorta(hasta))")
                        .font(.caption2)
                        .foregroundStyle(Color.cmoTintaSuave)
                }
            }
        }
        .tarjeta()
        .accessibilityElement(children: .combine)
    }
}
