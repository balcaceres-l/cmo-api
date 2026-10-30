//
//  IngresadosView.swift
//  appcmo
//

import SwiftUI

/// Pacientes ingresados (pantalla 09). "Míos" son los que están a cargo del médico; "Todos" es el mapa de camas,
/// donde solo se identifica a los pacientes propios (los demás salen como "Ocupada", por privacidad).
struct IngresadosView: View {
    @State private var viewModel = IngresadosViewModel()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Picker("Vista", selection: $viewModel.vista) {
                ForEach(IngresadosViewModel.Vista.allCases) { vista in
                    Text(vista.rawValue).tag(vista)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .accessibilityIdentifier("ingresados.vista")

            switch viewModel.vista {
            case .mios:
                EstadoCarga(carga: viewModel.mios, reintentar: { Task { await viewModel.cargar() } }) { ingresos in
                    mios(ingresos)
                }
            case .todos:
                EstadoCarga(carga: viewModel.mapa, reintentar: { Task { await viewModel.cargar() } }) { mapa in
                    todos(mapa)
                }
            }
        }
        .padding(.top, 8)
        .background(Color.cmoFondo)
        .navigationTitle("Pacientes ingresados")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.cargar() }
    }

    // MARK: - Míos

    private func mios(_ ingresos: [Ingreso]) -> some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                if ingresos.isEmpty {
                    InicioVacio(
                        icono: "bed.double",
                        titulo: "Sin pacientes ingresados",
                        detalle: "Los pacientes hospitalizados a tu cargo aparecerán aquí."
                    )
                }
                ForEach(ingresos) { ingreso in
                    fila(ingreso)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .refreshable { await viewModel.cargar() }
        .accessibilityIdentifier("ingresados.mios")
    }

    @ViewBuilder
    private func fila(_ ingreso: Ingreso) -> some View {
        let contenido = HStack(spacing: 12) {
            AvatarIniciales(nombre: ingreso.paciente?.nombreCompleto ?? "P", tono: .info)
            VStack(alignment: .leading, spacing: 2) {
                Text(ingreso.paciente?.nombreCompleto ?? "Paciente")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                Text([ingreso.cama?.ubicacion, ingreso.motivo].compactMap { $0 }.joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(Color.cmoTintaSuave)
                    .lineLimit(2)
                Text("Ingresó el \(Formato.marcaTiempo(ingreso.fechaIngreso))")
                    .font(.caption2)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.cmoTintaSuave)
        }
        .tarjeta()
        if let paciente = ingreso.paciente {
            NavigationLink {
                ExpedienteView(paciente: paciente)
            } label: { contenido }
                .buttonStyle(.plain)
        } else {
            contenido
        }
    }

    // MARK: - Todos

    private func todos(_ mapa: MapaHabitaciones) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 10) {
                    resumen("\(viewModel.camasLibres)", "Camas libres", .exito)
                    resumen("\(viewModel.camasTotales - viewModel.camasLibres)", "Ocupadas", .advertencia)
                }
                ForEach(mapa.plantas) { planta in
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Planta \(planta.planta)")
                            .font(.headline)
                            .foregroundStyle(Color.cmoTinta)
                        ForEach(planta.habitaciones, id: \.codigo) { habitacion in
                            tarjeta(habitacion)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .refreshable { await viewModel.cargar() }
        .accessibilityIdentifier("ingresados.todos")
    }

    private func resumen(_ valor: String, _ titulo: String, _ tono: TonoEstado) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(valor).font(.title2.bold())
            Text(titulo).font(.caption)
        }
        .foregroundStyle(tono.primerPlano)
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tono.fondo, in: .rect(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }

    private func tarjeta(_ habitacion: HabitacionMapa) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(habitacion.codigo)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                Spacer()
                EstadoChip(texto: habitacion.tipo == "VIP" ? "VIP" : "Semiprivada", tono: .neutro)
            }
            ForEach(habitacion.camas, id: \.codigo) { cama in
                HStack(spacing: 10) {
                    Image(systemName: "bed.double")
                        .foregroundStyle(cama.ocupada ? Color.cmoAdvertencia : Color.cmoExito)
                        .accessibilityHidden(true)
                    Text(cama.codigo)
                        .font(.footnote)
                        .foregroundStyle(Color.cmoTintaSuave)
                    Spacer(minLength: 8)
                    if let paciente = cama.paciente {
                        Text(paciente.nombreCompleto)
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Color.cmoTinta)
                    } else {
                        EstadoChip(texto: cama.ocupada ? "Ocupada" : "Libre", tono: cama.ocupada ? .advertencia : .exito)
                    }
                }
            }
        }
        .tarjeta()
    }
}
