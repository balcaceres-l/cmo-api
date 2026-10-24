//
//  AvisoBanner.swift
//  appcmo
//

import SwiftUI

/// Mensaje destacado sobre el formulario: error (rojo) o información (azul).
struct AvisoBanner: View {
    let aviso: LoginViewModel.Aviso

    private var esError: Bool {
        if case .error = aviso { true } else { false }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: esError ? "exclamationmark.triangle.fill" : "info.circle.fill")
                .accessibilityHidden(true)
            Text(aviso.texto)
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .foregroundStyle(esError ? Color.cmoError : Color.cmoInfo)
        .padding(12)
        .background(esError ? Color.cmoErrorSuave : Color.cmoInfoSuave, in: .rect(cornerRadius: 10))
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier(esError ? "login.error" : "login.info")
    }
}
