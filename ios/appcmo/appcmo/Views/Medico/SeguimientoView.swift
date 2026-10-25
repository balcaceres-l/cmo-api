//
//  SeguimientoView.swift
//  appcmo
//

import SwiftUI

/// Agendar un seguimiento de un paciente propio en un bloque libre de la agenda del médico (HU-36).
struct SeguimientoView: View {
    @Environment(\.dismiss) private var cerrar
    @State private var viewModel: SeguimientoViewModel
    @FocusState private var consultorioActivo: Bool
    private let alAgendar: () -> Void

    init(paciente: Paciente, idMedico: String, alAgendar: @escaping () -> Void) {
        _viewModel = State(initialValue: SeguimientoViewModel(paciente: paciente, idMedico: idMedico))
        self.alAgendar = alAgendar
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(viewModel.paciente.nombreCompleto)
                        .font(.title3.bold())
                    Text(viewModel.paciente.numeroExpediente)
                        .font(.subheadline)
                        .foregroundStyle(Color.cmoTintaSuave)

                    if let aviso = viewModel.aviso {
                        AvisoBanner(aviso: aviso)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Fecha").font(.footnote.weight(.semibold)).foregroundStyle(Color.cmoTintaSuave)
                        DatePicker("Fecha", selection: $viewModel.fecha, in: Date()..., displayedComponents: .date)
                            .labelsHidden()
                            .environment(\.locale, Locale(identifier: "es"))
                            .environment(\.timeZone, Formato.zona)
                    }

                    bloques

                    if !viewModel.consultorios.isEmpty {
                        consultorio
                    }

                    CampoFormulario(titulo: "Motivo del seguimiento") {
                        TextField("Ej. Control de presión", text: $viewModel.motivo, axis: .vertical)
                            .lineLimit(2...4)
                            .padding(.vertical, 10)
                            .accessibilityIdentifier("seguimiento.motivo")
                    }

                    BotonPrincipal(titulo: "Agendar seguimiento", cargando: viewModel.guardando, deshabilitado: !viewModel.puedeAgendar) {
                        Task { await viewModel.agendar() }
                    }
                    .accessibilityIdentifier("seguimiento.agendar")
                }
                .padding(20)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Color.cmoFondo)
            .navigationTitle("Agendar seguimiento")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { cerrar() }
                }
            }
            .task {
                await viewModel.cargarConsultorios()
                await viewModel.cargarBloques()
            }
            .onChange(of: viewModel.fecha) {
                Task { await viewModel.cargarBloques() }
            }
            .onChange(of: viewModel.agendada) {
                if viewModel.agendada {
                    alAgendar()
                    cerrar()
                }
            }
        }
    }

    /// Consultorio (opcional): se escribe y salen sugerencias; hay que elegir una de la lista.
    private var consultorio: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Consultorio (opcional)").font(.footnote.weight(.semibold)).foregroundStyle(Color.cmoTintaSuave)

            if let elegido = viewModel.consultorio {
                HStack(spacing: 10) {
                    Image(systemName: "door.left.hand.open").foregroundStyle(Color.cmoMarca)
                    Text("\(elegido.codigo) · \(elegido.nombre)").font(.subheadline)
                    Spacer(minLength: 8)
                    Button {
                        viewModel.quitarConsultorio()
                    } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(Color.cmoTintaSuave)
                    }
                    .accessibilityLabel("Quitar consultorio")
                }
                .padding(12)
                .background(Color.cmoMarcaSuave, in: .rect(cornerRadius: 10))
                .accessibilityIdentifier("seguimiento.consultorioElegido")
            } else {
                TextField("Escribe el código o el nombre (ej. C-101)", text: $viewModel.textoConsultorio)
                    .focused($consultorioActivo)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 12)
                    .frame(minHeight: 46)
                    .background(Color.cmoFondo, in: .rect(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(viewModel.consultorioPendiente ? Color.cmoAdvertencia : Color.cmoLinea))
                    .accessibilityIdentifier("seguimiento.consultorio")

                if consultorioActivo || viewModel.consultorioPendiente {
                    sugerencias
                }
            }
        }
    }

    private var sugerencias: some View {
        let opciones = viewModel.sugerencias
        return VStack(alignment: .leading, spacing: 0) {
            if opciones.isEmpty {
                Text("No hay consultorios con ese texto. Borra lo escrito o prueba con otro código.")
                    .font(.footnote)
                    .foregroundStyle(Color.cmoAdvertencia)
                    .padding(12)
            }
            ForEach(Array(opciones.enumerated()), id: \.element.id) { posicion, opcion in
                Button {
                    viewModel.elegir(opcion)
                    consultorioActivo = false
                } label: {
                    HStack {
                        Text(opcion.codigo).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoMarca)
                        Text(opcion.nombre).font(.subheadline).foregroundStyle(Color.cmoTinta)
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 12)
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("sugerencia.\(opcion.codigo)")
                if posicion < opciones.count - 1 { Divider().padding(.leading, 12) }
            }
        }
        .background(Color.cmoSuperficie, in: .rect(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.cmoLinea))
    }

    private var bloques: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Horarios libres").font(.footnote.weight(.semibold)).foregroundStyle(Color.cmoTintaSuave)
            switch viewModel.bloques {
            case .inicial, .cargando:
                ProgressView()
            case .fallo(let mensaje):
                Text(mensaje).font(.subheadline).foregroundStyle(Color.cmoError)
            case .listo(let libres) where libres.isEmpty:
                Text("No hay horarios libres este día. Elige otra fecha.")
                    .font(.subheadline)
                    .foregroundStyle(Color.cmoTintaSuave)
            case .listo(let libres):
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 92), spacing: 8)], spacing: 8) {
                    ForEach(libres) { bloque in
                        let elegido = viewModel.bloque == bloque
                        Button {
                            viewModel.bloque = bloque
                        } label: {
                            Text(Formato.hora(bloque.horaInicio))
                                .font(.subheadline.weight(.semibold).monospacedDigit())
                                .frame(maxWidth: .infinity, minHeight: 40)
                                .foregroundStyle(elegido ? Color.cmoSobreMarca : Color.cmoTinta)
                                .background(elegido ? Color.cmoMarca : Color.cmoSuperficie, in: .rect(cornerRadius: 10))
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(elegido ? Color.cmoMarca : Color.cmoLinea))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Horario \(Formato.hora(bloque.horaInicio))")
                        .accessibilityAddTraits(elegido ? .isSelected : [])
                    }
                }
            }
        }
    }
}
