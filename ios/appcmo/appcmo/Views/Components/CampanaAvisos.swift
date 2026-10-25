//
//  CampanaAvisos.swift
//  appcmo
//

import SwiftUI

/// Campana con el número de avisos sin ver. Está en el encabezado de las pantallas principales y abre la lista de avisos.
struct CampanaAvisos: View {
    @Environment(AvisosViewModel.self) private var avisos
    @State private var abierta = false

    var body: some View {
        Button {
            abierta = true
        } label: {
            Image(systemName: "bell")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Color.cmoTinta)
                .frame(width: 40, height: 40)
                .background(Color.cmoSuperficie, in: .circle)
                .overlay(Circle().stroke(Color.cmoLinea))
                .overlay(alignment: .topTrailing) {
                    if avisos.sinLeer > 0 {
                        Text("\(min(avisos.sinLeer, 9))")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(minWidth: 18, minHeight: 18)
                            .background(Color.cmoError, in: .circle)
                            .offset(x: 4, y: -4)
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(avisos.sinLeer > 0 ? "Avisos, \(avisos.sinLeer) sin ver" : "Avisos")
        .accessibilityIdentifier("campana.avisos")
        .sheet(isPresented: $abierta) {
            AvisosView()
        }
    }
}

/// Lista de avisos vigentes. Los que no se habían visto salen con la etiqueta "Nuevo" mientras está abierta.
struct AvisosView: View {
    @Environment(\.dismiss) private var cerrar
    @Environment(AvisosViewModel.self) private var avisos
    @State private var nuevos: Set<String> = []

    var body: some View {
        NavigationStack {
            EstadoCarga(carga: avisos.carga, reintentar: { Task { await avisos.cargar() } }) { lista in
                ScrollView {
                    LazyVStack(spacing: 10) {
                        if lista.isEmpty {
                            InicioVacio(icono: "bell", titulo: "Sin avisos", detalle: "Cuando el hospital publique un aviso aparecerá aquí.")
                        }
                        ForEach(lista) { aviso in
                            AvisoTarjeta(aviso: aviso, nuevo: nuevos.contains(aviso.id))
                        }
                    }
                    .padding(16)
                }
                .refreshable { await avisos.cargar() }
            }
            .background(Color.cmoFondo)
            .navigationTitle("Avisos")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { cerrar() } }
            }
            .task {
                await avisos.cargar()
                nuevos = Set((avisos.carga.valor ?? []).filter { avisos.esNuevo($0) }.map(\.id))
                avisos.marcarVistos()
            }
        }
        .presentationDetents([.large])
    }
}
