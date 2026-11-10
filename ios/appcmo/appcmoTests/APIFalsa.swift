//
//  APIFalsa.swift
//  appcmoTests
//

import Foundation
@testable import appcmo

/// API falsa para las pruebas: responde lo que cada prueba indica, sin red ni servidor.
/// Va debajo de los repositorios reales, así que también se prueba cómo se lee el JSON de la API.
final class APIFalsa: URLProtocol, @unchecked Sendable {
    struct Respuesta {
        let estado: Int
        let json: String
        var cabeceras: [String: String] = [:]
    }

    struct Peticion {
        let metodo: String
        let ruta: String
        let cuerpo: [String: Any]
        let token: String?
    }

    nonisolated(unsafe) private static var respuestas: [String: Respuesta] = [:]
    nonisolated(unsafe) private(set) static var peticiones: [Peticion] = []
    private static let candado = NSLock()

    /// `ruta` es "MÉTODO ruta", p. ej. "POST auth/login".
    static func responder(_ ruta: String, _ estado: Int, _ json: String, cabeceras: [String: String] = [:]) {
        candado.withLock { respuestas[ruta] = Respuesta(estado: estado, json: json, cabeceras: cabeceras) }
    }

    static func reiniciar() {
        candado.withLock { respuestas = [:]; peticiones = [] }
    }

    static func peticion(_ ruta: String) -> Peticion? {
        candado.withLock { peticiones.last { "\($0.metodo) \($0.ruta)" == ruta } }
    }

    /// Repositorios conectados a esta API falsa.
    static func cliente(keychain: KeychainManager) -> NetworkClient {
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [APIFalsa.self]
        return NetworkClient(baseURL: URL(string: "https://api.prueba/api/v1")!, session: URLSession(configuration: config), keychain: keychain)
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func stopLoading() {}

    override func startLoading() {
        let url = request.url!
        let ruta = url.path().replacingOccurrences(of: "/api/v1/", with: "")
        let metodo = request.httpMethod ?? "GET"
        let datos = request.httpBody ?? request.httpBodyStream.map(Self.leer) ?? Data()
        let cuerpo = (try? JSONSerialization.jsonObject(with: datos) as? [String: Any]) ?? [:]
        let token = request.value(forHTTPHeaderField: "Authorization")?.replacingOccurrences(of: "Bearer ", with: "")

        let respuesta = Self.candado.withLock {
            Self.peticiones.append(Peticion(metodo: metodo, ruta: ruta, cuerpo: cuerpo, token: token))
            return Self.respuestas["\(metodo) \(ruta)"]
        } ?? Respuesta(estado: 404, json: #"{"success":false,"message":"Sin respuesta falsa para \#(metodo) \#(ruta)"}"#)

        let http = HTTPURLResponse(url: url, statusCode: respuesta.estado, httpVersion: nil, headerFields: respuesta.cabeceras)!
        client?.urlProtocol(self, didReceive: http, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(respuesta.json.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    private static func leer(_ flujo: InputStream) -> Data {
        flujo.open()
        defer { flujo.close() }
        var datos = Data()
        var bufer = [UInt8](repeating: 0, count: 4096)
        while flujo.hasBytesAvailable {
            let leidos = flujo.read(&bufer, maxLength: bufer.count)
            if leidos <= 0 { break }
            datos.append(bufer, count: leidos)
        }
        return datos
    }
}

/// Respuestas de ejemplo de la API.
enum JSONPrueba {
    static func login(rol: String, token: String = "token-prueba", idPaciente: String? = nil) -> String {
        let paciente = idPaciente.map { "\"\($0)\"" } ?? "null"
        return """
        {"success":true,"message":"Sesión iniciada.","data":{"token":"\(token)","id_paciente":\(paciente),
        "usuario":{"id":"u-1","nombre_completo":"Persona Prueba","usuario":"prueba","email":null,"rol":"\(rol)","cargo":null,"estado":"ACTIVO"}}}
        """
    }

    static func validacion(_ mensaje: String = "Los datos enviados no son válidos.", campos: [String: String]) -> String {
        let errores = campos.map { "\"\($0.key)\":[\"\($0.value)\"]" }.joined(separator: ",")
        return #"{"success":false,"message":"\#(mensaje)","errors":{\#(errores)}}"#
    }

    static let ok = #"{"success":true,"message":"OK","data":null}"#
}
