//
//  Formato.swift
//  appcmo
//

import Foundation

/// Fechas y horas tal como las usa la API (`YYYY-MM-DD`, `HH:mm`) y como se muestran en pantalla.
/// Todo se interpreta en la zona del hospital (`America/El_Salvador`).
nonisolated enum Formato {
    static let zona = TimeZone(identifier: "America/El_Salvador") ?? .current

    static var calendario: Calendar {
        var calendario = Calendar(identifier: .gregorian)
        calendario.timeZone = zona
        calendario.locale = Locale(identifier: "es")
        calendario.firstWeekday = 2 // la semana empieza el lunes
        return calendario
    }

    private static func formateador(_ patron: String, posix: Bool = false) -> DateFormatter {
        let formateador = DateFormatter()
        formateador.locale = Locale(identifier: posix ? "en_US_POSIX" : "es")
        formateador.timeZone = zona
        formateador.dateFormat = patron
        return formateador
    }

    // MARK: - API

    static func fechaAPI(_ fecha: Date) -> String {
        formateador("yyyy-MM-dd", posix: true).string(from: fecha)
    }

    static func fecha(desdeAPI texto: String?) -> Date? {
        guard let texto, texto.count >= 10 else { return nil }
        return formateador("yyyy-MM-dd", posix: true).date(from: String(texto.prefix(10)))
    }

    static var hoyAPI: String { fechaAPI(Date()) }

    // MARK: - Pantalla

    /// `08:00:00` → `08:00`.
    static func hora(_ texto: String?) -> String {
        guard let texto, texto.count >= 5 else { return texto ?? "" }
        return String(texto.prefix(5))
    }

    static func rangoHoras(_ inicio: String?, _ fin: String?) -> String {
        "\(hora(inicio))–\(hora(fin))"
    }

    /// `2026-10-26` → `Lunes 26 de octubre`.
    static func fechaLarga(_ texto: String?) -> String {
        guard let fecha = fecha(desdeAPI: texto) else { return texto ?? "" }
        return mayuscula(formateador("EEEE d 'de' MMMM").string(from: fecha))
    }

    static func fechaLarga(_ fecha: Date) -> String {
        mayuscula(formateador("EEEE d 'de' MMMM").string(from: fecha))
    }

    /// `2026-10-26` → `26 oct 2026`.
    static func fechaCorta(_ texto: String?) -> String {
        guard let fecha = fecha(desdeAPI: texto) else { return texto ?? "" }
        return formateador("d MMM yyyy").string(from: fecha).replacingOccurrences(of: ".", with: "")
    }

    /// Marca de tiempo ISO 8601 de la API → `26 oct 2026, 14:30`.
    static func marcaTiempo(_ texto: String?) -> String {
        guard let texto else { return "" }
        let conFraccion = ISO8601DateFormatter()
        conFraccion.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let sinFraccion = ISO8601DateFormatter()
        sinFraccion.formatOptions = [.withInternetDateTime]
        guard let fecha = conFraccion.date(from: texto) ?? sinFraccion.date(from: texto) else { return texto }
        return formateador("d MMM yyyy, HH:mm").string(from: fecha).replacingOccurrences(of: ".", with: "")
    }

    /// Marca de tiempo ISO 8601 de la API → `Date`.
    static func instante(_ texto: String?) -> Date? {
        guard let texto else { return nil }
        let conFraccion = ISO8601DateFormatter()
        conFraccion.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let sinFraccion = ISO8601DateFormatter()
        sinFraccion.formatOptions = [.withInternetDateTime]
        return conFraccion.date(from: texto) ?? sinFraccion.date(from: texto)
    }

    /// ¿La hora actual cae dentro de `[inicio, fin]` (formato `HH:mm[:ss]`)?
    static func horaActualEntre(_ inicio: String, _ fin: String) -> Bool {
        let ahora = formateador("HH:mm:ss", posix: true).string(from: Date())
        return hora(inicio) <= String(ahora.prefix(5)) && String(ahora.prefix(5)) <= hora(fin)
    }

    /// `2026-10-19` → `Lun 19`.
    static func diaCorto(_ texto: String?) -> String {
        guard let fecha = fecha(desdeAPI: texto) else { return texto ?? "" }
        return mayuscula(formateador("EEE d").string(from: fecha).replacingOccurrences(of: ".", with: ""))
    }

    /// `Octubre 2026` para el mes de una fecha.
    static func mesYAnio(_ fecha: Date) -> String {
        mayuscula(formateador("LLLL yyyy").string(from: fecha))
    }

    /// Hora actual del hospital en formato `HH:mm`.
    static var ahoraHHmm: String {
        String(formateador("HH:mm", posix: true).string(from: Date()))
    }

    /// `Dra. Ana Martínez` → `AM` (se ignoran los títulos).
    static func iniciales(_ nombre: String) -> String {
        let titulos: Set<String> = ["dr", "dr.", "dra", "dra.", "lic", "lic.", "ing", "ing."]
        let palabras = nombre.split(whereSeparator: \.isWhitespace).map(String.init).filter { !titulos.contains($0.lowercased()) }
        return palabras.prefix(2).compactMap { $0.first.map { String($0).uppercased() } }.joined()
    }

    /// Buenos días, buenas tardes o buenas noches según la hora del hospital.
    static func saludo(en fecha: Date = Date()) -> String {
        let hora = calendario.component(.hour, from: fecha)
        switch hora {
        case 5..<12: return "Buenos días"
        case 12..<19: return "Buenas tardes"
        default: return "Buenas noches"
        }
    }

    private static func mayuscula(_ texto: String) -> String {
        guard let primera = texto.first else { return texto }
        return primera.uppercased() + texto.dropFirst()
    }
}
