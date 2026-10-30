//
//  PacientesView.swift
//  appcmo
//

import SwiftUI

/// Mis pacientes (HU-29): solo aparecen los que el médico trata; la búsqueda es por nombre, DUI o expediente.
struct PacientesView: View {
    @State private var viewModel = PacientesViewModel()

    private var subtitulo: String? {
        guard viewModel.carga.valor != nil else { return nil }
        let total = viewModel.pacientes.count
        if !viewModel.texto.isEmpty { return "\(total) \(total == 1 ? "resultado" : "resultados")" }
        return "\(total) \(total == 1 ? "paciente" : "pacientes") a tu cargo"
    }

    private var filtros: some View {
        HStack(spacing: 8) {
            ForEach(PacientesViewModel.Filtro.allCases) { filtro in
                let elegido = viewModel.filtro == filtro
                Button {
                    viewModel.filtro = filtro
                } label: {
                    Text("\(filtro.rawValue) \(viewModel.cuantos(filtro))")
                        .font(.footnote.weight(.semibold))
                        .padding(.horizontal, 12)
                        .frame(minHeight: 34)
                        .foregroundStyle(elegido ? Color.cmoSobreMarca : Color.cmoTinta)
                        .background(elegido ? Color.cmoMarca : Color.cmoSuperficie, in: .capsule)
                        .overlay(Capsule().stroke(elegido ? Color.cmoMarca : Color.cmoLinea))
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(elegido ? .isSelected : [])
                .accessibilityIdentifier("pacientes.filtro.\(filtro.rawValue)")
            }
            Spacer(minLength: 0)
        }
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                EncabezadoPantalla("Mis pacientes", subtitulo: subtitulo) { CampanaAvisos() }
                    .padding(.horizontal, 16)

                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass").foregroundStyle(Color.cmoTintaSuave)
                    TextField("Nombre, DUI o número de expediente", text: $viewModel.texto)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .accessibilityIdentifier("pacientes.buscar")
                    if !viewModel.texto.isEmpty {
                        Button { viewModel.texto = "" } label: {
                            Image(systemName: "xmark.circle.fill").foregroundStyle(Color.cmoTintaSuave)
                        }
                        .accessibilityLabel("Borrar búsqueda")
                    }
                }
                .padding(.horizontal, 12)
                .frame(minHeight: 46)
                .background(Color.cmoSuperficie, in: .rect(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.cmoLinea))
                .padding(.horizontal, 16)

                filtros
                    .padding(.horizontal, 16)

                EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.buscar(completo: true) } }) { _ in
                    let pacientes = viewModel.pacientes
                    ScrollView {
                        LazyVStack(spacing: 10) {
                            if pacientes.isEmpty {
                                InicioVacio(
                                    icono: "person.2",
                                    titulo: viewModel.texto.isEmpty && viewModel.filtro == .todos ? "Aún no tienes pacientes" : "Sin resultados",
                                    detalle: viewModel.texto.isEmpty && viewModel.filtro == .todos
                                        ? "Aquí aparecen los pacientes con los que tienes una cita activa o un ingreso a tu cargo."
                                        : "Prueba con otro filtro, nombre, DUI o número de expediente."
                                )
                            }
                            ForEach(pacientes) { paciente in
                                NavigationLink(value: paciente) {
                                    HStack(spacing: 12) {
                                        AvatarIniciales(nombre: paciente.nombreCompleto, tono: .info)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(paciente.nombreCompleto)
                                                .font(.subheadline.weight(.semibold))
                                                .foregroundStyle(Color.cmoTinta)
                                            Text([paciente.numeroExpediente, paciente.dui].compactMap { $0 }.joined(separator: " · "))
                                                .font(.caption)
                                                .foregroundStyle(Color.cmoTintaSuave)
                                            if let fecha = viewModel.ultimaConsulta(de: paciente) {
                                                Text("Última consulta: \(Formato.fechaCorta(fecha))")
                                                    .font(.caption2)
                                                    .foregroundStyle(Color.cmoTintaSuave)
                                            }
                                        }
                                        Spacer(minLength: 8)
                                        if viewModel.estaIngresado(paciente) {
                                            EstadoChip(texto: "Ingresado", tono: .info)
                                        }
                                        Image(systemName: "chevron.right")
                                            .font(.footnote.weight(.semibold))
                                            .foregroundStyle(Color.cmoTintaSuave)
                                    }
                                    .tarjeta()
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                    .refreshable { await viewModel.buscar(completo: true) }
                    .accessibilityIdentifier("pacientes.lista")
                }
            }
            .padding(.top, 8)
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Paciente.self) { paciente in
                PacienteDetalleView(paciente: paciente)
            }
            .task(id: viewModel.texto) {
                // Espera a que termine de escribir antes de buscar.
                if viewModel.carga.valor != nil {
                    try? await Task.sleep(for: .milliseconds(350))
                    if Task.isCancelled { return }
                }
                await viewModel.buscar()
            }
        }
    }
}

/// Ficha del paciente y acceso a sus acciones.
struct PacienteDetalleView: View {
    @Environment(SessionStore.self) private var store
    let paciente: Paciente
    @State private var seguimiento: Paciente?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    AvatarIniciales(nombre: paciente.nombreCompleto, tono: .info, tamano: 52)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(paciente.nombreCompleto)
                            .font(.headline)
                            .foregroundStyle(Color.cmoTinta)
                        Text(paciente.numeroExpediente)
                            .font(.caption)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                    Spacer(minLength: 0)
                }
                .tarjeta()

                grupo("Datos personales") {
                    dato("DUI", paciente.dui)
                    dato("Nacimiento", Formato.fechaCorta(paciente.fechaNacimiento))
                    dato("Sexo", paciente.sexoTexto)
                    dato("Teléfono", paciente.telefono)
                    dato("Dirección", paciente.direccion)
                }
                grupo("Datos clínicos") {
                    dato("Tipo de sangre", paciente.tipoSangre)
                    dato("Alergias", paciente.alergias)
                }

                BotonPrincipal(titulo: "Agendar seguimiento") {
                    seguimiento = paciente
                }
                .accessibilityIdentifier("paciente.seguimiento")

                NavigationLink {
                    ExpedienteView(paciente: paciente)
                } label: {
                    HStack(spacing: 12) {
                        IconoCirculo(sistema: "folder", tono: .info, tamano: 36)
                        Text("Expediente clínico")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.cmoTinta)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                    .tarjeta(relleno: 12)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("paciente.expediente")
            }
            .padding(16)
        }
        .background(Color.cmoFondo)
        .navigationTitle(paciente.nombreCompleto)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $seguimiento) { paciente in
            SeguimientoView(paciente: paciente, idMedico: store.sesion?.usuario.id ?? "", alAgendar: {})
        }
    }

    private func grupo<Contenido: View>(_ titulo: String, @ViewBuilder _ contenido: () -> Contenido) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(titulo).font(.headline).foregroundStyle(Color.cmoTinta)
            VStack(spacing: 0) { contenido() }.tarjeta(relleno: 0)
        }
    }

    private func dato(_ titulo: String, _ valor: String?) -> some View {
        HStack(alignment: .top) {
            Text(titulo).foregroundStyle(Color.cmoTintaSuave)
            Spacer(minLength: 16)
            Text(valor ?? "—").foregroundStyle(Color.cmoTinta).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .padding(14)
    }
}
