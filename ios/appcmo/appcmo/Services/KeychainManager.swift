//
//  KeychainManager.swift
//  appcmo
//

import CryptoKit
import Foundation
import Security

nonisolated struct KeychainError: LocalizedError {
    let estado: OSStatus

    var errorDescription: String? {
        "No se pudo guardar la información de forma segura en este dispositivo."
    }
}

/// Guarda en el Keychain del sistema el token de sesión y la llave que cifra los borradores.
/// Nada de esto se imprime ni va a UserDefaults.
nonisolated struct KeychainManager: Sendable {
    static let shared = KeychainManager()

    private let servicio = Bundle.main.bundleIdentifier ?? "com.catolica.edu.sv.appcmo"
    private let cuentaToken = "auth_token"
    private let cuentaLlaveBorradores = "borradores_key"

    // MARK: - Token de sesión

    func guardarToken(_ token: String) throws {
        try guardar(Data(token.utf8), cuenta: cuentaToken)
    }

    func leerToken() -> String? {
        leer(cuenta: cuentaToken).flatMap { String(data: $0, encoding: .utf8) }
    }

    func borrarToken() {
        borrar(cuenta: cuentaToken)
    }

    // MARK: - Llave de los borradores

    /// Llave AES de 256 bits que cifra los borradores en disco. Se crea la primera vez y no sale del dispositivo.
    func llaveBorradores() throws -> SymmetricKey {
        if let datos = leer(cuenta: cuentaLlaveBorradores) {
            return SymmetricKey(data: datos)
        }
        let llave = SymmetricKey(size: .bits256)
        try guardar(llave.withUnsafeBytes { Data($0) }, cuenta: cuentaLlaveBorradores)
        return llave
    }

    // MARK: - Interno

    private func consulta(cuenta: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: servicio,
            kSecAttrAccount as String: cuenta,
        ]
    }

    private func guardar(_ datos: Data, cuenta: String) throws {
        var actualizar = consulta(cuenta: cuenta)
        let atributos: [String: Any] = [kSecValueData as String: datos]

        let estado = SecItemUpdate(actualizar as CFDictionary, atributos as CFDictionary)
        if estado == errSecItemNotFound {
            actualizar[kSecValueData as String] = datos
            actualizar[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            let alta = SecItemAdd(actualizar as CFDictionary, nil)
            guard alta == errSecSuccess else { throw KeychainError(estado: alta) }
        } else if estado != errSecSuccess {
            throw KeychainError(estado: estado)
        }
    }

    private func leer(cuenta: String) -> Data? {
        var consulta = consulta(cuenta: cuenta)
        consulta[kSecReturnData as String] = true
        consulta[kSecMatchLimit as String] = kSecMatchLimitOne

        var resultado: CFTypeRef?
        guard SecItemCopyMatching(consulta as CFDictionary, &resultado) == errSecSuccess else { return nil }
        return resultado as? Data
    }

    private func borrar(cuenta: String) {
        SecItemDelete(consulta(cuenta: cuenta) as CFDictionary)
    }
}
