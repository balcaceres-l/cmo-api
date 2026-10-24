//
//  AppConfig.swift
//  appcmo
//

import Foundation

/// Valores de entorno. `API_BASE_URL` sale de `ios/Config.xcconfig` (ver `Config.example.xcconfig`)
/// vía `Info.plist`; si el archivo no existe se usa la API local del simulador.
nonisolated enum AppConfig {
    static let urlPorDefecto = "http://localhost:8000/api/v1"

    static var apiBaseURL: URL {
        let valor = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String
        // Sin Config.xcconfig el Info.plist conserva el literal "$(API_BASE_URL)".
        let limpio = valor?.trimmingCharacters(in: .whitespaces)
        let texto = (limpio?.isEmpty == false && limpio?.hasPrefix("$(") == false) ? limpio! : urlPorDefecto
        return URL(string: texto) ?? URL(string: urlPorDefecto)!
    }
}
