//
//  LaboratorioView.swift
//  appcmo
//

import SwiftUI

/// Órdenes de laboratorio que solicitó el médico, con su estado y fechas (HU-47).
/// Las tres tarjetas de arriba cuentan por estado y sirven de filtro.
struct LaboratorioView: View {
    @State private var viewModel = LaboratorioViewModel()

    private func tono(_ estado: EstadoOrden) -> EstadoChip.Tono { estado.tono }

    var body: some View {
        EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { _ in
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Exámenes que pediste")
                        .font(.subheadline)
                        .foregroundStyle(Color.cmoTintaSuave)

                    HStack(spacing: 10) {
                        ForEach(EstadoOrden.allCases, id: \.self) { estado in
                            resumen(estado)
                        }
                    }

                    let ordenes = viewModel.ordenes
                    if ordenes.isEmpty {
                        InicioVacio(
                            icono: "testtube.2",
                            titulo: "Sin órdenes",
                            detalle: "Los exámenes que pidas al finalizar una consulta aparecerán aquí con su estado."
                        )
                    }
                    ForEach(ordenes) { orden in
                        OrdenFila(orden: orden, mostrarPaciente: true)
                            .tarjeta()
                    }
                }
                .padding(16)
            }
            .refreshable { await viewModel.cargar() }
        }
        .background(Color.cmoFondo)
        .navigationTitle("Laboratorio")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { Task { await viewModel.cargar() } }
    }

    private func resumen(_ estado: EstadoOrden) -> some View {
        let tono = estado.tono
        let elegido = viewModel.filtro == estado
        return Button {
            viewModel.alternar(estado)
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(viewModel.cuantas(estado))")
                    .font(.title2.bold())
                Text(estado.nombre)
                    .font(.caption)
            }
            .foregroundStyle(tono.primerPlano)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(tono.fondo, in: .rect(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(elegido ? tono.primerPlano : .clear, lineWidth: 1.5))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(estado.nombre): \(viewModel.cuantas(estado))")
        .accessibilityAddTraits(elegido ? .isSelected : [])
        .accessibilityHint("Filtra la lista por este estado")
    }
}

/// Una orden de laboratorio: examen, paciente, estado y fechas.
struct OrdenFila: View {
    let orden: OrdenLaboratorio
    var mostrarPaciente = false

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            IconoCirculo(sistema: "testtube.2", tono: orden.estado.tono)
            VStack(alignment: .leading, spacing: 3) {
                HStack(alignment: .firstTextBaseline) {
                    Text(orden.examen?.nombre ?? "Examen")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.cmoTinta)
                    Spacer(minLength: 8)
                    EstadoChip(texto: orden.estado.nombre, tono: orden.estado.tono)
                }
                if mostrarPaciente, let paciente = orden.paciente {
                    Text(paciente.nombreCompleto)
                        .font(.caption)
                        .foregroundStyle(Color.cmoTinta)
                }
                if let observaciones = orden.observaciones, !observaciones.isEmpty {
                    Text(observaciones).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                }
                Group {
                    Text("Solicitada: \(Formato.marcaTiempo(orden.fechaSolicitud))")
                    if let inicio = orden.fechaInicio { Text("En proceso desde: \(Formato.marcaTiempo(inicio))") }
                    if let resultado = orden.fechaResultado { Text("Completada: \(Formato.marcaTiempo(resultado))") }
                }
                .font(.caption2)
                .foregroundStyle(Color.cmoTintaSuave)
            }
        }
    }
}
