//
//  MedicoHomeView.swift
//  appcmo
//

import SwiftUI

/// Inicio del rol MEDICO. Aquí irán la agenda del día, mis pacientes y laboratorio.
struct MedicoHomeView: View {
    let sesion: Sesion

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    PerfilCard(usuario: sesion.usuario, icono: "stethoscope")
                    InicioVacio(
                        icono: "calendar",
                        titulo: "Tu agenda aparecerá aquí",
                        detalle: "Pronto verás tus citas del día, tus pacientes y tus órdenes de laboratorio."
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
        .accessibilityIdentifier("inicio.medico")
    }
}
