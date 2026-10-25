//
//  AgendaView.swift
//  appcmo
//

import SwiftUI

/// Agenda del médico (HU-36): tira de la semana, y las citas del día elegido con paciente, motivo y estado.
struct AgendaView: View {
    @State private var viewModel = AgendaViewModel()

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                EncabezadoPantalla("Agenda", subtitulo: viewModel.mes) {
                    HStack(spacing: 6) {
                        botonSemana("chevron.left", "Semana anterior") { viewModel.moverSemana(-1) }
                        botonSemana("chevron.right", "Semana siguiente") { viewModel.moverSemana(1) }
                        CampanaAvisos().padding(.leading, 4)
                    }
                }
                .padding(.horizontal, 16)

                semana
                    .padding(.horizontal, 16)

                EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { _ in
                    lista
                }
            }
            .padding(.top, 8)
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Cita.self) { cita in
                CitaDetalleView(cita: cita)
            }
            // Al volver de una cita o de una consulta, el estado pudo haber cambiado.
            .onAppear { Task { await viewModel.cargar() } }
        }
    }

    private func botonSemana(_ sistema: String, _ etiqueta: String, accion: @escaping () -> Void) -> some View {
        Button(action: accion) {
            Image(systemName: sistema)
                .font(.subheadline.weight(.semibold))
                .frame(width: 36, height: 36)
                .background(Color.cmoSuperficie, in: .circle)
                .overlay(Circle().stroke(Color.cmoLinea))
        }
        .accessibilityLabel(etiqueta)
    }

    private var semana: some View {
        HStack(spacing: 8) {
            ForEach(viewModel.diasDeLaSemana, id: \.self) { dia in
                let calendario = Formato.calendario
                let elegido = calendario.isDate(dia, inSameDayAs: viewModel.dia)
                let esHoy = calendario.isDateInToday(dia)
                Button {
                    viewModel.dia = dia
                } label: {
                    VStack(spacing: 4) {
                        Text(inicialDelDia(dia))
                            .font(.caption2.weight(.semibold))
                        Text("\(calendario.component(.day, from: dia))")
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity, minHeight: 56)
                    .foregroundStyle(elegido ? Color.cmoSobreMarca : Color.cmoTinta)
                    .background(elegido ? Color.cmoMarca : Color.cmoSuperficie, in: .rect(cornerRadius: 14))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(esHoy && !elegido ? Color.cmoMarca : Color.cmoLinea))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Formato.fechaLarga(dia) + (esHoy ? ", hoy" : ""))
                .accessibilityAddTraits(elegido ? .isSelected : [])
            }
        }
    }

    private func inicialDelDia(_ dia: Date) -> String {
        let letras = ["D", "L", "M", "X", "J", "V", "S"]
        return letras[Formato.calendario.component(.weekday, from: dia) - 1]
    }

    private var lista: some View {
        let citas = viewModel.citasDelDia
        return ScrollView {
            LazyVStack(alignment: .leading, spacing: 10) {
                Text("\(Formato.fechaLarga(viewModel.dia)) · \(citas.count) \(citas.count == 1 ? "cita" : "citas")")
                    .font(.headline)
                    .foregroundStyle(Color.cmoTinta)
                    .padding(.top, 2)
                if citas.isEmpty {
                    InicioVacio(
                        icono: "calendar.badge.checkmark",
                        titulo: "Sin citas este día",
                        detalle: "Tus citas agendadas para este día aparecerán aquí."
                    )
                }
                ForEach(citas) { cita in
                    NavigationLink(value: cita) {
                        CitaTarjeta(cita: cita, icono: .reloj)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .refreshable { await viewModel.cargar() }
        .accessibilityIdentifier("agenda.lista")
    }
}
