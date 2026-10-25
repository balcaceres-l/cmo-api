//
//  Tarjeta.swift
//  appcmo
//

import SwiftUI

/// Encabezado de una pantalla principal: título grande y una línea de apoyo (como en el Figma).
struct EncabezadoPantalla<Accesorio: View>: View {
    let titulo: String
    var subtitulo: String?
    @ViewBuilder var accesorio: () -> Accesorio

    init(_ titulo: String, subtitulo: String? = nil, @ViewBuilder accesorio: @escaping () -> Accesorio = { EmptyView() }) {
        self.titulo = titulo
        self.subtitulo = subtitulo
        self.accesorio = accesorio
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(titulo)
                    .font(.title.bold())
                    .foregroundStyle(Color.cmoTinta)
                if let subtitulo {
                    Text(subtitulo)
                        .font(.subheadline)
                        .foregroundStyle(Color.cmoTintaSuave)
                }
            }
            Spacer(minLength: 0)
            accesorio()
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

/// Fondo blanco con esquinas redondeadas y borde fino, como las tarjetas del diseño.
private struct EstiloTarjeta: ViewModifier {
    var relleno: CGFloat

    func body(content: Content) -> some View {
        content
            .padding(relleno)
            .background(Color.cmoSuperficie, in: .rect(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.cmoLinea))
    }
}

extension View {
    func tarjeta(relleno: CGFloat = 14) -> some View {
        modifier(EstiloTarjeta(relleno: relleno))
    }
}

/// Círculo con las iniciales de una persona, teñido según el estado.
struct AvatarIniciales: View {
    let nombre: String
    var tono: EstadoChip.Tono = .neutro
    var tamano: CGFloat = 40

    var body: some View {
        Text(Formato.iniciales(nombre))
            .font(.system(size: tamano * 0.36, weight: .semibold))
            .foregroundStyle(tono.primerPlano)
            .frame(width: tamano, height: tamano)
            .background(tono.fondo, in: .circle)
            .accessibilityHidden(true)
    }
}

/// Círculo con un ícono, teñido según el estado.
struct IconoCirculo: View {
    let sistema: String
    var tono: EstadoChip.Tono = .neutro
    var tamano: CGFloat = 40

    var body: some View {
        Image(systemName: sistema)
            .font(.system(size: tamano * 0.4, weight: .semibold))
            .foregroundStyle(tono.primerPlano)
            .frame(width: tamano, height: tamano)
            .background(tono.fondo, in: .circle)
            .accessibilityHidden(true)
    }
}

/// Una cita en la agenda del médico: hora, paciente, motivo, consultorio y estado.
struct CitaTarjeta: View {
    enum Icono {
        case iniciales, reloj
    }

    let cita: Cita
    var icono: Icono = .iniciales

    private var subtitulo: String {
        [cita.motivo, cita.consultorio?.codigo].compactMap { $0 }.joined(separator: " · ")
    }

    var body: some View {
        HStack(spacing: 12) {
            switch icono {
            case .iniciales:
                AvatarIniciales(nombre: cita.paciente?.nombreCompleto ?? "P", tono: cita.estado.tonoMedico)
            case .reloj:
                IconoCirculo(sistema: "clock", tono: cita.estado.tonoMedico)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("\(Formato.hora(cita.horaInicio)) · \(cita.paciente?.nombreCompleto ?? "Paciente")")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                    .strikethrough(cita.estado.estaInactiva)
                    .lineLimit(1)
                Text(subtitulo)
                    .font(.caption)
                    .foregroundStyle(Color.cmoTintaSuave)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            EstadoChip(texto: cita.estado.nombreMedico, tono: cita.estado.tonoMedico)
        }
        .tarjeta()
        .opacity(cita.estado.estaInactiva ? 0.6 : 1)
    }
}
