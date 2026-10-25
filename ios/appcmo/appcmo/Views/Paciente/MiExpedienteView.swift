//
//  MiExpedienteView.swift
//  appcmo
//

import SwiftUI

/// Mi expediente (pantalla 07, HU-43): mi historial, mis consultas con diagnóstico y recetas, y mis citas pasadas.
/// Todo es de solo lectura y solo trae mis datos.
struct MiExpedienteView: View {
    @Environment(PacienteDatos.self) private var datos
    /// Si hay una fecha elegida, solo se muestran las consultas de ese día.
    @State private var fechaFiltro: Date?

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                EncabezadoPantalla("Mi expediente", subtitulo: datos.expediente.valor?.numeroExpediente) {
                    CampanaAvisos()
                }
                .padding(.horizontal, 16)

                EstadoCarga(carga: datos.expediente, reintentar: { Task { await datos.cargarExpediente() } }) { expediente in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 14) {
                            datosClinicos(expediente)
                            historial(expediente)
                            consultas(expediente)
                            otrasCitas(expediente)
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 24)
                    }
                    .refreshable { await datos.cargarExpediente() }
                }
            }
            .padding(.top, 8)
            .background(Color.cmoFondo)
            .toolbar(.hidden, for: .navigationBar)
            .onAppear { Task { await datos.cargarExpediente() } }
        }
    }

    // MARK: - Secciones

    private func datosClinicos(_ expediente: Expediente) -> some View {
        HStack(spacing: 10) {
            dato("Tipo de sangre", expediente.tipoSangre ?? "—", .info)
            dato("Alergias", (expediente.alergias?.isEmpty == false ? expediente.alergias : nil) ?? "Ninguna", .error)
        }
    }

    private func dato(_ titulo: String, _ valor: String, _ tono: TonoEstado) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(titulo).font(.caption)
            Text(valor).font(.headline)
        }
        .foregroundStyle(tono.primerPlano)
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(tono.fondo, in: .rect(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }

    private func historial(_ expediente: Expediente) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Historial").font(.headline).foregroundStyle(Color.cmoTinta)
            VStack(spacing: 0) {
                FilaDato("Antecedentes personales", expediente.historial?.antecedentesPersonales)
                Divider()
                FilaDato("Antecedentes familiares", expediente.historial?.antecedentesFamiliares)
            }
            .tarjeta(relleno: 0)
        }
    }

    private func consultas(_ expediente: Expediente) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            let todas = expediente.consultas.filter { $0.consulta?.fechaHoraFin != nil }
            let dia = fechaFiltro.map { Formato.fechaAPI($0) }
            let lista = dia == nil ? todas : todas.filter { $0.fecha == dia }
            HStack {
                Text("Consultas").font(.headline).foregroundStyle(Color.cmoTinta)
                Spacer()
                if !todas.isEmpty {
                    Button {
                        // Al abrir el buscador arranca en la fecha de la consulta más reciente.
                        fechaFiltro = fechaFiltro == nil ? (Formato.fecha(desdeAPI: todas.first?.fecha) ?? Date()) : nil
                    } label: {
                        Label(fechaFiltro == nil ? "Buscar por fecha" : "Quitar filtro", systemImage: fechaFiltro == nil ? "calendar" : "xmark.circle")
                    }
                    .font(.subheadline.weight(.semibold))
                    .accessibilityIdentifier("expediente.buscarFecha")
                }
            }
            if fechaFiltro != nil {
                DatePicker(
                    "Fecha de la consulta",
                    selection: Binding(get: { fechaFiltro ?? Date() }, set: { fechaFiltro = $0 }),
                    displayedComponents: .date
                )
                .environment(\.locale, Locale(identifier: "es"))
                .environment(\.timeZone, Formato.zona)
                .font(.subheadline)
                .tarjeta(relleno: 12)
                .accessibilityIdentifier("expediente.fecha")
            }
            if lista.isEmpty {
                if fechaFiltro != nil {
                    InicioVacio(icono: "calendar", titulo: "Sin consultas ese día", detalle: "Prueba con otra fecha o quita el filtro para ver todas tus consultas.")
                } else {
                    InicioVacio(icono: "stethoscope", titulo: "Aún no tienes consultas", detalle: "Cuando termines una consulta aparecerá aquí con su diagnóstico y tus recetas.")
                }
            }
            ForEach(lista) { cita in
                if let consulta = cita.consulta {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .firstTextBaseline) {
                            Text(Formato.fechaCorta(cita.fecha)).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                            Spacer()
                            EstadoChip(texto: "Atendida", tono: .exito)
                        }
                        if let medico = cita.medico {
                            Text([medico.nombreCompleto, medico.cargo].compactMap { $0 }.joined(separator: " · "))
                                .font(.caption)
                                .foregroundStyle(Color.cmoTintaSuave)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Diagnóstico").font(.caption).foregroundStyle(Color.cmoTintaSuave)
                            Text(consulta.diagnostico ?? "Sin diagnóstico").font(.subheadline).foregroundStyle(Color.cmoTinta)
                        }
                        ForEach(consulta.recetas ?? []) { receta in
                            HStack(spacing: 10) {
                                Image(systemName: "pills").foregroundStyle(Color.cmoMarca).accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text("\(receta.medicamento) \(receta.dosis)").font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                                    Text("\(receta.frecuencia) · \(receta.duracion)").font(.caption).foregroundStyle(Color.cmoTintaSuave)
                                    if let indicaciones = receta.indicaciones, !indicaciones.isEmpty {
                                        Text(indicaciones).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                                    }
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(10)
                            .background(Color.cmoMarcaSuave, in: .rect(cornerRadius: 10))
                            .accessibilityElement(children: .combine)
                        }
                    }
                    .tarjeta()
                    .accessibilityIdentifier("expediente.consulta")
                }
            }
        }
    }

    /// Citas pasadas que no llegaron a consulta (canceladas o a las que no asistió).
    @ViewBuilder
    private func otrasCitas(_ expediente: Expediente) -> some View {
        let lista = expediente.citas
            .filter { $0.consulta == nil && ($0.estado == .cancelada || $0.estado == .noAsistio) }
            .sorted { ($0.fecha ?? "") > ($1.fecha ?? "") }
        if !lista.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Otras citas").font(.headline).foregroundStyle(Color.cmoTinta)
                ForEach(lista) { cita in
                    HStack(spacing: 12) {
                        IconoCirculo(sistema: "calendar", tono: .neutro, tamano: 36)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(Formato.fechaCorta(cita.fecha)).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                            Text(cita.motivo).font(.caption).foregroundStyle(Color.cmoTintaSuave).lineLimit(1)
                        }
                        Spacer(minLength: 8)
                        EstadoChip(texto: cita.estado.nombrePaciente(esHoy: false), tono: cita.estado.tonoPaciente)
                    }
                    .tarjeta(relleno: 12)
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }
}
