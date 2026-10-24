//
//  LoginView.swift
//  appcmo
//

import SwiftUI

struct LoginView: View {
    private enum Campo: Hashable {
        case usuario, password
    }

    @State private var viewModel: LoginViewModel
    @State private var verPassword = false
    @FocusState private var foco: Campo?

    init(store: SessionStore) {
        _viewModel = State(initialValue: LoginViewModel(store: store))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 28) {
                encabezado
                formulario
            }
            .frame(maxWidth: 440)
            .padding(.horizontal, 20)
            .padding(.vertical, 40)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.cmoFondo.ignoresSafeArea())
        .onSubmit { avanzar() }
    }

    // MARK: - Secciones

    private var encabezado: some View {
        VStack(spacing: 12) {
            Image(systemName: "cross.case.fill")
                .font(.system(size: 34))
                .foregroundStyle(Color.cmoSobreMarca)
                .frame(width: 72, height: 72)
                .background(Color.cmoMarca, in: .rect(cornerRadius: 20))
                .accessibilityHidden(true)
            Text("Centro Médico de Occidente")
                .font(.title2.bold())
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.cmoTinta)
            Text("Inicia sesión con tu usuario y contraseña")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.cmoTintaSuave)
        }
    }

    private var formulario: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let aviso = viewModel.aviso {
                AvisoBanner(aviso: aviso)
            }

            campo(titulo: "Usuario", error: viewModel.errorUsuario) {
                TextField("Ej. medico.general", text: $viewModel.usuario)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .focused($foco, equals: .usuario)
                    .accessibilityIdentifier("login.usuario")
            }

            campo(titulo: "Contraseña", error: viewModel.errorPassword) {
                HStack {
                    Group {
                        if verPassword {
                            TextField("Contraseña", text: $viewModel.password)
                        } else {
                            SecureField("Contraseña", text: $viewModel.password)
                        }
                    }
                    .textContentType(.password)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.go)
                    .focused($foco, equals: .password)
                    .accessibilityIdentifier("login.password")

                    Button {
                        verPassword.toggle()
                    } label: {
                        Image(systemName: verPassword ? "eye.slash" : "eye")
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                    .accessibilityLabel(verPassword ? "Ocultar contraseña" : "Mostrar contraseña")
                }
            }

            botonEntrar
                .padding(.top, 4)
        }
        .padding(20)
        .background(Color.cmoSuperficie, in: .rect(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.cmoLinea))
    }

    private var botonEntrar: some View {
        Button {
            enviar()
        } label: {
            ZStack {
                Text("Iniciar sesión")
                    .opacity(viewModel.estaCargando ? 0 : 1)
                if viewModel.estaCargando {
                    ProgressView().tint(Color.cmoSobreMarca)
                }
            }
            .font(.headline)
            .frame(maxWidth: .infinity, minHeight: 48)
        }
        .buttonStyle(.borderedProminent)
        .tint(Color.cmoMarca)
        .foregroundStyle(Color.cmoSobreMarca)
        .disabled(!viewModel.puedeEnviar)
        .accessibilityIdentifier("login.entrar")
    }

    private func campo<Contenido: View>(
        titulo: String, error: String?, @ViewBuilder contenido: () -> Contenido
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(titulo)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.cmoTintaSuave)
            contenido()
                .padding(.horizontal, 12)
                .frame(minHeight: 46)
                .background(Color.cmoFondo, in: .rect(cornerRadius: 10))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(error == nil ? Color.cmoLinea : Color.cmoError))
            if let error {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(Color.cmoError)
            }
        }
    }

    // MARK: - Acciones

    private func avanzar() {
        if foco == .usuario {
            foco = .password
        } else {
            enviar()
        }
    }

    private func enviar() {
        foco = nil
        Task { await viewModel.enviar() }
    }
}

#Preview {
    LoginView(store: SessionStore())
}
