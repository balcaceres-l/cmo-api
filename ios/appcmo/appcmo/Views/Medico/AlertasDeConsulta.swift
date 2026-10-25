//
//  AlertasDeConsulta.swift
//  appcmo
//

import SwiftUI

/// Navegación a la consulta y avisos que comparten el Inicio y el detalle de una cita:
/// ir a la consulta, "ya tienes una consulta en atención" y errores.
private struct AlertasDeConsulta: ViewModifier {
    @Binding var destino: String?
    @Binding var enAtencion: Cita?
    @Binding var error: String?
    let idUsuario: String
    let continuar: (Cita) -> Void

    func body(content: Content) -> some View {
        content
            .navigationDestination(item: $destino) { id in
                ConsultaView(idConsulta: id, idUsuario: idUsuario)
            }
            .alert(
                "Ya tienes una consulta en atención",
                isPresented: Binding(get: { enAtencion != nil }, set: { if !$0 { enAtencion = nil } }),
                presenting: enAtencion
            ) { otra in
                Button("Continuar esa consulta") { continuar(otra) }
                Button("Cancelar", role: .cancel) {}
            } message: { otra in
                Text("Estás atendiendo a \(otra.paciente?.nombreCompleto ?? "un paciente") (cita de las \(Formato.hora(otra.horaInicio))). Finalízala antes de iniciar otra consulta.")
            }
            .alert("No se pudo continuar", isPresented: Binding(get: { error != nil }, set: { if !$0 { error = nil } })) {
                Button("Entendido", role: .cancel) {}
            } message: {
                Text(error ?? "")
            }
    }
}

extension View {
    func alertasDeConsulta(
        destino: Binding<String?>,
        enAtencion: Binding<Cita?>,
        error: Binding<String?>,
        idUsuario: String,
        continuar: @escaping (Cita) -> Void
    ) -> some View {
        modifier(AlertasDeConsulta(destino: destino, enAtencion: enAtencion, error: error, idUsuario: idUsuario, continuar: continuar))
    }
}
