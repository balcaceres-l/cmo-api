//
//  InicioView.swift
//  appcmo
//

import SwiftUI

/// Dashboard del médico: saludo, resumen del día, la consulta que sigue y la agenda de hoy.
struct InicioView: View {
    @Environment(SessionStore.self) private var store
    @State private var viewModel = InicioViewModel()
    @State private var destino: String?
    @State private var otraEnAtencion: Cita?
    @State private var mensajeError: String?
    @State private var trabajando = false

    let verAgenda: () -> Void

    private var usuario: Usuario? { store.sesion?.usuario }

    var body: some View {
        NavigationStack {
            EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { _ in
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        encabezado
                        resumen
                        destacada
                        agendaDeHoy
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 24)
                }
                .refreshable { await viewModel.cargar() }
            }
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: Cita.self) { cita in
                CitaDetalleView(cita: cita)
            }
            .alertasDeConsulta(
                destino: $destino,
                enAtencion: $otraEnAtencion,
                error: $mensajeError,
                idUsuario: usuario?.id ?? "",
                continuar: { otra in Task { await continuar(otra) } }
            )
            .onAppear { Task { await viewModel.cargar() } }
        }
    }

    // MARK: - Secciones

    private var encabezado: some View {
        HStack(spacing: 12) {
            AvatarIniciales(nombre: usuario?.nombreCompleto ?? "", tono: .neutro, tamano: 46)
            VStack(alignment: .leading, spacing: 1) {
                Text(Formato.saludo())
                    .font(.footnote)
                    .foregroundStyle(Color.cmoTintaSuave)
                Text(usuario?.nombreCompleto ?? "Médico")
                    .font(.title3.bold())
                    .foregroundStyle(Color.cmoTinta)
                    .accessibilityIdentifier("inicio.nombre")
            }
            Spacer(minLength: 0)
            CampanaAvisos()
        }
    }

    private var resumen: some View {
        HStack(spacing: 10) {
            dato(viewModel.atendidas, "Atendidas", .exito)
            dato(viewModel.enSala, "En sala", .info)
            dato(viewModel.porVenir, "Por llegar", .advertencia)
        }
        .accessibilityIdentifier("inicio.resumen")
    }

    private func dato(_ valor: Int, _ titulo: String, _ tono: EstadoChip.Tono) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(valor)")
                .font(.title2.bold())
                .foregroundStyle(tono.primerPlano)
            Text(titulo)
                .font(.caption)
                .foregroundStyle(tono.primerPlano)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tono.fondo, in: .rect(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var destacada: some View {
        if let cita = viewModel.destacada {
            let enCurso = cita.estado == .enAtencion
            VStack(alignment: .leading, spacing: 12) {
                Text("\(enCurso ? "En consulta" : "Siguiente") · \(Formato.hora(cita.horaInicio))")
                    .font(.caption.weight(.semibold))
                    .opacity(0.85)
                HStack(spacing: 12) {
                    Text(Formato.iniciales(cita.paciente?.nombreCompleto ?? "P"))
                        .font(.footnote.weight(.semibold))
                        .frame(width: 40, height: 40)
                        .background(Color.cmoSobreMarca.opacity(0.18), in: .circle)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(cita.paciente?.nombreCompleto ?? "Paciente")
                            .font(.headline)
                        Text([cita.motivo, cita.consultorio.map { "Consultorio \($0.codigo)" }].compactMap { $0 }.joined(separator: " · "))
                            .font(.caption)
                            .opacity(0.85)
                            .lineLimit(2)
                    }
                    Spacer(minLength: 0)
                }
                Button {
                    Task { await accionPrincipal(cita) }
                } label: {
                    HStack(spacing: 8) {
                        if trabajando { ProgressView().tint(Color.cmoMarca) } else { Image(systemName: "stethoscope") }
                        Text(enCurso ? "Continuar consulta" : "Iniciar consulta")
                    }
                    .font(.subheadline.weight(.semibold))
                    .frame(maxWidth: .infinity, minHeight: 46)
                    .foregroundStyle(Color.cmoMarca)
                    .background(Color.cmoSobreMarca, in: .rect(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .disabled(trabajando)
                .accessibilityIdentifier("inicio.iniciarConsulta")
            }
            .foregroundStyle(Color.cmoSobreMarca)
            .padding(16)
            .background(Color.cmoMarca, in: .rect(cornerRadius: 20))
        } else {
            VStack(spacing: 8) {
                Image(systemName: "checkmark.circle")
                    .font(.largeTitle)
                    .foregroundStyle(Color.cmoExito)
                Text("No tienes más consultas pendientes hoy")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.cmoTinta)
                Text("Cuando agenden una cita para hoy aparecerá aquí.")
                    .font(.footnote)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .tarjeta(relleno: 20)
        }
    }

    private var agendaDeHoy: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Agenda de hoy")
                    .font(.headline)
                    .foregroundStyle(Color.cmoTinta)
                Spacer()
                Button("Ver todo", action: verAgenda)
                    .font(.subheadline.weight(.semibold))
                    .accessibilityIdentifier("inicio.verTodo")
            }
            let citas = viewModel.citasDeHoy
            if citas.isEmpty {
                Text("No hay citas para hoy.")
                    .font(.subheadline)
                    .foregroundStyle(Color.cmoTintaSuave)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .tarjeta()
            }
            ForEach(citas.prefix(4)) { cita in
                NavigationLink(value: cita) {
                    CitaTarjeta(cita: cita)
                }
                .buttonStyle(.plain)
            }
            if citas.count > 4 {
                Button("Ver las \(citas.count) citas de hoy", action: verAgenda)
                    .font(.subheadline)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - Acciones

    /// Inicia (o continúa) la consulta de la cita destacada, con la misma revisión de "una a la vez" del detalle.
    private func accionPrincipal(_ cita: Cita) async {
        trabajando = true
        defer { trabajando = false }
        let accion = CitaDetalleViewModel(cita: cita)
        if cita.estado == .enAtencion {
            await accion.verConsulta()
        } else {
            await accion.iniciarConsulta()
        }
        destino = accion.consultaDestino
        otraEnAtencion = accion.enAtencion
        mensajeError = accion.error
    }

    private func continuar(_ otra: Cita) async {
        trabajando = true
        defer { trabajando = false }
        let accion = CitaDetalleViewModel(cita: otra)
        await accion.continuarConsulta(de: otra)
        destino = accion.consultaDestino
        mensajeError = accion.error
    }
}
