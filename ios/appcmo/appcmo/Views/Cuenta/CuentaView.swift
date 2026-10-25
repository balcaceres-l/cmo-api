//
//  CuentaView.swift
//  appcmo
//

import SwiftUI

/// Perfil, cambio de contraseña y cierre de sesión (HU-23).
struct CuentaView: View {
    @Environment(SessionStore.self) private var store
    @State private var confirmando = false

    var body: some View {
        NavigationStack {
            List {
                if let sesion = store.sesion {
                    Section {
                        PerfilCard(usuario: sesion.usuario, icono: sesion.usuario.rol == .medico ? "stethoscope" : "person.fill")
                            .listRowInsets(EdgeInsets())
                            .listRowBackground(Color.clear)
                    }
                }
                Section("Seguridad") {
                    NavigationLink("Cambiar contraseña") {
                        CambiarPasswordView(store: store)
                    }
                    .accessibilityIdentifier("cuenta.cambiarPassword")
                }
                Section {
                    Button("Cerrar sesión", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                        confirmando = true
                    }
                    .accessibilityIdentifier("cuenta.cerrarSesion")
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.cmoFondo)
            .navigationTitle("Cuenta")
            .confirmationDialog("¿Cerrar sesión?", isPresented: $confirmando, titleVisibility: .visible) {
                Button("Cerrar sesión", role: .destructive) {
                    Task { await store.cerrarSesion() }
                }
                Button("Cancelar", role: .cancel) {}
            }
        }
    }
}
