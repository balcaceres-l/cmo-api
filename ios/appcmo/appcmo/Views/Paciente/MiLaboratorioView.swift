//
//  MiLaboratorioView.swift
//  appcmo
//

import SwiftUI

/// Mi laboratorio (pantalla 08, HU-48): si mis exámenes están pendientes, en proceso o completados.
struct MiLaboratorioView: View {
    @Environment(PacienteDatos.self) private var datos
    @State private var filtro: EstadoOrden?

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                EncabezadoPantalla("Mi laboratorio", subtitulo: "Estado de tus exámenes") {
                    CampanaAvisos()
                }
                .padding(.horizontal, 16)

                EstadoCarga(carga: datos.ordenes, reintentar: { Task { await datos.cargarOrdenes() } }) { ordenes in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                ForEach(EstadoOrden.filtrables, id: \.self) { resumen($0) }
                            }

                            let visibles = ordenes
                                .filter { filtro == nil || $0.estado == filtro }
                                .sorted { ($0.fechaSolicitud ?? "") > ($1.fechaSolicitud ?? "") }
                            if visibles.isEmpty {
                                InicioVacio(
                                    icono: "testtube.2",
                                    titulo: filtro == nil ? "Aún no tienes exámenes" : "Sin exámenes en este estado",
                                    detalle: "Cuando tu médico pida un examen aparecerá aquí con su estado."
                                )
                            }
                            ForEach(visibles) { fila($0) }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                    .refreshable { await datos.cargarOrdenes() }
                }
            }
            .padding(.top, 8)
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .onAppear { Task { await datos.cargarOrdenes() } }
        }
    }

    private func resumen(_ estado: EstadoOrden) -> some View {
        let tono = estado.tono
        let elegido = filtro == estado
        return Button {
            filtro = elegido ? nil : estado
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(datos.cuantas(estado))").font(.title2.bold())
                Text(estado.nombre).font(.caption).lineLimit(1).minimumScaleFactor(0.7)
            }
            .foregroundStyle(tono.primerPlano)
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(tono.fondo, in: .rect(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(elegido ? tono.primerPlano : .clear, lineWidth: 1.5))
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(estado.nombre): \(datos.cuantas(estado))")
        .accessibilityAddTraits(elegido ? .isSelected : [])
        .accessibilityHint("Filtra la lista por este estado")
    }

    private func fila(_ orden: OrdenLaboratorio) -> some View {
        var fechas = ["Solicitado \(Formato.fechaCortaDeMarca(orden.fechaSolicitud))"]
        if orden.estado == .enProceso || orden.estado == .completado, orden.fechaInicio != nil {
            fechas.append("Inicio \(Formato.fechaCortaDeMarca(orden.fechaInicio))")
        }
        if let resultado = orden.fechaResultado { fechas.append("Resultado \(Formato.fechaCortaDeMarca(resultado))") }
        if let cancelada = orden.fechaCancelacion { fechas.append("Cancelado \(Formato.fechaCortaDeMarca(cancelada))") }
        return HStack(spacing: 12) {
            IconoCirculo(sistema: "testtube.2", tono: orden.estado.tono, tamano: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(orden.examen?.nombre ?? "Examen").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                Text(fechas.joined(separator: " · ")).font(.caption).foregroundStyle(Color.cmoTintaSuave)
            }
            Spacer(minLength: 8)
            EstadoChip(texto: orden.estado.nombre, tono: orden.estado.tono)
        }
        .tarjeta(relleno: 12)
        .opacity(orden.estado == .cancelada ? 0.6 : 1)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("laboratorio.fila")
    }
}
