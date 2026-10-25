//
//  PacienteInicioView.swift
//  appcmo
//

import SwiftUI

/// Inicio del paciente (pantalla 04): su próxima cita, el estado de sus exámenes, su habitación y pedir atención.
struct PacienteInicioView: View {
    enum Destino: Hashable {
        case solicitar, habitacion, hospital
    }

    @Environment(SessionStore.self) private var store
    @Environment(PacienteDatos.self) private var datos
    @State private var ruta: [Destino] = []

    let irA: (PacienteTabView.Pestana) -> Void

    private var usuario: Usuario? { store.sesion?.usuario }

    var body: some View {
        NavigationStack(path: $ruta) {
            EstadoCarga(carga: datos.citas, reintentar: { Task { await datos.cargarTodo() } }) { _ in
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        encabezado
                        proxima
                        examenes
                        habitacion
                        BotonSecundario(titulo: "Solicitar atención", sistema: "plus") { ruta.append(.solicitar) }
                            .accessibilityIdentifier("inicio.solicitar")
                        hospital
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 24)
                }
                .refreshable { await datos.cargarTodo() }
            }
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Destino.self) { destino in
                switch destino {
                case .solicitar: SolicitudAtencionView()
                case .habitacion: MiHabitacionView()
                case .hospital: InfoPublicaView(mostrarAvisos: false)
                }
            }
        }
    }

    // MARK: - Secciones

    private var encabezado: some View {
        EncabezadoPantalla("Hola, \(usuario?.nombreCompleto ?? "")", subtitulo: datos.expediente.valor.map { "Expediente \($0.numeroExpediente)" }) {
            CampanaAvisos()
        }
    }

    @ViewBuilder
    private var proxima: some View {
        if let cita = datos.proxima {
            VStack(alignment: .leading, spacing: 10) {
                Text("Próxima cita")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color.cmoSobreMarca.opacity(0.8))
                Text("\(Formato.diaYMes(cita.fecha)) · \(Formato.hora(cita.horaInicio))")
                    .font(.title2.bold())
                    .foregroundStyle(Color.cmoSobreMarca)
                if let medico = cita.medico {
                    Label(medico.nombreCompleto, systemImage: "stethoscope")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.cmoSobreMarca)
                }
                Text([cita.medico?.cargo, cita.consultorio.map { "Consultorio \($0.codigo)" }].compactMap { $0 }.joined(separator: " · "))
                    .font(.footnote)
                    .foregroundStyle(Color.cmoSobreMarca.opacity(0.85))
                Text(cita.estado.nombrePaciente(esHoy: datos.esHoy(cita)))
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .foregroundStyle(Color.cmoMarca)
                    .background(Color.cmoSobreMarca, in: .capsule)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color.cmoMarca, in: .rect(cornerRadius: 18))
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier("inicio.proximaCita")
        } else {
            VStack(spacing: 8) {
                IconoCirculo(sistema: "calendar", tono: .neutro, tamano: 44)
                Text("No tienes citas próximas").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                Text("Cuando recepción agende una, aparecerá aquí.").font(.footnote).foregroundStyle(Color.cmoTintaSuave)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .tarjeta()
        }
    }

    private var examenes: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Mis exámenes").font(.headline).foregroundStyle(Color.cmoTinta)
                Spacer()
                Button("Ver todos") { irA(.laboratorio) }
                    .font(.subheadline.weight(.semibold))
                    .accessibilityIdentifier("inicio.verExamenes")
            }
            HStack(spacing: 10) {
                ForEach(EstadoOrden.allCases, id: \.self) { estado in
                    let tono = estado.tono
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(datos.cuantas(estado))").font(.title2.bold())
                        Text(estado.nombre).font(.caption)
                    }
                    .foregroundStyle(tono.primerPlano)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(tono.fondo, in: .rect(cornerRadius: 14))
                    .accessibilityElement(children: .combine)
                }
            }
        }
        .tarjeta()
    }

    private var habitacion: some View {
        Button {
            ruta.append(.habitacion)
        } label: {
            HStack(spacing: 12) {
                IconoCirculo(sistema: "bed.double", tono: datos.ingresoActual == nil ? .neutro : .info, tamano: 40)
                VStack(alignment: .leading, spacing: 2) {
                    if let cama = datos.ingresoActual?.cama {
                        Text("\(cama.codigoHabitacion) · \(cama.nombreCama)")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.cmoTinta)
                        Text(planta(cama))
                            .font(.caption)
                            .foregroundStyle(Color.cmoTintaSuave)
                    } else {
                        Text("Mi habitación").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                        Text("No estás hospitalizado").font(.caption).foregroundStyle(Color.cmoTintaSuave)
                    }
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            .tarjeta(relleno: 12)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("inicio.habitacion")
    }

    private var hospital: some View {
        Button {
            ruta.append(.hospital)
        } label: {
            HStack(spacing: 12) {
                IconoCirculo(sistema: "cross.case", tono: .neutro, tamano: 40)
                Text("Información del hospital").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            .tarjeta(relleno: 12)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("inicio.hospital")
    }

    private func planta(_ cama: CamaResumen) -> String {
        guard let habitacion = cama.habitacion else { return "" }
        return [habitacion.planta.map { "Planta \($0)" }, habitacion.tipoNombre].compactMap { $0 }.joined(separator: " · ")
    }
}
