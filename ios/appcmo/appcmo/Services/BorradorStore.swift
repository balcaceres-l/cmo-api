//
//  BorradorStore.swift
//  appcmo
//

import CryptoKit
import Foundation

/// Guarda en disco los borradores de consulta, cifrados con AES-GCM. La llave vive en el Keychain,
/// así que el diagnóstico, las notas y las recetas nunca quedan en texto plano en el iPhone.
/// Hay un archivo por médico y por consulta.
nonisolated struct BorradorStore: Sendable {
    private let directorio: URL
    private let keychain: KeychainManager

    init(
        directorio: URL = URL.applicationSupportDirectory.appending(path: "Borradores", directoryHint: .isDirectory),
        keychain: KeychainManager = .shared
    ) {
        self.directorio = directorio
        self.keychain = keychain
    }

    private func archivo(usuario: String, consulta: String) -> URL {
        directorio.appending(path: "\(usuario)_\(consulta).borrador")
    }

    func leer(usuario: String, consulta: String) -> BorradorConsulta? {
        guard let datos = try? Data(contentsOf: archivo(usuario: usuario, consulta: consulta)),
              let llave = try? keychain.llaveBorradores(),
              let caja = try? AES.GCM.SealedBox(combined: datos),
              let plano = try? AES.GCM.open(caja, using: llave) else { return nil }
        return try? JSONDecoder().decode(BorradorConsulta.self, from: plano)
    }

    func guardar(_ borrador: BorradorConsulta, usuario: String, consulta: String) throws {
        var carpeta = directorio
        try FileManager.default.createDirectory(at: carpeta, withIntermediateDirectories: true)
        var valores = URLResourceValues()
        valores.isExcludedFromBackup = true // el borrador no viaja a copias de seguridad
        try? carpeta.setResourceValues(valores)

        let llave = try keychain.llaveBorradores()
        let caja = try AES.GCM.seal(try JSONEncoder().encode(borrador), using: llave)
        guard let datos = caja.combined else { throw KeychainError(estado: errSecParam) }
        #if os(iOS)
        try datos.write(to: archivo(usuario: usuario, consulta: consulta), options: [.atomic, .completeFileProtection])
        #else
        try datos.write(to: archivo(usuario: usuario, consulta: consulta), options: .atomic)
        #endif
    }

    func borrar(usuario: String, consulta: String) {
        try? FileManager.default.removeItem(at: archivo(usuario: usuario, consulta: consulta))
    }
}
