//
//  ExpedienteView.swift
//  appcmo
//

import SwiftUI

/// Expediente clínico del paciente (pantalla 07): Resumen, Consultas y Recetas.
/// Solo lo abre el médico tratante; para cualquier otro la API responde 403 y se muestra "Sin acceso".
struct ExpedienteView: View {
    @Environment(SessionStore.self) private var store
    @State private var viewModel: ExpedienteViewModel
    @State private var seguimiento: Paciente?

    init(paciente: Paciente) {
        _viewModel = State(initialValue: ExpedienteViewModel(paciente: paciente))
    }

    var body: some View {
        Group {
            if viewModel.sinAcceso {
                sinAcceso
            } else {
                EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { expediente in
                    contenido(expediente)
                }
            }
        }
        .background(Color.cmoFondo)
        .navigationTitle("Expediente")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.cargar() }
        .sheet(item: $seguimiento) { paciente in
            SeguimientoView(paciente: paciente, idMedico: store.sesion?.usuario.id ?? "", alAgendar: {
                Task { await viewModel.cargar() }
            })
        }
    }

    // MARK: - Sin acceso

    private var sinAcceso: some View {
        VStack(spacing: 14) {
            IconoCirculo(sistema: "lock.fill", tono: .error, tamano: 72)
            Text("Sin acceso")
                .font(.title3.bold())
                .foregroundStyle(Color.cmoTinta)
            Text("Solo el médico tratante puede ver el expediente de \(viewModel.paciente.nombreCompleto).")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.cmoTintaSuave)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityIdentifier("expediente.sinAcceso")
    }

    // MARK: - Contenido

    private func contenido(_ expediente: Expediente) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                cabecera(expediente)

                Picker("Sección", selection: $viewModel.seccion) {
                    ForEach(ExpedienteViewModel.Seccion.allCases) { seccion in
                        Text(seccion.rawValue).tag(seccion)
                    }
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("expediente.secciones")

                if let aviso = viewModel.aviso, !viewModel.editando {
                    AvisoBanner(aviso: aviso)
                }

                switch viewModel.seccion {
                case .resumen: resumen(expediente)
                case .consultas: consultas(expediente)
                case .recetas: recetas(expediente)
                }

                BotonPrincipal(titulo: "Agendar seguimiento") {
                    seguimiento = viewModel.paciente
                }
                .accessibilityIdentifier("expediente.seguimiento")
            }
            .padding(16)
        }
        .refreshable { await viewModel.cargar() }
    }

    private func cabecera(_ expediente: Expediente) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                AvatarIniciales(nombre: expediente.nombreCompleto, tono: .info, tamano: 56)
                VStack(alignment: .leading, spacing: 2) {
                    Text(expediente.nombreCompleto)
                        .font(.headline)
                        .foregroundStyle(Color.cmoTinta)
                    Text("\(expediente.numeroExpediente) · \(edad(expediente.fechaNacimiento)) · \(expediente.sexoTexto)")
                        .font(.caption)
                        .foregroundStyle(Color.cmoTintaSuave)
                }
                Spacer(minLength: 0)
            }
            HStack(spacing: 8) {
                if let sangre = expediente.tipoSangre {
                    EstadoChip(texto: "Sangre \(sangre)", tono: .error)
                }
                if let alergias = expediente.alergias, !alergias.isEmpty {
                    EstadoChip(texto: "Alergias: \(alergias)", tono: .advertencia)
                }
                if let ingreso = expediente.ingresoActivo {
                    EstadoChip(texto: ingreso.cama.map { "Ingresado · \($0.codigo)" } ?? "Ingresado", tono: .info)
                }
            }
        }
        .tarjeta()
    }

    private func edad(_ nacimiento: String) -> String {
        guard let fecha = Formato.fecha(desdeAPI: nacimiento),
              let anios = Formato.calendario.dateComponents([.year], from: fecha, to: Date()).year else { return "" }
        return "\(anios) años"
    }

    // MARK: - Resumen

    private func resumen(_ expediente: Expediente) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            grupo("Datos personales") {
                dato("DUI", expediente.dui)
                dato("Nacimiento", Formato.fechaCorta(expediente.fechaNacimiento))
                dato("Teléfono", expediente.telefono)
                dato("Dirección", expediente.direccion)
            }

            antecedentes(expediente)

            if let ingreso = expediente.ingresoActivo {
                grupo("Ingreso actual") {
                    dato("Ubicación", ingreso.cama?.ubicacion)
                    dato("Desde", Formato.marcaTiempo(ingreso.fechaIngreso))
                    dato("Motivo", ingreso.motivo)
                }
            }

            if !expediente.ordenesLaboratorio.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Exámenes de laboratorio").font(.headline).foregroundStyle(Color.cmoTinta)
                    ForEach(expediente.ordenesLaboratorio) { orden in
                        OrdenFila(orden: orden).tarjeta()
                    }
                }
            }
        }
    }

    private func antecedentes(_ expediente: Expediente) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Antecedentes").font(.headline).foregroundStyle(Color.cmoTinta)
                Spacer()
                if !viewModel.editando {
                    Button("Editar") { viewModel.empezarEdicion() }
                        .font(.subheadline.weight(.semibold))
                        .accessibilityIdentifier("expediente.editarAntecedentes")
                }
            }
            if viewModel.editando {
                VStack(alignment: .leading, spacing: 12) {
                    if let aviso = viewModel.aviso { AvisoBanner(aviso: aviso) }
                    CampoFormulario(titulo: "Personales") {
                        TextField("Enfermedades, cirugías…", text: $viewModel.personales, axis: .vertical)
                            .lineLimit(2...6)
                            .padding(.vertical, 10)
                            .accessibilityIdentifier("expediente.personales")
                    }
                    CampoFormulario(titulo: "Familiares") {
                        TextField("Antecedentes de la familia", text: $viewModel.familiares, axis: .vertical)
                            .lineLimit(2...6)
                            .padding(.vertical, 10)
                            .accessibilityIdentifier("expediente.familiares")
                    }
                    HStack(spacing: 10) {
                        BotonSecundario(titulo: "Cancelar") { viewModel.cancelarEdicion() }
                        BotonPrincipal(titulo: "Guardar", cargando: viewModel.guardando) {
                            Task { await viewModel.guardarAntecedentes() }
                        }
                        .accessibilityIdentifier("expediente.guardarAntecedentes")
                    }
                }
                .tarjeta()
            } else {
                VStack(spacing: 0) {
                    dato("Personales", expediente.historial?.antecedentesPersonales)
                    Divider()
                    dato("Familiares", expediente.historial?.antecedentesFamiliares)
                }
                .tarjeta(relleno: 0)
            }
        }
    }

    // MARK: - Consultas

    private func consultas(_ expediente: Expediente) -> some View {
        VStack(spacing: 10) {
            let lista = expediente.consultas
            if lista.isEmpty {
                InicioVacio(icono: "stethoscope", titulo: "Sin consultas", detalle: "Las consultas atendidas aparecerán aquí.")
            }
            ForEach(lista) { cita in
                if let consulta = cita.consulta {
                    HStack(alignment: .top, spacing: 12) {
                        IconoCirculo(sistema: "stethoscope", tono: consulta.fechaHoraFin == nil ? .info : .exito)
                        VStack(alignment: .leading, spacing: 3) {
                            HStack(alignment: .firstTextBaseline) {
                                Text(Formato.fechaCorta(cita.fecha))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(Color.cmoTinta)
                                Spacer(minLength: 8)
                                if consulta.fechaHoraFin == nil {
                                    EstadoChip(texto: "En curso", tono: .info)
                                }
                            }
                            Text(consulta.diagnostico ?? "Sin diagnóstico")
                                .font(.subheadline)
                                .foregroundStyle(Color.cmoTinta)
                            Text(consulta.motivo ?? cita.motivo)
                                .font(.caption)
                                .foregroundStyle(Color.cmoTintaSuave)
                            if let notas = consulta.notas, !notas.isEmpty {
                                Text(notas).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                            }
                        }
                    }
                    .tarjeta()
                }
            }
        }
    }

    // MARK: - Recetas

    private func recetas(_ expediente: Expediente) -> some View {
        VStack(spacing: 10) {
            let lista = expediente.recetas
            if lista.isEmpty {
                InicioVacio(icono: "pills", titulo: "Sin recetas", detalle: "Las recetas de las consultas aparecerán aquí.")
            }
            ForEach(Array(lista.enumerated()), id: \.offset) { _, elemento in
                HStack(alignment: .top, spacing: 12) {
                    IconoCirculo(sistema: "pills", tono: .exito)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(elemento.receta.medicamento)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color.cmoTinta)
                        Text("\(elemento.receta.dosis) · \(elemento.receta.frecuencia) · \(elemento.receta.duracion)")
                            .font(.caption)
                            .foregroundStyle(Color.cmoTinta)
                        if let indicaciones = elemento.receta.indicaciones, !indicaciones.isEmpty {
                            Text(indicaciones).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                        }
                        Text(Formato.fechaCorta(elemento.fecha))
                            .font(.caption2)
                            .foregroundStyle(Color.cmoTintaSuave)
                    }
                    Spacer(minLength: 0)
                }
                .tarjeta()
            }
        }
    }

    // MARK: - Piezas

    private func grupo<Contenido: View>(_ titulo: String, @ViewBuilder _ contenido: () -> Contenido) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(titulo).font(.headline).foregroundStyle(Color.cmoTinta)
            VStack(spacing: 0) { contenido() }.tarjeta(relleno: 0)
        }
    }

    private func dato(_ titulo: String, _ valor: String?) -> some View {
        HStack(alignment: .top) {
            Text(titulo).foregroundStyle(Color.cmoTintaSuave)
            Spacer(minLength: 16)
            Text(valor.flatMap { $0.isEmpty ? nil : $0 } ?? "—").foregroundStyle(Color.cmoTinta).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .padding(14)
    }
}
