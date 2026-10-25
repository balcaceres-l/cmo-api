//
//  RegistroView.swift
//  appcmo
//

import SwiftUI

/// Registro de un paciente (HU-22). Si recepción ya lo registró, se vincula con el código de activación.
struct RegistroView: View {
    @Environment(\.dismiss) private var cerrar
    @State private var viewModel: RegistroViewModel

    init(store: SessionStore) {
        _viewModel = State(initialValue: RegistroViewModel(store: store))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Crea tu cuenta de paciente para consultar tus citas y pedir atención.")
                        .font(.subheadline)
                        .foregroundStyle(Color.cmoTintaSuave)

                    if let aviso = viewModel.aviso {
                        AvisoBanner(aviso: aviso)
                    }

                    CampoFormulario(titulo: "Nombre completo", error: viewModel.errores[.nombreCompleto]) {
                        TextField("Como aparece en tu DUI", text: $viewModel.nombreCompleto)
                            .textContentType(.name)
                            .accessibilityIdentifier("registro.nombre")
                    }
                    CampoFormulario(titulo: "Usuario", error: viewModel.errores[.usuario]) {
                        TextField("Para iniciar sesión", text: $viewModel.usuario)
                            .textContentType(.username)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .accessibilityIdentifier("registro.usuario")
                    }
                    CampoFormulario(titulo: "DUI", error: viewModel.errores[.dui]) {
                        TextField("Ej. 01234567-8", text: $viewModel.dui)
                            .keyboardType(.numbersAndPunctuation)
                            .accessibilityIdentifier("registro.dui")
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Fecha de nacimiento")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Color.cmoTintaSuave)
                        DatePicker("Fecha de nacimiento", selection: $viewModel.fechaNacimiento, in: ...Date(), displayedComponents: .date)
                            .labelsHidden()
                            .environment(\.locale, Locale(identifier: "es"))
                            .environment(\.timeZone, Formato.zona)
                        if let error = viewModel.errores[.fechaNacimiento] {
                            Text(error).font(.footnote).foregroundStyle(Color.cmoError)
                        }
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Sexo")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(Color.cmoTintaSuave)
                        Picker("Sexo", selection: $viewModel.sexo) {
                            Text("Femenino").tag("F")
                            Text("Masculino").tag("M")
                        }
                        .pickerStyle(.segmented)
                    }

                    CampoFormulario(titulo: "Correo (opcional)", error: viewModel.errores[.email]) {
                        TextField("correo@ejemplo.com", text: $viewModel.email)
                            .textContentType(.emailAddress)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                    CampoFormulario(titulo: "Teléfono (opcional)", error: viewModel.errores[.telefono]) {
                        TextField("Ej. 7000-0000", text: $viewModel.telefono)
                            .textContentType(.telephoneNumber)
                            .keyboardType(.phonePad)
                    }
                    CampoFormulario(titulo: "Dirección (opcional)", error: viewModel.errores[.direccion]) {
                        TextField("Dirección", text: $viewModel.direccion)
                            .textContentType(.fullStreetAddress)
                    }

                    CampoFormulario(titulo: "Contraseña", error: viewModel.errores[.password]) {
                        SecureField("Mínimo 8 caracteres", text: $viewModel.password)
                            .textContentType(.newPassword)
                            .accessibilityIdentifier("registro.password")
                    }
                    CampoFormulario(titulo: "Confirmar contraseña", error: viewModel.errores[.confirmacion]) {
                        SecureField("Repite la contraseña", text: $viewModel.confirmacion)
                            .textContentType(.newPassword)
                            .accessibilityIdentifier("registro.confirmacion")
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        CampoFormulario(titulo: "Código de activación (opcional)", error: viewModel.errores[.codigoActivacion]) {
                            TextField("6 dígitos", text: $viewModel.codigoActivacion)
                                .keyboardType(.numberPad)
                                .accessibilityIdentifier("registro.codigo")
                        }
                        Text("Si recepción ya te registró, pide ahí tu código de 6 dígitos para vincular tu cuenta a tu expediente.")
                            .font(.footnote)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }

                    BotonPrincipal(titulo: "Crear cuenta", cargando: viewModel.enviando) {
                        Task { await viewModel.registrar() }
                    }
                    .accessibilityIdentifier("registro.crear")
                    .padding(.top, 4)
                }
                .padding(20)
                .frame(maxWidth: 520)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Color.cmoFondo)
            .navigationTitle("Crear cuenta")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancelar") { cerrar() }
                }
            }
        }
    }
}
