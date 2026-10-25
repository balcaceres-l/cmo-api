//
//  TurnosView.swift
//  appcmo
//

import SwiftUI

/// Mis turnos de la semana, con el turno actual resaltado (HU-66).
struct TurnosView: View {
    @State private var viewModel = TurnosViewModel()

    var body: some View {
        EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { _ in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    cabecera
                    let turnos = viewModel.turnosDeLaSemana
                    if turnos.isEmpty {
                        InicioVacio(
                            icono: "clock",
                            titulo: "Sin turnos esta semana",
                            detalle: "Cuando administración te asigne turnos aparecerán aquí."
                        )
                    }
                    ForEach(turnos) { turno in
                        tarjeta(turno)
                    }
                }
                .padding(16)
            }
            .refreshable { await viewModel.cargar() }
        }
        .background(Color.cmoFondo)
        .navigationTitle("Mis turnos")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { Task { await viewModel.cargar() } }
    }

    private var cabecera: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text(Formato.mesYAnio(viewModel.inicioSemana))
                    .font(.subheadline)
                    .foregroundStyle(Color.cmoTintaSuave)
                Text(viewModel.titulo)
                    .font(.headline)
                    .foregroundStyle(Color.cmoTinta)
                if !viewModel.esSemanaActual {
                    Button("Ir a esta semana") { viewModel.irASemanaActual() }
                        .font(.footnote.weight(.semibold))
                }
            }
            Spacer()
            HStack(spacing: 6) {
                boton("chevron.left", "Semana anterior") { viewModel.moverSemana(-1) }
                boton("chevron.right", "Semana siguiente") { viewModel.moverSemana(1) }
            }
        }
    }

    private func boton(_ sistema: String, _ etiqueta: String, accion: @escaping () -> Void) -> some View {
        Button(action: accion) {
            Image(systemName: sistema)
                .font(.subheadline.weight(.semibold))
                .frame(width: 36, height: 36)
                .background(Color.cmoSuperficie, in: .circle)
                .overlay(Circle().stroke(Color.cmoLinea))
        }
        .accessibilityLabel(etiqueta)
    }

    private func tarjeta(_ turno: TurnoPersonal) -> some View {
        let etiqueta = viewModel.etiqueta(turno)
        let actual = viewModel.esActual(turno)
        return HStack(spacing: 12) {
            IconoCirculo(sistema: "calendar", tono: etiqueta.tono)
            VStack(alignment: .leading, spacing: 2) {
                Text("\(Formato.diaCorto(turno.fecha)) · \(Formato.hora(turno.horaInicio)) a \(Formato.hora(turno.horaFin))")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Text(turno.area)
                    .font(.caption)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            Spacer(minLength: 8)
            EstadoChip(texto: etiqueta.texto, tono: etiqueta.tono)
        }
        .tarjeta()
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(actual ? Color.cmoMarca : .clear, lineWidth: 1.5))
        .accessibilityIdentifier(actual ? "turno.actual" : "turno.fila")
    }
}
