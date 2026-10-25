//
//  InfoPublicaView.swift
//  appcmo
//

import SwiftUI

/// Información pública del hospital (pantalla 01, HU-68): camas disponibles, médicos de turno, avisos y estado de la
/// ambulancia. Funciona sin iniciar sesión y nunca muestra datos de pacientes.
struct InfoPublicaView: View {
    /// Acciones del visitante que aún no inicia sesión (los botones del pie).
    struct Acciones {
        let iniciarSesion: () -> Void
        let crearCuenta: () -> Void
    }

    @State private var viewModel = PublicoViewModel()
    let acciones: Acciones?
    /// Quien ya inició sesión ve los avisos con la campana, así que ahí no se repiten.
    let mostrarAvisos: Bool

    init(acciones: Acciones? = nil, mostrarAvisos: Bool = true) {
        self.acciones = acciones
        self.mostrarAvisos = mostrarAvisos
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                encabezado
                ambulancia
                camas
                medicos
                if mostrarAvisos { avisos }
                if let acciones {
                    VStack(spacing: 10) {
                        BotonPrincipal(titulo: "Iniciar sesión", accion: acciones.iniciarSesion)
                            .accessibilityIdentifier("publico.iniciarSesion")
                        BotonSecundario(titulo: "Crear cuenta de paciente", accion: acciones.crearCuenta)
                            .accessibilityIdentifier("publico.crearCuenta")
                    }
                    .padding(.top, 6)
                }
            }
            .padding(16)
        }
        .background(Color.cmoFondo)
        .navigationTitle("Hospital")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.cargarTodo() }
        .task { await viewModel.vigilarAmbulancia() }
        .refreshable { await viewModel.cargarTodo() }
    }

    // MARK: - Secciones

    private var encabezado: some View {
        HStack(spacing: 12) {
            Image("Logo")
                .resizable()
                .scaledToFit()
                .frame(width: 52, height: 52)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text("Centro Médico de Occidente").font(.headline).foregroundStyle(Color.cmoTinta)
                Text("Información pública").font(.footnote).foregroundStyle(Color.cmoTintaSuave)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }

    private var ambulancia: some View {
        VStack(alignment: .leading, spacing: 0) {
            switch viewModel.ambulancia {
            case .inicial, .cargando:
                cargando
            case .fallo(let mensaje):
                Text(mensaje).font(.subheadline).foregroundStyle(Color.cmoError)
            case .listo(let ambulancia):
                HStack(spacing: 12) {
                    IconoCirculo(sistema: "cross.vial", tono: ambulancia.estado == "DISPONIBLE" ? .exito : .advertencia, tamano: 40)
                    Text("Ambulancia").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                    Spacer()
                    EstadoChip(texto: ambulancia.nombre, tono: ambulancia.estado == "DISPONIBLE" ? .exito : .advertencia)
                }
                .accessibilityElement(children: .combine)
                .accessibilityIdentifier("publico.ambulancia")
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .tarjeta(relleno: 12)
    }

    private var camas: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Camas disponibles").font(.headline).foregroundStyle(Color.cmoTinta)
            switch viewModel.ocupacion {
            case .inicial, .cargando:
                cargando
            case .fallo(let mensaje):
                Text(mensaje).font(.subheadline).foregroundStyle(Color.cmoError)
            case .listo(let grupos):
                if grupos.isEmpty {
                    Text("Sin información de camas.").font(.subheadline).foregroundStyle(Color.cmoTintaSuave)
                }
                // La planta más alta primero (VIP), como en el diseño.
                ForEach(Array(grupos.reversed())) { grupo in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("Planta \(grupo.planta) · \(grupo.tipo == "VIP" ? "VIP" : "Semiprivadas")")
                                .font(.subheadline)
                                .foregroundStyle(Color.cmoTinta)
                            Spacer()
                            Text("\(grupo.disponibles) de \(grupo.total) libres")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(grupo.disponibles > 0 ? Color.cmoExito : Color.cmoError)
                        }
                        ProgressView(value: Double(grupo.disponibles), total: Double(max(grupo.total, 1)))
                            .tint(grupo.disponibles > 0 ? Color.cmoExito : Color.cmoError)
                    }
                    .accessibilityElement(children: .combine)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .tarjeta()
    }

    private var medicos: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Médicos de turno ahora").font(.headline).foregroundStyle(Color.cmoTinta)
            switch viewModel.medicos {
            case .inicial, .cargando:
                cargando
            case .fallo(let mensaje):
                Text(mensaje).font(.subheadline).foregroundStyle(Color.cmoError)
            case .listo(let medicos) where medicos.isEmpty:
                Text("No hay médicos de turno en este momento.").font(.subheadline).foregroundStyle(Color.cmoTintaSuave)
            case .listo(let medicos):
                ForEach(medicos) { medico in
                    HStack(spacing: 12) {
                        AvatarIniciales(nombre: medico.nombre, tono: .info, tamano: 36)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(medico.nombre).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                            Text([medico.cargo, medico.area].compactMap { $0 }.joined(separator: " · "))
                                .font(.caption)
                                .foregroundStyle(Color.cmoTintaSuave)
                        }
                        Spacer(minLength: 0)
                    }
                    .accessibilityElement(children: .combine)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .tarjeta()
    }

    @ViewBuilder
    private var avisos: some View {
        switch viewModel.avisos {
        case .inicial, .cargando:
            EmptyView()
        case .fallo(let mensaje):
            Text(mensaje).font(.subheadline).foregroundStyle(Color.cmoError)
        case .listo(let avisos):
            ForEach(avisos) { aviso in
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "megaphone")
                        .foregroundStyle(Color.cmoAdvertencia)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(aviso.titulo).font(.subheadline.weight(.semibold))
                        Text(aviso.contenido).font(.footnote)
                    }
                    Spacer(minLength: 0)
                }
                .foregroundStyle(Color.cmoAdvertencia)
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.cmoAdvertenciaSuave, in: .rect(cornerRadius: 14))
                .accessibilityElement(children: .combine)
                .accessibilityIdentifier("publico.aviso")
            }
        }
    }

    private var cargando: some View {
        ProgressView().frame(maxWidth: .infinity)
    }
}
