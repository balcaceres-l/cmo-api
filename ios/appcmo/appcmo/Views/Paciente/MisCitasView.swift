//
//  MisCitasView.swift
//  appcmo
//

import SwiftUI

/// Mis citas y solicitud de atención del paciente (HU-37). Reprogramar o cancelar lo hace recepción.
struct MisCitasView: View {
    @State private var viewModel = MisCitasViewModel()
    @State private var solicitando = false

    var body: some View {
        NavigationStack {
            EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { citas in
                List {
                    if let aviso = viewModel.aviso {
                        Section { AvisoBanner(aviso: aviso) }
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets())
                    }
                    if citas.isEmpty {
                        InicioVacio(
                            icono: "calendar",
                            titulo: "Aún no tienes citas",
                            detalle: "Pide atención con el botón + y recepción te asignará médico, fecha y hora."
                        )
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                    }
                    seccion("Pendientes de confirmación", viewModel.pendientes)
                    seccion("Próximas", viewModel.proximas)
                    seccion("Anteriores", viewModel.anteriores)
                }
                .scrollContentBackground(.hidden)
                .refreshable { await viewModel.cargar() }
                .accessibilityIdentifier("citas.lista")
            }
            .background(Color.cmoFondo)
            .navigationTitle("Mis citas")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Solicitar atención", systemImage: "plus") { solicitando = true }
                        .accessibilityIdentifier("citas.solicitar")
                }
            }
            .sheet(isPresented: $solicitando) {
                SolicitudAtencionView(viewModel: viewModel)
            }
            .onAppear { Task { await viewModel.cargar() } }
        }
    }

    @ViewBuilder
    private func seccion(_ titulo: String, _ citas: [Cita]) -> some View {
        if !citas.isEmpty {
            Section(titulo) {
                ForEach(citas) { cita in
                    CitaPacienteFila(cita: cita)
                }
            }
        }
    }
}

/// Una cita del paciente: fecha, hora, consultorio y el nombre y cargo de su médico, sin más datos de él.
struct CitaPacienteFila: View {
    let cita: Cita

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(alignment: .firstTextBaseline) {
                if cita.estado == .solicitada {
                    Text("Sin fecha asignada").font(.headline)
                } else {
                    Text(Formato.fechaLarga(cita.fecha)).font(.headline)
                }
                Spacer()
                EstadoChip(texto: cita.estado.nombre, tono: cita.estado.tono)
            }
            if cita.estado != .solicitada {
                Text(Formato.rangoHoras(cita.horaInicio, cita.horaFin))
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(Color.cmoMarca)
            }
            Text(cita.motivo).font(.subheadline)
            if let medico = cita.medico {
                Label([medico.nombreCompleto, medico.cargo].compactMap { $0 }.joined(separator: " · "), systemImage: "stethoscope")
                    .font(.footnote)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            if let consultorio = cita.consultorio {
                Label("\(consultorio.codigo) · \(consultorio.nombre)", systemImage: "door.left.hand.open")
                    .font(.footnote)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            if cita.estado == .solicitada {
                Text("Recepción te asignará médico, fecha y hora.")
                    .font(.footnote)
                    .foregroundStyle(Color.cmoAdvertencia)
            }
        }
        .padding(.vertical, 4)
        .opacity(cita.estado.estaInactiva ? 0.65 : 1)
    }
}

/// Pedir atención indicando el motivo: queda "Pendiente de confirmación".
struct SolicitudAtencionView: View {
    @Environment(\.dismiss) private var cerrar
    let viewModel: MisCitasViewModel

    var body: some View {
        @Bindable var viewModel = viewModel
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Cuéntanos por qué necesitas atención. Recepción revisará tu solicitud y te asignará médico, fecha y hora.")
                        .font(.subheadline)
                        .foregroundStyle(Color.cmoTintaSuave)

                    if let aviso = viewModel.avisoSolicitud {
                        AvisoBanner(aviso: aviso)
                    }

                    CampoFormulario(titulo: "Motivo") {
                        TextField("Ej. Dolor de cabeza desde hace tres días", text: $viewModel.motivo, axis: .vertical)
                            .lineLimit(3...6)
                            .padding(.vertical, 10)
                            .accessibilityIdentifier("solicitud.motivo")
                    }

                    BotonPrincipal(titulo: "Enviar solicitud", cargando: viewModel.enviando, deshabilitado: !viewModel.puedeSolicitar) {
                        Task { if await viewModel.solicitar() { cerrar() } }
                    }
                    .accessibilityIdentifier("solicitud.enviar")
                }
                .padding(20)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Color.cmoFondo)
            .navigationTitle("Solicitar atención")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { cerrar() } }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
