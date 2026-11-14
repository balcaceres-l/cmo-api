//
//  VincularExpedienteViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Lógica de vinculación de expediente clínico mediante OTP a correo electrónico (Resend).
@Observable
final class VincularExpedienteViewModel {
    enum Paso {
        case ingresarDui
        case verificarOtp
        case exito(numeroExpediente: String)
    }

    var paso: Paso = .ingresarDui
    var dui: String = "" {
        didSet { errorCampo = nil }
    }
    var otp: String = "" {
        didSet {
            if otp.count > 6 { otp = String(otp.prefix(6)) }
            errorCampo = nil
        }
    }

    private(set) var emailEnmascarado: String?
    private(set) var enviando: Bool = false
    var aviso: AvisoMensaje?
    var errorCampo: String?
    private(set) var segundosReenvio: Int = 0

    @ObservationIgnored private let repositorio: PacientesRepository
    @ObservationIgnored private let store: SessionStore
    @ObservationIgnored private var tareaTemporizador: Task<Void, Never>?

    init(repositorio: PacientesRepository = PacientesRepository(), store: SessionStore) {
        self.repositorio = repositorio
        self.store = store
    }

    deinit {
        tareaTemporizador?.cancel()
    }

    var puedeSolicitarOtp: Bool {
        !dui.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !enviando
    }

    var puedeConfirmarOtp: Bool {
        otp.trimmingCharacters(in: .whitespacesAndNewlines).count == 6 && !enviando
    }

    /// Paso 1: Enviar el OTP al correo enlazado a la cuenta
    func solicitarOtp() async -> Bool {
        let duiLimpio = dui.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !duiLimpio.isEmpty else {
            errorCampo = "Por favor ingresa tu número de DUI."
            return false
        }

        enviando = true
        aviso = nil
        errorCampo = nil
        defer { enviando = false }

        do {
            let res = try await repositorio.solicitarOtpVinculacion(dui: duiLimpio)
            emailEnmascarado = res.emailEnmascarado
            paso = .verificarOtp
            iniciarTemporizadorReenvio()
            aviso = .informacion("Ingresa el código de 6 dígitos que enviamos a \(res.emailEnmascarado).")
            return true
        } catch let error as APIError {
            manejarError(error)
            return false
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
            return false
        }
    }

    /// Paso 2: Verificar el código OTP e imputar el idPaciente a la sesión
    func confirmarOtp() async -> Bool {
        let duiLimpio = dui.trimmingCharacters(in: .whitespacesAndNewlines)
        let otpLimpio = otp.trimmingCharacters(in: .whitespacesAndNewlines)

        guard otpLimpio.count == 6 else {
            errorCampo = "El código debe tener exactamente 6 dígitos."
            return false
        }

        enviando = true
        aviso = nil
        errorCampo = nil
        defer { enviando = false }

        do {
            let res = try await repositorio.confirmarOtpVinculacion(dui: duiLimpio, otp: otpLimpio)
            store.actualizarIdPaciente(res.idPaciente)
            paso = .exito(numeroExpediente: res.numeroExpediente)
            aviso = .exito("¡Tu expediente \(res.numeroExpediente) ha sido vinculado con éxito!")
            tareaTemporizador?.cancel()
            return true
        } catch let error as APIError {
            manejarError(error)
            return false
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
            return false
        }
    }

    func reenviarOtp() async {
        guard segundosReenvio == 0 else { return }
        _ = await solicitarOtp()
    }

    private func iniciarTemporizadorReenvio() {
        tareaTemporizador?.cancel()
        segundosReenvio = 60
        tareaTemporizador = Task { [weak self] in
            while let self, self.segundosReenvio > 0 {
                try? await Task.sleep(for: .seconds(1))
                if Task.isCancelled { break }
                self.segundosReenvio -= 1
            }
        }
    }

    private func manejarError(_ error: APIError) {
        switch error {
        case .validacion(let mensaje, let campos):
            if let errorDui = campos["dui"]?.first {
                errorCampo = errorDui
            } else if let errorOtp = campos["otp"]?.first {
                errorCampo = errorOtp
            } else if let errorEmail = campos["email"]?.first {
                errorCampo = errorEmail
            }
            aviso = .error(mensaje)
        case .noEncontrado:
            errorCampo = "No se encontró ningún expediente con ese DUI."
            aviso = .error(error.localizedDescription)
        case .conflicto:
            aviso = .error(error.localizedDescription)
        default:
            aviso = .error(error.localizedDescription)
        }
    }
}
