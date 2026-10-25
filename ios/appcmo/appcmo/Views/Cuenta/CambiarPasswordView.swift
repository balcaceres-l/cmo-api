//
//  CambiarPasswordView.swift
//  appcmo
//

import SwiftUI

struct CambiarPasswordView: View {
    @State private var viewModel: CambiarPasswordViewModel

    init(store: SessionStore) {
        _viewModel = State(initialValue: CambiarPasswordViewModel(store: store))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                AvisoBanner(aviso: .informacion("Al cambiar la contraseña se cerrará tu sesión en todos tus dispositivos y tendrás que entrar de nuevo."))

                if let aviso = viewModel.aviso {
                    AvisoBanner(aviso: aviso)
                }

                CampoFormulario(titulo: "Contraseña actual", error: viewModel.errorActual) {
                    SecureField("Contraseña actual", text: $viewModel.actual)
                        .textContentType(.password)
                        .accessibilityIdentifier("password.actual")
                }
                CampoFormulario(titulo: "Nueva contraseña", error: viewModel.errorNueva) {
                    SecureField("Mínimo 8 caracteres", text: $viewModel.nueva)
                        .textContentType(.newPassword)
                        .accessibilityIdentifier("password.nueva")
                }
                CampoFormulario(titulo: "Confirmar nueva contraseña", error: viewModel.errorConfirmacion) {
                    SecureField("Repite la nueva contraseña", text: $viewModel.confirmacion)
                        .textContentType(.newPassword)
                        .accessibilityIdentifier("password.confirmacion")
                }

                BotonPrincipal(titulo: "Cambiar contraseña", cargando: viewModel.enviando, deshabilitado: !viewModel.puedeEnviar) {
                    Task { await viewModel.cambiar() }
                }
                .accessibilityIdentifier("password.cambiar")
            }
            .padding(20)
            .frame(maxWidth: 520)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.cmoFondo)
        .navigationTitle("Cambiar contraseña")
        .navigationBarTitleDisplayMode(.inline)
    }
}
