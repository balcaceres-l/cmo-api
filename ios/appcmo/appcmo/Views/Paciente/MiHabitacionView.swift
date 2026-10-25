//
//  MiHabitacionView.swift
//  appcmo
//

import SwiftUI

/// Mi habitación y cama (pantalla 09, HU-54): salen de mi expediente. Nunca aparece nadie más de la habitación.
struct MiHabitacionView: View {
    @Environment(PacienteDatos.self) private var datos

    var body: some View {
        EstadoCarga(carga: datos.expediente, reintentar: { Task { await datos.cargarExpediente() } }) { _ in
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    if let ingreso = datos.ingresoActual, let cama = ingreso.cama {
                        VStack(alignment: .leading, spacing: 6) {
                            Text([cama.habitacion?.planta.map { "Planta \($0)" }, cama.habitacion?.tipoNombre].compactMap { $0 }.joined(separator: " · "))
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(Color.cmoSobreMarca.opacity(0.85))
                            Text(cama.codigoHabitacion)
                                .font(.largeTitle.bold())
                                .foregroundStyle(Color.cmoSobreMarca)
                            Text(cama.nombreCama)
                                .font(.headline)
                                .foregroundStyle(Color.cmoSobreMarca)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(18)
                        .background(Color.cmoMarca, in: .rect(cornerRadius: 18))
                        .accessibilityElement(children: .combine)
                        .accessibilityIdentifier("habitacion.tarjeta")

                        VStack(spacing: 0) {
                            FilaDato("Ingresado desde", Formato.fechaDeMarca(ingreso.fechaIngreso))
                            Divider()
                            FilaDato("Médico tratante", ingreso.medico?.nombreCompleto)
                            Divider()
                            FilaDato("Motivo", ingreso.motivo)
                        }
                        .tarjeta(relleno: 0)
                    } else {
                        InicioVacio(icono: "bed.double", titulo: "No estás hospitalizado", detalle: "Si te ingresan, aquí verás tu habitación y tu cama.")
                            .accessibilityIdentifier("habitacion.vacia")
                    }
                }
                .padding(16)
            }
            .refreshable { await datos.cargarExpediente() }
        }
        .background(Color.cmoFondo)
        .navigationTitle("Mi habitación")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .onAppear { Task { await datos.cargarExpediente() } }
    }
}
