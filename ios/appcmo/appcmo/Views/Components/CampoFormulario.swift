//
//  CampoFormulario.swift
//  appcmo
//

import SwiftUI

/// Campo con título, borde y mensaje de error junto al campo (los 422 de la API).
struct CampoFormulario<Contenido: View>: View {
    let titulo: String
    var error: String?
    @ViewBuilder let contenido: () -> Contenido

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(titulo)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.cmoTintaSuave)
            contenido()
                .padding(.horizontal, 12)
                .frame(minHeight: 46)
                .background(Color.cmoFondo, in: .rect(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(error == nil ? Color.cmoLinea : Color.cmoError))
            if let error {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(Color.cmoError)
            }
        }
    }
}

/// Botón principal de ancho completo con indicador de carga.
struct BotonPrincipal: View {
    let titulo: String
    var cargando = false
    var deshabilitado = false
    let accion: () -> Void

    var body: some View {
        Button(action: accion) {
            ZStack {
                Text(titulo).opacity(cargando ? 0 : 1)
                if cargando { ProgressView().tint(Color.cmoSobreMarca) }
            }
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 48)
        }
        .buttonStyle(.borderedProminent)
        .tint(Color.cmoMarca)
        .foregroundStyle(Color.cmoSobreMarca)
        .disabled(deshabilitado || cargando)
    }
}

/// Botón de contorno de ancho completo (acciones secundarias).
struct BotonSecundario: View {
    let titulo: String
    var sistema: String?
    var deshabilitado = false
    let accion: () -> Void

    var body: some View {
        Button(action: accion) {
            HStack(spacing: 8) {
                if let sistema { Image(systemName: sistema) }
                Text(titulo)
            }
            .font(.subheadline.weight(.semibold))
            .frame(maxWidth: .infinity, minHeight: 46)
            .foregroundStyle(Color.cmoMarca)
            .background(Color.cmoSuperficie, in: .rect(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.cmoMarca.opacity(0.45)))
        }
        .buttonStyle(.plain)
        .disabled(deshabilitado)
        .opacity(deshabilitado ? 0.5 : 1)
    }
}
