//
//  PacienteTabView.swift
//  appcmo
//

import SwiftUI

/// Navegación principal del rol PACIENTE.
struct PacienteTabView: View {
    var body: some View {
        TabView {
            MisCitasView()
                .tabItem { Label("Mis citas", systemImage: "calendar") }
            NavigationStack {
                InfoPublicaView()
            }
            .tabItem { Label("Hospital", systemImage: "cross.case") }
            CuentaView()
                .tabItem { Label("Cuenta", systemImage: "person.crop.circle") }
        }
        .accessibilityIdentifier("inicio.paciente")
    }
}
