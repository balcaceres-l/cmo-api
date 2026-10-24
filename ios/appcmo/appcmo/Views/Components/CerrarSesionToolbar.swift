//
//  CerrarSesionToolbar.swift
//  appcmo
//

import SwiftUI

/// Menú de cuenta con "Cerrar sesión" para la barra de navegación de los inicios.
private struct CerrarSesionToolbar: ViewModifier {
    @Environment(SessionStore.self) private var store
    @State private var confirmando = false

    func body(content: Content) -> some View {
        content
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("Cerrar sesión", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                            confirmando = true
                        }
                    } label: {
                        Image(systemName: "person.crop.circle")
                    }
                    .accessibilityLabel("Cuenta")
                    .accessibilityIdentifier("cuenta.menu")
                }
            }
            .confirmationDialog("¿Cerrar sesión?", isPresented: $confirmando, titleVisibility: .visible) {
                Button("Cerrar sesión", role: .destructive) {
                    Task { await store.cerrarSesion() }
                }
                Button("Cancelar", role: .cancel) {}
            }
    }
}

extension View {
    func cerrarSesionToolbar() -> some View {
        modifier(CerrarSesionToolbar())
    }
}
