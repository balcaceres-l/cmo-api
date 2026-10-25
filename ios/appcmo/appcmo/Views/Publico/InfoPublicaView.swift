//
//  InfoPublicaView.swift
//  appcmo
//

import SwiftUI

/// Información pública del hospital (HU-68). Funciona sin iniciar sesión y nunca muestra datos de pacientes.
struct InfoPublicaView: View {
    @State private var viewModel = PublicoViewModel()

    var body: some View {
        List {
            seccionAmbulancia
            seccionOcupacion
            seccionMedicos
            seccionAvisos
        }
        .scrollContentBackground(.hidden)
        .background(Color.cmoFondo)
        .navigationTitle("Hospital")
        .task { await viewModel.cargarTodo() }
        .task { await viewModel.vigilarAmbulancia() }
        .refreshable { await viewModel.cargarTodo() }
    }

    // MARK: - Secciones

    private var seccionAmbulancia: some View {
        Section {
            switch viewModel.ambulancia {
            case .inicial, .cargando:
                ProgressView()
            case .fallo(let mensaje):
                Text(mensaje).foregroundStyle(Color.cmoError)
            case .listo(let ambulancia):
                HStack {
                    Label("Ambulancia", systemImage: "cross.vial")
                    Spacer()
                    EstadoChip(texto: ambulancia.nombre, tono: ambulancia.estado == "DISPONIBLE" ? .exito : .advertencia)
                }
                .accessibilityIdentifier("publico.ambulancia")
            }
        } footer: {
            Text("Se actualiza cada 10 segundos.")
        }
    }

    private var seccionOcupacion: some View {
        Section("Camas disponibles") {
            switch viewModel.ocupacion {
            case .inicial, .cargando:
                ProgressView()
            case .fallo(let mensaje):
                Text(mensaje).foregroundStyle(Color.cmoError)
            case .listo(let grupos):
                ForEach(grupos) { grupo in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(grupo.tipoNombre)
                            Text("Planta \(grupo.planta)")
                                .font(.footnote)
                                .foregroundStyle(Color.cmoTintaSuave)
                        }
                        Spacer()
                        Text("\(grupo.disponibles) de \(grupo.total)")
                            .font(.headline)
                            .foregroundStyle(grupo.disponibles > 0 ? Color.cmoExito : Color.cmoError)
                    }
                }
            }
        }
    }

    private var seccionMedicos: some View {
        Section("Médicos de turno ahora") {
            switch viewModel.medicos {
            case .inicial, .cargando:
                ProgressView()
            case .fallo(let mensaje):
                Text(mensaje).foregroundStyle(Color.cmoError)
            case .listo(let medicos) where medicos.isEmpty:
                Text("No hay médicos de turno en este momento.")
                    .foregroundStyle(Color.cmoTintaSuave)
            case .listo(let medicos):
                ForEach(medicos) { medico in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(medico.nombre)
                        Text([medico.cargo, medico.area].compactMap { $0 }.joined(separator: " · "))
                            .font(.footnote)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                }
            }
        }
    }

    private var seccionAvisos: some View {
        Section("Avisos") {
            switch viewModel.avisos {
            case .inicial, .cargando:
                ProgressView()
            case .fallo(let mensaje):
                Text(mensaje).foregroundStyle(Color.cmoError)
            case .listo(let avisos) where avisos.isEmpty:
                Text("No hay avisos vigentes.")
                    .foregroundStyle(Color.cmoTintaSuave)
            case .listo(let avisos):
                ForEach(avisos) { aviso in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(aviso.titulo).font(.headline)
                        Text(aviso.contenido)
                            .font(.subheadline)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                    .padding(.vertical, 2)
                }
            }
        }
    }
}
