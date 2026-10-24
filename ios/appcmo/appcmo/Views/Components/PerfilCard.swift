//
//  PerfilCard.swift
//  appcmo
//

import SwiftUI

/// Tarjeta con el nombre y el cargo de quien inició sesión.
struct PerfilCard: View {
    let usuario: Usuario
    let icono: String

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icono)
                .font(.title2)
                .foregroundStyle(Color.cmoMarca)
                .frame(width: 52, height: 52)
                .background(Color.cmoMarcaSuave, in: .circle)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(usuario.nombreCompleto)
                    .font(.headline)
                    .foregroundStyle(Color.cmoTinta)
                    .accessibilityIdentifier("inicio.nombre")
                if let cargo = usuario.cargo {
                    Text(cargo)
                        .font(.subheadline)
                        .foregroundStyle(Color.cmoTintaSuave)
                }
                Text(usuario.rol.nombre)
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .foregroundStyle(Color.cmoMarca)
                    .background(Color.cmoMarcaSuave, in: .capsule)
                    .accessibilityIdentifier("inicio.rol")
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(Color.cmoSuperficie, in: .rect(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.cmoLinea))
        .accessibilityElement(children: .combine)
    }
}

/// Estado vacío de los inicios mientras no existan los módulos.
struct InicioVacio: View {
    let icono: String
    let titulo: String
    let detalle: String

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: icono)
                .font(.system(size: 40))
                .foregroundStyle(Color.cmoTintaSuave)
                .accessibilityHidden(true)
            Text(titulo)
                .font(.headline)
                .foregroundStyle(Color.cmoTinta)
            Text(detalle)
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.cmoTintaSuave)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
        .padding(.horizontal, 20)
    }
}
