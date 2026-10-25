//
//  AmbulanciaView.swift
//  appcmo
//

import SwiftUI

/// Estado de la ambulancia y del traslado en curso (pantalla 10). Se actualiza sola cada 10 segundos.
/// El reporte clínico no incluye el nombre ni la dirección de la persona atendida.
struct AmbulanciaView: View {
    @State private var viewModel = AmbulanciaViewModel()

    var body: some View {
        EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { ambulancia in
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    estado(ambulancia)
                    if let servicio = ambulancia.servicioActivo {
                        traslado(servicio)
                    }
                    if let fecha = viewModel.ultimaActualizacion {
                        Text("Actualizado a las \(horaDe(fecha))")
                            .font(.caption)
                            .foregroundStyle(Color.cmoTintaSuave)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(16)
            }
            .refreshable { await viewModel.cargar() }
        }
        .background(Color.cmoFondo)
        .navigationTitle("Ambulancia")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.seguir() }
    }

    private func horaDe(_ fecha: Date) -> String {
        let formateador = DateFormatter()
        formateador.timeZone = Formato.zona
        formateador.dateFormat = "HH:mm:ss"
        return formateador.string(from: fecha)
    }

    private func estado(_ ambulancia: AmbulanciaMedico) -> some View {
        HStack(spacing: 14) {
            IconoCirculo(sistema: "cross.case.fill", tono: ambulancia.estado.tono, tamano: 56)
            VStack(alignment: .leading, spacing: 4) {
                Text("Ambulancia")
                    .font(.subheadline)
                    .foregroundStyle(Color.cmoTintaSuave)
                EstadoChip(texto: ambulancia.estado.nombre, tono: ambulancia.estado.tono)
                    .accessibilityIdentifier("ambulancia.estado")
            }
            Spacer(minLength: 0)
        }
        .tarjeta()
    }

    private func traslado(_ servicio: ServicioEnCurso) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Traslado en curso").font(.headline).foregroundStyle(Color.cmoTinta)
                Spacer()
                EstadoChip(texto: "Prioridad \(servicio.prioridad.nombre)", tono: servicio.prioridad.tono)
            }
            VStack(spacing: 0) {
                fila("Salida", servicio.horaSalida.map { Formato.marcaTiempo($0) })
                if let reporte = servicio.ultimoReporte {
                    Divider()
                    HStack {
                        Text("Estado de la persona").foregroundStyle(Color.cmoTintaSuave)
                        Spacer()
                        EstadoChip(texto: reporte.estadoPaciente.nombre, tono: reporte.estadoPaciente.tono)
                    }
                    .font(.subheadline)
                    .padding(14)
                    Divider()
                    fila("Llegada estimada", reporte.llegadaEstimada.map { Formato.marcaTiempo($0) })
                    if let observaciones = reporte.observaciones, !observaciones.isEmpty {
                        Divider()
                        fila("Observaciones", observaciones)
                    }
                } else {
                    Divider()
                    fila("Reporte", "Aún sin reporte clínico")
                }
            }
            .tarjeta(relleno: 0)
        }
        .accessibilityIdentifier("ambulancia.traslado")
    }

    private func fila(_ titulo: String, _ valor: String?) -> some View {
        HStack(alignment: .top) {
            Text(titulo).foregroundStyle(Color.cmoTintaSuave)
            Spacer(minLength: 16)
            Text(valor ?? "—").foregroundStyle(Color.cmoTinta).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .padding(14)
    }
}
