//
//  LoginView.swift
//  appcmo
//

import SwiftUI

struct LoginView: View {
    private enum Campo: Hashable {
        case usuario, password
    }

    private enum Hoja: String, Identifiable {
        case registro, publico
        var id: String { rawValue }
    }

    private let store: SessionStore
    @State private var viewModel: LoginViewModel
    @State private var verPassword = false
    @State private var hoja: Hoja?
    @FocusState private var foco: Campo?

    init(store: SessionStore) {
        self.store = store
        _viewModel = State(initialValue: LoginViewModel(store: store))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 28) {
                encabezado
                formulario
                otrasOpciones
            }
            .frame(maxWidth: 440)
            .padding(.horizontal, 20)
            .padding(.vertical, 40)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.cmoFondo.ignoresSafeArea())
        .onSubmit { avanzar() }
        .sheet(item: $hoja) { hoja in
            switch hoja {
            case .registro:
                RegistroView(store: store)
            case .publico:
                NavigationStack {
                    InfoPublicaView(acciones: .init(
                        iniciarSesion: { self.hoja = nil },
                        crearCuenta: { self.hoja = .registro }
                    ))
                        .toolbar {
                            ToolbarItem(placement: .cancellationAction) {
                                Button("Cerrar") { self.hoja = nil }
                            }
                        }
                }
            }
        }
    }

    // MARK: - Secciones

    private var encabezado: some View {
        VStack(spacing: 10) {
            Image("Logo")
                .resizable()
                .scaledToFit()
                .frame(width: 112, height: 112)
                .accessibilityLabel("Centro Médico de Occidente")
            Text("CMO")
                .font(.title.bold())
                .foregroundStyle(Color.cmoMarca)
                .padding(.bottom, 8)
            Text("Iniciar sesión")
                .font(.title2.bold())
                .foregroundStyle(Color.cmoTinta)
            Text("Escribe tu usuario y tu contraseña. La app reconoce tu rol automáticamente.")
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

            campo(titulo: "Usuario", sistema: "person", error: viewModel.errorUsuario) {
                TextField("Ej. medico.general", text: $viewModel.usuario)
                    .textContentType(.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .focused($foco, equals: .usuario)
                    .accessibilityIdentifier("login.usuario")
            }

            campo(titulo: "Contraseña", sistema: "lock", error: viewModel.errorPassword) {
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
    }

    private var otrasOpciones: some View {
        VStack(spacing: 12) {
            Text("¿Eres paciente y aún no tienes cuenta?")
                .font(.footnote)
                .foregroundStyle(Color.cmoTintaSuave)
            BotonSecundario(titulo: "Crear cuenta") { hoja = .registro }
                .accessibilityIdentifier("login.registro")
            Button("Información del hospital") { hoja = .publico }
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier("login.publico")
        }
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
        titulo: String, sistema: String, error: String?, @ViewBuilder contenido: () -> Contenido
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(titulo)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color.cmoTintaSuave)
            HStack(spacing: 10) {
                Image(systemName: sistema).foregroundStyle(Color.cmoTintaSuave).accessibilityHidden(true)
                contenido()
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 48)
            .background(Color.cmoSuperficie, in: .rect(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(error == nil ? Color.cmoLinea : Color.cmoError))
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
