//
//  BorradorStore.swift
//  appcmo
//

import CryptoKit
import Foundation

/// Guarda en disco los borradores de consulta, cifrados con AES-GCM. La llave vive en el Keychain,
/// así que el diagnóstico, las notas y las recetas nunca quedan en texto plano en el iPhone.
/// Hay un archivo por médico y por consulta. Un borrador que lleva más de una hora sin editarse se elimina solo,
/// para que no se acumulen en el iPhone.
nonisolated struct BorradorStore: Sendable {
    /// Tiempo que dura un borrador desde la última vez que se editó.
    static let vigencia: TimeInterval = 60 * 60

    private let directorio: URL
    private let keychain: KeychainManager
    private let vigencia: TimeInterval

    init(
        directorio: URL = URL.applicationSupportDirectory.appending(path: "Borradores", directoryHint: .isDirectory),
        keychain: KeychainManager = .shared,
        vigencia: TimeInterval = BorradorStore.vigencia
    ) {
        self.directorio = directorio
        self.keychain = keychain
        self.vigencia = vigencia
    }

    private func estaVencido(_ archivo: URL, ahora: Date = Date()) -> Bool {
        guard let modificado = (try? FileManager.default.attributesOfItem(atPath: archivo.path))?[.modificationDate] as? Date else { return false }
        return ahora.timeIntervalSince(modificado) > vigencia
    }

    /// Elimina los borradores que llevan más de una hora sin editarse (se llama al abrir la app y al volver a ella).
    func limpiarVencidos() {
        let archivos = (try? FileManager.default.contentsOfDirectory(at: directorio, includingPropertiesForKeys: nil)) ?? []
        for archivo in archivos where archivo.pathExtension == "borrador" && estaVencido(archivo) {
            try? FileManager.default.removeItem(at: archivo)
        }
    }

    private func archivo(usuario: String, consulta: String) -> URL {
        directorio.appending(path: "\(usuario)_\(consulta).borrador")
    }

    func leer(usuario: String, consulta: String) -> BorradorConsulta? {
        let ruta = archivo(usuario: usuario, consulta: consulta)
        if estaVencido(ruta) {
            try? FileManager.default.removeItem(at: ruta)
            return nil
        }
        guard let datos = try? Data(contentsOf: ruta),
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
