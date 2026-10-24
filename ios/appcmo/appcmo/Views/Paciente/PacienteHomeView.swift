//
//  PacienteHomeView.swift
//  appcmo
//

import SwiftUI

/// Inicio del rol PACIENTE. Aquí irán la información pública, mis citas, mi expediente y mi habitación.
struct PacienteHomeView: View {
    let sesion: Sesion

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    PerfilCard(usuario: sesion.usuario, icono: "person.fill")
                    InicioVacio(
                        icono: "heart.text.square",
                        titulo: "Tu información aparecerá aquí",
                        detalle: "Pronto podrás ver tus citas, tu expediente y tus resultados de laboratorio."
                    )
                }
                .padding(20)
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
            }
            .background(Color.cmoFondo)
            .navigationTitle("Inicio")
            .cerrarSesionToolbar()
        }
        .accessibilityIdentifier("inicio.paciente")
    }
}
