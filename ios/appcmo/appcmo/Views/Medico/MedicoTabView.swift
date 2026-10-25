//
//  MedicoTabView.swift
//  appcmo
//

import SwiftUI

/// Navegación principal del rol MEDICO: Inicio, Agenda, Pacientes y Perfil (como en el Figma).
/// Laboratorio y Mis turnos se abren desde Perfil.
struct MedicoTabView: View {
    enum Pestana: Hashable {
        case inicio, agenda, pacientes, perfil
    }

    @State private var seleccion: Pestana = .inicio

    var body: some View {
        TabView(selection: $seleccion) {
            InicioView(verAgenda: { seleccion = .agenda })
                .tabItem { Label("Inicio", systemImage: "house") }
                .tag(Pestana.inicio)
            AgendaView()
                .tabItem { Label("Agenda", systemImage: "calendar") }
                .tag(Pestana.agenda)
            PacientesView()
                .tabItem { Label("Pacientes", systemImage: "person.2") }
                .tag(Pestana.pacientes)
            PerfilMedicoView()
                .tabItem { Label("Perfil", systemImage: "person.crop.circle") }
                .tag(Pestana.perfil)
        }
        .accessibilityIdentifier("inicio.medico")
    }
}
