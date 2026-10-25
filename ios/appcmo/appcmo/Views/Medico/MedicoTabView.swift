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

    @Environment(SessionStore.self) private var store
    @Environment(\.scenePhase) private var fase
    @State private var seleccion: Pestana = .inicio
    @State private var avisos = AvisosViewModel()

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
        .environment(avisos)
        .task {
            avisos.configurar(idUsuario: store.sesion?.usuario.id ?? "")
            await avisos.cargar()
        }
        .onChange(of: fase) {
            if fase == .active { Task { await avisos.cargar() } }
        }
        .accessibilityIdentifier("inicio.medico")
    }
}
