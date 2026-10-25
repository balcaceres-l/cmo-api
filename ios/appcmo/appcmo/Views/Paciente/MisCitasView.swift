//
//  MisCitasView.swift
//  appcmo
//

import SwiftUI

/// Mis citas (pantalla 06): se filtran por Hoy y por estado (Agendadas, Atendidas, Canceladas y Solicitadas). Menos Hoy,
/// cada filtro se puede acotar a un día. Reprogramar o cancelar lo hace recepción (HU-37).
struct MisCitasView: View {
    private enum Filtro: String, CaseIterable, Identifiable {
        case hoy = "Hoy", agendadas = "Agendadas", atendidas = "Atendidas", canceladas = "Canceladas", solicitadas = "Solicitadas"
        var id: String { rawValue }
    }

    @Environment(PacienteDatos.self) private var datos
    @State private var filtro: Filtro = .hoy
    /// Si hay una fecha elegida, solo se muestran las citas de ese día (no aplica a "Hoy").
    @State private var fechaFiltro: Date?
    @State private var ruta: [Bool] = []

    private func citas(_ filtro: Filtro) -> [Cita] {
        switch filtro {
        case .hoy: datos.citasDeHoy
        case .agendadas: datos.agendadas
        case .atendidas: datos.atendidas
        case .canceladas: datos.canceladas
        case .solicitadas: datos.solicitudes
        }
    }

    /// El día de una cita (de una solicitud, el día en que se envió).
    private func dia(_ cita: Cita) -> String? {
        cita.estado == .solicitada ? Formato.diaDeMarca(cita.createdAt) : cita.fecha
    }

    private var visibles: [Cita] {
        let lista = citas(filtro)
        guard filtro != .hoy, let fecha = fechaFiltro else { return lista }
        let dia = Formato.fechaAPI(fecha)
        return lista.filter { self.dia($0) == dia }
    }

    var body: some View {
        NavigationStack(path: $ruta) {
            VStack(alignment: .leading, spacing: 14) {
                EncabezadoPantalla("Mis citas") {
                    HStack(spacing: 8) {
                        CampanaAvisos()
                        Button { ruta.append(true) } label: {
                            Image(systemName: "plus")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(Color.cmoSobreMarca)
                                .frame(width: 40, height: 40)
                                .background(Color.cmoMarca, in: .circle)
                        }
                        .accessibilityLabel("Solicitar atención")
                        .accessibilityIdentifier("citas.solicitar")
                    }
                }
                .padding(.horizontal, 16)

                filtros

                EstadoCarga(carga: datos.citas, reintentar: { Task { await datos.cargarCitas() } }) { _ in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 10) {
                            if filtro != .hoy, !citas(filtro).isEmpty {
                                FiltroPorFecha(fecha: $fechaFiltro, inicial: Formato.fecha(desdeAPI: citas(filtro).first.flatMap(dia)) ?? Date())
                            }
                            lista
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                    .refreshable { await datos.cargarCitas() }
                    .accessibilityIdentifier("citas.lista")
                }
            }
            .padding(.top, 8)
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Bool.self) { _ in SolicitudAtencionView() }
            .onAppear { Task { await datos.cargarCitas() } }
            .onChange(of: filtro) { fechaFiltro = nil }
        }
    }

    /// Los cinco filtros siempre visibles (sin desplazarse): tres arriba y dos abajo, todos del mismo ancho.
    private var filtros: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                boton(.hoy)
                boton(.agendadas)
                boton(.atendidas)
            }
            HStack(spacing: 8) {
                boton(.canceladas)
                boton(.solicitadas)
            }
        }
        .padding(.horizontal, 16)
    }

    private func boton(_ opcion: Filtro) -> some View {
        let elegido = filtro == opcion
        return Button {
            filtro = opcion
        } label: {
            Text("\(opcion.rawValue) \(citas(opcion).count)")
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: 40)
                .foregroundStyle(elegido ? Color.cmoSobreMarca : Color.cmoTinta)
                .background(elegido ? Color.cmoMarca : Color.cmoSuperficie, in: .rect(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(elegido ? Color.cmoMarca : Color.cmoLinea))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(elegido ? .isSelected : [])
        .accessibilityIdentifier("citas.filtro.\(opcion.rawValue)")
    }

    @ViewBuilder
    private var lista: some View {
        let citas = visibles
        if citas.isEmpty {
            vacio
        }
        ForEach(citas) { cita in
            if cita.estado == .solicitada {
                solicitud(cita)
            } else {
                CitaPacienteTarjeta(cita: cita)
            }
        }
    }

    private var vacio: some View {
        let conFecha = filtro != .hoy && fechaFiltro != nil
        let (titulo, detalle): (String, String) = {
            if conFecha { return ("Sin citas ese día", "Prueba con otra fecha o quita el filtro.") }
            switch filtro {
            case .hoy: return ("No tienes citas hoy", "Las citas de hoy aparecen aquí.")
            case .agendadas: return ("No tienes citas agendadas", "Pide atención con el botón + y recepción te asignará médico, fecha y hora.")
            case .atendidas: return ("Aún no tienes citas atendidas", "Aquí aparecerán las citas a las que ya asististe.")
            case .canceladas: return ("No tienes citas canceladas", "Aquí aparecerán las citas canceladas o a las que no asististe.")
            case .solicitadas: return ("No tienes solicitudes pendientes", "Las solicitudes de atención que envíes aparecen aquí hasta que recepción las confirme.")
            }
        }()
        return InicioVacio(icono: "calendar", titulo: titulo, detalle: detalle)
    }

    /// Una solicitud que recepción todavía no confirma: cuando la confirma pasa a Agendadas con su fecha y hora.
    private func solicitud(_ solicitud: Cita) -> some View {
        HStack(spacing: 12) {
            IconoCirculo(sistema: "clock", tono: .advertencia, tamano: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(solicitud.motivo).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta).lineLimit(2)
                Text("Enviada \(Formato.enviada(solicitud.createdAt))").font(.caption).foregroundStyle(Color.cmoTintaSuave)
            }
            Spacer(minLength: 8)
            EstadoChip(texto: "Pendiente", tono: .advertencia)
        }
        .tarjeta(relleno: 12)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("citas.solicitudPendiente")
    }
}

/// Una cita del paciente: fecha, hora, consultorio y el nombre y cargo de su médico, sin más datos de él.
struct CitaPacienteTarjeta: View {
    @Environment(PacienteDatos.self) private var datos
    let cita: Cita

    private var chip: (texto: String, tono: TonoEstado) {
        if datos.quedoSinAtender(cita) { return ("No se atendió", .neutro) }
        return (cita.estado.nombrePaciente(esHoy: datos.esHoy(cita)), cita.estado.tonoPaciente)
    }

    var body: some View {
        let estado = chip
        HStack(alignment: .center, spacing: 12) {
            VStack(spacing: 0) {
                Text(Formato.numeroDelDia(cita.fecha)).font(.title3.bold())
                Text(Formato.mesAbreviado(cita.fecha)).font(.caption2.weight(.semibold))
            }
            .foregroundStyle(estado.tono.primerPlano)
            .frame(width: 52, height: 56)
            .background(estado.tono.fondo, in: .rect(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 2) {
                Text(cita.medico?.nombreCompleto ?? cita.motivo)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                    .lineLimit(1)
                if let cargo = cita.medico?.cargo {
                    Text(cargo).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                }
                Text([Formato.hora(cita.horaInicio), cita.consultorio.map { "Consultorio \($0.codigo)" }].compactMap { $0 }.joined(separator: " · "))
                    .font(.caption)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            Spacer(minLength: 8)
            EstadoChip(texto: estado.texto, tono: estado.tono)
        }
        .tarjeta(relleno: 12)
        .opacity(cita.estado.estaInactiva ? 0.65 : 1)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("citas.fila")
    }
}

/// Pedir atención (pantalla 05): el motivo queda como solicitud "Pendiente" hasta que recepción la confirme.
struct SolicitudAtencionView: View {
    @Environment(PacienteDatos.self) private var datos
    @State private var viewModel = SolicitudAtencionViewModel()

    var body: some View {
        @Bindable var viewModel = viewModel
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Cuéntanos qué necesitas")
                    .font(.subheadline)
                    .foregroundStyle(Color.cmoTintaSuave)

                AvisoBanner(aviso: .informacion("Recepción revisará tu solicitud y te asignará médico, consultorio, fecha y hora."))

                if let aviso = viewModel.aviso {
                    AvisoBanner(aviso: aviso)
                }

                VStack(alignment: .leading, spacing: 6) {
                    CampoFormulario(titulo: "Motivo de la consulta") {
                        TextField("Ejemplo: dolor de cabeza desde hace tres días", text: $viewModel.motivo, axis: .vertical)
                            .lineLimit(4...8)
                            .padding(.vertical, 12)
                            .accessibilityIdentifier("solicitud.motivo")
                    }
                    Text("\(viewModel.motivo.count) de \(SolicitudAtencionViewModel.limite) caracteres")
                        .font(.caption)
                        .foregroundStyle(Color.cmoTintaSuave)
                }

                BotonPrincipal(titulo: "Enviar solicitud", cargando: viewModel.enviando, deshabilitado: !viewModel.puedeEnviar) {
                    Task {
                        if await viewModel.enviar() { await datos.cargarCitas() }
                    }
                }
                .accessibilityIdentifier("solicitud.enviar")
            }
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(Color.cmoFondo)
        .navigationTitle("Solicitar atención")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
    }
}
