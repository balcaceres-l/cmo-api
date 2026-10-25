//
//  CitaDetalleView.swift
//  appcmo
//

import SwiftUI

struct CitaDetalleView: View {
    @Environment(SessionStore.self) private var store
    @State private var viewModel: CitaDetalleViewModel
    @State private var seguimiento: Paciente?

    init(cita: Cita) {
        _viewModel = State(initialValue: CitaDetalleViewModel(cita: cita))
    }

    private var cita: Cita { viewModel.cita }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    AvatarIniciales(nombre: cita.paciente?.nombreCompleto ?? "P", tono: cita.estado.tonoMedico, tamano: 48)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(cita.paciente?.nombreCompleto ?? "Paciente")
                            .font(.headline)
                            .foregroundStyle(Color.cmoTinta)
                        Text(cita.paciente?.numeroExpediente ?? "")
                            .font(.caption)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                    Spacer(minLength: 8)
                    EstadoChip(texto: cita.estado.nombreMedico, tono: cita.estado.tonoMedico)
                }
                .tarjeta()

                VStack(spacing: 0) {
                    fila("Fecha", Formato.fechaLarga(cita.fecha))
                    Divider()
                    fila("Hora", Formato.rangoHoras(cita.horaInicio, cita.horaFin))
                    Divider()
                    fila("Consultorio", cita.consultorio.map { "\($0.codigo) · \($0.nombre)" })
                    Divider()
                    fila("Motivo", cita.motivo)
                    if let motivo = cita.motivoCancelacion, cita.estado == .cancelada {
                        Divider()
                        fila("Cancelación", motivo)
                    }
                }
                .tarjeta(relleno: 0)

                if cita.estado.sePuedeAtender {
                    BotonPrincipal(titulo: "Iniciar consulta", cargando: viewModel.trabajando) {
                        Task { await viewModel.iniciarConsulta() }
                    }
                    .accessibilityIdentifier("cita.iniciarConsulta")
                }
                if cita.estado.tieneConsulta {
                    BotonPrincipal(titulo: cita.estado == .enAtencion ? "Continuar consulta" : "Ver consulta", cargando: viewModel.trabajando) {
                        Task { await viewModel.verConsulta() }
                    }
                    .accessibilityIdentifier("cita.verConsulta")
                }
                if let paciente = cita.paciente {
                    BotonSecundario(titulo: "Agendar seguimiento", sistema: "calendar.badge.plus") {
                        seguimiento = paciente
                    }
                    .accessibilityIdentifier("cita.seguimiento")
                }
            }
            .padding(16)
        }
        .background(Color.cmoFondo)
        .navigationTitle("Cita")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { Task { await viewModel.recargar() } }
        .alertasDeConsulta(
            destino: $viewModel.consultaDestino,
            enAtencion: $viewModel.enAtencion,
            error: $viewModel.error,
            idUsuario: store.sesion?.usuario.id ?? "",
            continuar: { otra in Task { await viewModel.continuarConsulta(de: otra) } }
        )
        .sheet(item: $seguimiento) { paciente in
            SeguimientoView(paciente: paciente, idMedico: store.sesion?.usuario.id ?? "", alAgendar: {})
        }
    }

    private func fila(_ titulo: String, _ valor: String?) -> some View {
        HStack(alignment: .top) {
            Text(titulo).foregroundStyle(Color.cmoTintaSuave)
            Spacer(minLength: 16)
            Text(valor ?? "—")
                .foregroundStyle(Color.cmoTinta)
                .multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .padding(14)
    }
}
