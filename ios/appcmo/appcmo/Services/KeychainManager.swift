//
//  KeychainManager.swift
//  appcmo
//

import Foundation
import Security

nonisolated struct KeychainError: LocalizedError {
    let estado: OSStatus

    var errorDescription: String? {
        "No se pudo guardar la sesión de forma segura en este dispositivo."
    }
}

/// Guarda el token de sesión en el Keychain del sistema. El token nunca se imprime ni va a UserDefaults.
nonisolated struct KeychainManager: Sendable {
    static let shared = KeychainManager()

    private let servicio = Bundle.main.bundleIdentifier ?? "com.catolica.edu.sv.appcmo"
    private let cuentaToken = "auth_token"

    private var consultaBase: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: servicio,
            kSecAttrAccount as String: cuentaToken,
        ]
    }

    func guardarToken(_ token: String) throws {
        let datos = Data(token.utf8)
        var actualizar = consultaBase
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

    func leerToken() -> String? {
        var consulta = consultaBase
        consulta[kSecReturnData as String] = true
        consulta[kSecMatchLimit as String] = kSecMatchLimitOne

        var resultado: CFTypeRef?
        guard SecItemCopyMatching(consulta as CFDictionary, &resultado) == errSecSuccess,
              let datos = resultado as? Data else { return nil }
        return String(data: datos, encoding: .utf8)
    }

    func borrarToken() {
        SecItemDelete(consultaBase as CFDictionary)
    }
}
