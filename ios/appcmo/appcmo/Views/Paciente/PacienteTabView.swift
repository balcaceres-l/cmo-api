//
//  PacienteTabView.swift
//  appcmo
//

import SwiftUI

/// Navegación principal del rol PACIENTE: Inicio, Citas, Expediente, Laboratorio y Perfil (como en el Figma).
struct PacienteTabView: View {
    enum Pestana: Hashable {
        case inicio, citas, expediente, laboratorio, perfil
    }

    @Environment(SessionStore.self) private var store
    @Environment(\.scenePhase) private var fase
    @State private var seleccion: Pestana = .inicio
    @State private var datos = PacienteDatos()
    @State private var avisos = AvisosViewModel(publico: PublicoRepository())

    var body: some View {
        TabView(selection: $seleccion) {
            PacienteInicioView(irA: { seleccion = $0 })
                .tabItem { Label("Inicio", systemImage: "house") }
                .tag(Pestana.inicio)
            MisCitasView()
                .tabItem { Label("Citas", systemImage: "calendar") }
                .tag(Pestana.citas)
            MiExpedienteView()
                .tabItem { Label("Expediente", systemImage: "doc.text") }
                .tag(Pestana.expediente)
            MiLaboratorioView()
                .tabItem { Label("Laboratorio", systemImage: "testtube.2") }
                .tag(Pestana.laboratorio)
            PerfilPacienteView()
                .tabItem { Label("Perfil", systemImage: "person.crop.circle") }
                .tag(Pestana.perfil)
        }
        .environment(datos)
        .environment(avisos)
        .task {
            avisos.configurar(idUsuario: store.sesion?.usuario.id ?? "")
            async let a: Void = avisos.cargar()
            async let b: Void = datos.cargarTodo()
            _ = await (a, b)
        }
        .onChange(of: fase) {
            if fase == .active {
                Task {
                    await avisos.cargar()
                    await datos.cargarTodo()
                }
            }
        }
        .accessibilityIdentifier("inicio.paciente")
    }
}
