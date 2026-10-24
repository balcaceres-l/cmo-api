//
//  RootView.swift
//  appcmo
//

import SwiftUI

/// Elige la pantalla según la sesión: login, inicio del médico o inicio del paciente.
struct RootView: View {
    @Environment(SessionStore.self) private var store

    var body: some View {
        Group {
            switch store.estado {
            case .restaurando:
                ProgressView("Cargando…")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.cmoFondo)
            case .noAutenticado:
                LoginView(store: store)
            case .autenticado(let sesion):
                inicio(para: sesion)
            }
        }
        .task { await store.restaurarSesion() }
        .onReceive(NotificationCenter.default.publisher(for: .apiSesionExpirada)) { _ in
            store.sesionExpirada()
        }
    }

    @ViewBuilder
    private func inicio(para sesion: Sesion) -> some View {
        switch sesion.usuario.rol {
        case .medico:
            MedicoHomeView(sesion: sesion)
        case .paciente:
            PacienteHomeView(sesion: sesion)
        case .administrador, .recepcionista:
            // No debería ocurrir: el store rechaza estos roles antes de guardar la sesión.
            LoginView(store: store)
        }
    }
}
