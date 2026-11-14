//
//  VincularExpedienteView.swift
//  appcmo
//

import SwiftUI

/// Pantalla para vincular un expediente clínico existente mediante OTP enviado a correo electrónico (Resend).
struct VincularExpedienteView: View {
    @Environment(\.dismiss) private var cerrar
    @Environment(SessionStore.self) private var store
    @Environment(PacienteDatos.self) private var datos

    @State private var viewModel: VincularExpedienteViewModel

    init(repositorio: PacientesRepository = PacientesRepository(), store: SessionStore) {
        _viewModel = State(initialValue: VincularExpedienteViewModel(repositorio: repositorio, store: store))
    }

    var body: some View {
        @Bindable var viewModel = viewModel

        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if let aviso = viewModel.aviso {
                        AvisoBanner(aviso: aviso)
                    }

                    switch viewModel.paso {
                    case .ingresarDui:
                        pasoIngresarDui
                    case .verificarOtp:
                        pasoVerificarOtp
                    case .exito(let numeroExpediente):
                        pasoExito(numeroExpediente: numeroExpediente)
                    }
                }
                .padding(20)
                .frame(maxWidth: 500)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Color.cmoFondo)
            .navigationTitle("Vincular expediente")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cerrar") { cerrar() }
                }
            }
        }
    }

    // MARK: - Paso 1: Ingreso de DUI y solicitud de OTP

    private var pasoIngresarDui: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 14) {
                IconoCirculo(sistema: "folder.badge.person.crop", tono: .info, tamano: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Enlazar mi historial clínico")
                        .font(.headline)
                        .foregroundStyle(Color.cmoTinta)
                    Text("Ingresa tu DUI para localizar tu expediente")
                        .font(.footnote)
                        .foregroundStyle(Color.cmoTintaSuave)
                }
            }

            Text("Si pasaste consulta en el hospital antes de tener esta cuenta, te enviaremos un código de seguridad a tu correo para confirmar tu identidad y habilitar tu expediente digital.")
                .font(.subheadline)
                .foregroundStyle(Color.cmoTintaSuave)
                .lineSpacing(3)

            if let correo = store.sesion?.usuario.email {
                HStack(spacing: 8) {
                    Image(systemName: "envelope.fill")
                        .font(.footnote)
                        .foregroundStyle(Color.cmoMarca)
                    Text("Correo registrado: \(correo)")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(Color.cmoTinta)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.cmoMarcaSuave, in: .rect(cornerRadius: 10))
            }

            CampoFormulario(titulo: "Número de DUI", error: viewModel.errorCampo) {
                TextField("01234567-8", text: $viewModel.dui)
                    .keyboardType(.numbersAndPunctuation)
                    .accessibilityIdentifier("vincular.dui")
            }

            BotonPrincipal(
                titulo: "Enviar código por correo",
                cargando: viewModel.enviando,
                deshabilitado: !viewModel.puedeSolicitarOtp
            ) {
                Task { _ = await viewModel.solicitarOtp() }
            }
            .accessibilityIdentifier("vincular.solicitarOtp")
            .padding(.top, 6)
        }
    }

    // MARK: - Paso 2: Digitación y confirmación del OTP

    private var pasoVerificarOtp: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 14) {
                IconoCirculo(sistema: "envelope.badge", tono: .info, tamano: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Revisa tu correo")
                        .font(.headline)
                        .foregroundStyle(Color.cmoTinta)
                    if let correo = viewModel.emailEnmascarado {
                        Text("Enviado a \(correo)")
                            .font(.footnote)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                }
            }

            Text("Ingresa el código numérico de 6 dígitos que te enviamos vía correo electrónico mediante Resend.")
                .font(.subheadline)
                .foregroundStyle(Color.cmoTintaSuave)

            CampoFormulario(titulo: "Código de verificación (6 dígitos)", error: viewModel.errorCampo) {
                TextField("000000", text: $viewModel.otp)
                    .font(.title2.weight(.bold).monospacedDigit())
                    .multilineTextAlignment(.center)
                    .keyboardType(.numberPad)
                    .accessibilityIdentifier("vincular.otp")
            }

            HStack {
                Spacer()
                if viewModel.segundosReenvio > 0 {
                    Text("Reenviar en \(viewModel.segundosReenvio) s")
                        .font(.footnote)
                        .foregroundStyle(Color.cmoTintaSuave)
                } else {
                    Button("¿No recibiste el correo? Reenviar código") {
                        Task { await viewModel.reenviarOtp() }
                    }
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color.cmoMarca)
                    .accessibilityIdentifier("vincular.reenviar")
                }
                Spacer()
            }
            .padding(.vertical, 4)

            BotonPrincipal(
                titulo: "Verificar y vincular expediente",
                cargando: viewModel.enviando,
                deshabilitado: !viewModel.puedeConfirmarOtp
            ) {
                Task {
                    if await viewModel.confirmarOtp() {
                        await datos.cargarTodo()
                    }
                }
            }
            .accessibilityIdentifier("vincular.confirmarOtp")

            Button("Cambiar número de DUI") {
                viewModel.paso = .ingresarDui
                viewModel.otp = ""
            }
            .font(.footnote)
            .foregroundStyle(Color.cmoTintaSuave)
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Paso 3: Vinculación Exitosa

    private func pasoExito(numeroExpediente: String) -> some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(Color.cmoExito)
                .padding(.top, 16)

            Text("¡Expediente vinculado!")
                .font(.title2.bold())
                .foregroundStyle(Color.cmoTinta)

            Text("Tu expediente \(numeroExpediente) quedó asociado permanentemente a tu cuenta. Ahora podrás consultar todas tus consultas, citas, recetas y resultados de laboratorio.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.cmoTintaSuave)
                .padding(.horizontal, 8)

            BotonPrincipal(titulo: "Continuar a mi inicio") {
                cerrar()
            }
            .accessibilityIdentifier("vincular.exitoContinuar")
            .padding(.top, 12)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
    }
}
