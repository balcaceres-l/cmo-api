//
//  FilaDato.swift
//  appcmo
//

import SwiftUI

/// Una fila "etiqueta ... valor" para las tarjetas de datos (se usa dentro de `.tarjeta(relleno: 0)`).
struct FilaDato: View {
    let titulo: String
    let valor: String?

    init(_ titulo: String, _ valor: String?) {
        self.titulo = titulo
        self.valor = valor
    }

    var body: some View {
        HStack(alignment: .top) {
            Text(titulo).foregroundStyle(Color.cmoTintaSuave)
            Spacer(minLength: 16)
            Text(valor.flatMap { $0.isEmpty ? nil : $0 } ?? "—")
                .foregroundStyle(Color.cmoTinta)
                .multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .padding(14)
    }
}

/// "Buscar por fecha": un selector de día para acotar una lista; "Quitar filtro" vuelve a mostrarlo todo.
struct FiltroPorFecha: View {
    @Binding var fecha: Date?
    /// Día en que arranca el selector al abrirlo.
    let inicial: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Spacer()
                Button {
                    fecha = fecha == nil ? inicial : nil
                } label: {
                    Label(fecha == nil ? "Buscar por fecha" : "Quitar filtro", systemImage: fecha == nil ? "calendar" : "xmark.circle")
                }
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier("filtro.buscarFecha")
            }
            if fecha != nil {
                DatePicker(
                    "Fecha",
                    selection: Binding(get: { fecha ?? inicial }, set: { fecha = $0 }),
                    displayedComponents: .date
                )
                .environment(\.locale, Locale(identifier: "es"))
                .environment(\.timeZone, Formato.zona)
                .font(.subheadline)
                .tarjeta(relleno: 12)
                .accessibilityIdentifier("filtro.fecha")
            }
        }
    }
}
