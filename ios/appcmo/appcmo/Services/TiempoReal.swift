//
//  TiempoReal.swift
//  appcmo
//

import Foundation

/// Conexión en vivo con Reverb (protocolo de Pusher sobre WebSocket, sin librerías externas).
/// Solo avisa *qué* cambió: la pantalla vuelve a pedir los datos a la API, que es la fuente de verdad.
/// Los canales `private-*` se autorizan con el token del Keychain en `POST /broadcasting/auth`.
nonisolated enum TiempoReal {
    enum Aviso: Sendable, Equatable {
        /// La conexión quedó abierta y suscrita.
        case conectado
        /// Llegó un evento del servidor (nombre corto, p. ej. `AmbulanciaActualizada`).
        case evento(String)
    }

    /// Se abre al empezar a iterar y se cierra al cancelar la tarea. Termina si la conexión se cae.
    static func avisos(canales: [String], config: Config = .actual, keychain: KeychainManager = .shared) -> AsyncStream<Aviso> {
        AsyncStream { continuacion in
            let tarea = Task {
                await escuchar(canales: canales, config: config, keychain: keychain) { continuacion.yield($0) }
                continuacion.finish()
            }
            continuacion.onTermination = { _ in tarea.cancel() }
        }
    }

    struct Config: Sendable {
        let urlSocket: URL
        let urlAuth: URL

        /// Sale de `Config.xcconfig` (REVERB_*) vía `Info.plist`; `nil` si no está configurado.
        static var actual: Config {
            let info = Bundle.main.infoDictionary ?? [:]
            func valor(_ clave: String) -> String? {
                guard let texto = (info[clave] as? String)?.trimmingCharacters(in: .whitespaces),
                      !texto.isEmpty, !texto.hasPrefix("$(") else { return nil }
                return texto
            }
            let host = valor("REVERB_HOST") ?? AppConfig.apiBaseURL.host() ?? "localhost"
            let esquema = valor("REVERB_SCHEME") == "https" ? "wss" : "ws"
            let puerto = valor("REVERB_PORT") ?? "8080"
            let llave = valor("REVERB_APP_KEY") ?? "hospital-universitario-key"
            let socket = URL(string: "\(esquema)://\(host):\(puerto)/app/\(llave)?protocol=7&client=appcmo&version=1.0")!
            return Config(urlSocket: socket, urlAuth: AppConfig.apiBaseURL.appending(path: "broadcasting/auth"))
        }
    }

    private static func escuchar(canales: [String], config: Config, keychain: KeychainManager, avisar: @Sendable (Aviso) -> Void) async {
        let socket = NetworkClient.sesionSinCache.webSocketTask(with: config.urlSocket)
        socket.resume()
        defer { socket.cancel(with: .goingAway, reason: nil) }

        func enviar(_ evento: String, _ datos: [String: String]) async throws {
            let json = try JSONSerialization.data(withJSONObject: ["event": evento, "data": datos])
            try await socket.send(.string(String(decoding: json, as: UTF8.self)))
        }

        var pendientes = Set(canales)
        while !Task.isCancelled {
            guard let mensaje = try? await socket.receive(), case let .string(texto) = mensaje,
                  let sobre = try? JSONSerialization.jsonObject(with: Data(texto.utf8)) as? [String: Any],
                  let evento = sobre["event"] as? String else { return }
            // `data` llega como texto JSON dentro del sobre.
            let datos = (sobre["data"] as? String).flatMap { try? JSONSerialization.jsonObject(with: Data($0.utf8)) as? [String: Any] }

            do {
                switch evento {
                case "pusher:connection_established":
                    guard let socketId = datos?["socket_id"] as? String else { return }
                    for canal in canales {
                        var suscripcion = ["channel": canal]
                        if canal.hasPrefix("private-") {
                            guard let firma = await autorizar(canal: canal, socketId: socketId, config: config, keychain: keychain) else { return }
                            suscripcion["auth"] = firma
                        }
                        try await enviar("pusher:subscribe", suscripcion)
                    }
                case "pusher:ping":
                    try await enviar("pusher:pong", [:])
                case "pusher_internal:subscription_succeeded":
                    if let canal = sobre["channel"] as? String { pendientes.remove(canal) }
                    if pendientes.isEmpty { avisar(.conectado) }
                case "pusher:error":
                    return
                default:
                    // Laravel nombra el evento con su clase: "App\\Events\\AmbulanciaActualizada".
                    if !evento.hasPrefix("pusher") { avisar(.evento(evento.components(separatedBy: "\\").last ?? evento)) }
                }
            } catch {
                return
            }
        }
    }

    private static func autorizar(canal: String, socketId: String, config: Config, keychain: KeychainManager) async -> String? {
        guard let token = keychain.leerToken() else { return nil }
        var peticion = URLRequest(url: config.urlAuth)
        peticion.httpMethod = "POST"
        peticion.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        peticion.setValue("application/json", forHTTPHeaderField: "Accept")
        peticion.setValue("application/json", forHTTPHeaderField: "Content-Type")
        peticion.httpBody = try? JSONSerialization.data(withJSONObject: ["socket_id": socketId, "channel_name": canal])
        guard let (datos, respuesta) = try? await NetworkClient.sesionSinCache.data(for: peticion),
              (respuesta as? HTTPURLResponse)?.statusCode == 200,
              let json = try? JSONSerialization.jsonObject(with: datos) as? [String: Any] else { return nil }
        return json["auth"] as? String
    }

    /// Recarga con `refrescar` en cada evento. Mientras no hay conexión en vivo, consulta cada `intervalo`
    /// y reintenta conectar. `enVivo` informa si la conexión está abierta (para mostrarlo en pantalla).
    static func vigilar(
        canales: [String],
        intervalo: Duration = .seconds(10),
        enVivo: @escaping @MainActor (Bool) -> Void,
        refrescar: @escaping @MainActor () async -> Void
    ) async {
        await withTaskGroup(of: Void.self) { grupo in
            let conectado = Bandera()
            grupo.addTask {
                while !Task.isCancelled {
                    for await aviso in avisos(canales: canales) {
                        if aviso == .conectado {
                            conectado.valor = true
                            await enVivo(true)
                        }
                        await refrescar()
                    }
                    conectado.valor = false
                    await enVivo(false)
                    try? await Task.sleep(for: intervalo)
                }
            }
            grupo.addTask {
                while !Task.isCancelled {
                    try? await Task.sleep(for: intervalo)
                    if !Task.isCancelled, !conectado.valor { await refrescar() }
                }
            }
            await grupo.waitForAll()
        }
    }

    private final class Bandera: @unchecked Sendable {
        private let candado = NSLock()
        private var _valor = false
        var valor: Bool {
            get { candado.withLock { _valor } }
            set { candado.withLock { _valor = newValue } }
        }
    }
}
