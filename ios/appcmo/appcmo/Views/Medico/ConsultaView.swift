//
//  ConsultaView.swift
//  appcmo
//

import SwiftUI

/// Consulta general: diagnóstico, recetas y exámenes de laboratorio (HU-40 y HU-47).
/// Todo lo que el médico escribe es un borrador en este iPhone; se envía a la API al finalizar y entonces
/// la consulta queda de solo lectura.
struct ConsultaView: View {
    private enum Hoja: String, Identifiable {
        case receta, examen
        var id: String { rawValue }
    }

    @State private var viewModel: ConsultaViewModel
    @State private var hoja: Hoja?
    @State private var confirmandoFin = false
    @State private var confirmandoDescarte = false

    init(idConsulta: String, idUsuario: String) {
        _viewModel = State(initialValue: ConsultaViewModel(idConsulta: idConsulta, idUsuario: idUsuario))
    }

    var body: some View {
        EstadoCarga(carga: viewModel.carga, reintentar: { Task { await viewModel.cargar() } }) { consulta in
            contenido(consulta)
        }
        .background(Color.cmoFondo)
        .navigationTitle("Consulta")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            // Solo la primera vez: volver a cargar pisaría lo que el médico está escribiendo.
            if viewModel.carga.valor == nil { await viewModel.cargar() }
        }
        // Cada vez que escribe, el borrador se guarda cifrado en el iPhone.
        .onChange(of: viewModel.textos) { viewModel.persistir() }
        .sheet(item: $hoja) { hoja in
            switch hoja {
            case .receta:
                RecetaFormView { receta in viewModel.agregarReceta(receta) }
            case .examen:
                OrdenFormView(
                    cargarExamenes: { await viewModel.examenes() },
                    solicitar: { examen, observaciones in viewModel.agregarExamen(examen, observaciones: observaciones) }
                )
            }
        }
        .confirmationDialog("¿Finalizar la consulta?", isPresented: $confirmandoFin, titleVisibility: .visible) {
            Button("Finalizar y enviar", role: .destructive) {
                Task { await viewModel.finalizar() }
            }
            Button("Cancelar", role: .cancel) {}
        } message: {
            Text(mensajeDeFinalizar)
        }
        .confirmationDialog("¿Descartar el borrador?", isPresented: $confirmandoDescarte, titleVisibility: .visible) {
            Button("Descartar borrador", role: .destructive) { viewModel.descartarBorrador() }
            Button("Cancelar", role: .cancel) {}
        } message: {
            Text("Se borra lo que escribiste en este iPhone y no enviaste todavía. Lo que ya esté en el servidor no cambia.")
        }
    }

    private var mensajeDeFinalizar: String {
        let recetas = viewModel.recetasBorrador.count
        let examenes = viewModel.examenesBorrador.count
        var partes = ["el diagnóstico y las notas"]
        if recetas > 0 { partes.append("\(recetas) \(recetas == 1 ? "receta" : "recetas")") }
        if examenes > 0 { partes.append("\(examenes) \(examenes == 1 ? "examen" : "exámenes")") }
        let lista = partes.count > 1 ? partes.dropLast().joined(separator: ", ") + " y " + partes[partes.count - 1] : partes[0]
        var texto = "Se enviarán \(lista)."
        if examenes > 0 { texto += examenes == 1 ? " El examen llegará al laboratorio." : " Los exámenes llegarán al laboratorio." }
        return texto + " La consulta quedará de solo lectura."
    }

    // MARK: - Contenido

    private func contenido(_ consulta: Consulta) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if let aviso = viewModel.aviso {
                    AvisoBanner(aviso: aviso)
                }

                paciente(consulta)

                VStack(spacing: 12) {
                    campo("Motivo de consulta", texto: $viewModel.motivo, lineas: 1...4, identificador: "consulta.motivo")
                    campo("Diagnóstico", texto: $viewModel.diagnostico, lineas: 2...6, identificador: "consulta.diagnostico")
                    campo("Notas clínicas", texto: $viewModel.notas, lineas: 3...8, identificador: "consulta.notas")
                }
                .disabled(viewModel.cerrada)

                recetas
                examenes

                if !viewModel.cerrada {
                    HStack(spacing: 10) {
                        BotonSecundario(titulo: "Receta", sistema: "plus") { hoja = .receta }
                            .accessibilityIdentifier("consulta.agregarReceta")
                        BotonSecundario(titulo: "Examen", sistema: "plus") { hoja = .examen }
                            .accessibilityIdentifier("consulta.solicitarExamen")
                    }
                    BotonPrincipal(titulo: "Finalizar consulta", cargando: viewModel.ocupado) {
                        confirmandoFin = true
                    }
                    .accessibilityIdentifier("consulta.finalizar")
                    if viewModel.hayBorrador {
                        Button("Descartar borrador") { confirmandoDescarte = true }
                            .font(.subheadline)
                            .foregroundStyle(Color.cmoTintaSuave)
                            .frame(maxWidth: .infinity)
                            .disabled(viewModel.ocupado)
                            .accessibilityIdentifier("consulta.descartar")
                    }
                }
            }
            .padding(16)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func paciente(_ consulta: Consulta) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                AvatarIniciales(nombre: consulta.cita?.paciente?.nombreCompleto ?? "P", tono: .info, tamano: 46)
                VStack(alignment: .leading, spacing: 2) {
                    Text(consulta.cita?.paciente?.nombreCompleto ?? "Paciente")
                        .font(.headline)
                        .foregroundStyle(Color.cmoTinta)
                    Text(consulta.cita?.paciente?.numeroExpediente ?? "")
                        .font(.caption)
                        .foregroundStyle(Color.cmoTintaSuave)
                }
                Spacer(minLength: 8)
                EstadoChip(texto: viewModel.cerrada ? "Finalizada" : "En consulta", tono: viewModel.cerrada ? .exito : .info)
            }
            Text("Inicio: \(Formato.marcaTiempo(consulta.fechaHoraInicio))")
                .font(.caption)
                .foregroundStyle(Color.cmoTintaSuave)
            if viewModel.cerrada {
                Label("Finalizada el \(Formato.marcaTiempo(consulta.fechaHoraFin)). Es de solo lectura.", systemImage: "lock.fill")
                    .font(.caption)
                    .foregroundStyle(Color.cmoExito)
            }
        }
        .tarjeta()
    }

    private var recetas: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recetas").font(.headline).foregroundStyle(Color.cmoTinta)
            if viewModel.recetas.isEmpty && viewModel.recetasBorrador.isEmpty {
                vacio("Aún no hay recetas.")
            }
            ForEach(viewModel.recetas) { receta in
                filaReceta(receta.medicamento, receta.dosis, receta.frecuencia, receta.duracion, receta.indicaciones, borrador: false) {
                    if !viewModel.cerrada { Task { await viewModel.eliminarReceta(receta) } }
                }
            }
            ForEach(viewModel.recetasBorrador) { receta in
                filaReceta(receta.medicamento, receta.dosis, receta.frecuencia, receta.duracion, receta.indicaciones, borrador: true) {
                    viewModel.quitarRecetaBorrador(receta)
                }
            }
        }
    }

    private var examenes: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Exámenes de laboratorio").font(.headline).foregroundStyle(Color.cmoTinta)
            if viewModel.ordenes.isEmpty && viewModel.examenesBorrador.isEmpty {
                vacio("Aún no hay exámenes solicitados.")
            }
            ForEach(viewModel.ordenes) { orden in
                OrdenFila(orden: orden).tarjeta()
            }
            ForEach(viewModel.examenesBorrador) { examen in
                HStack(spacing: 12) {
                    IconoCirculo(sistema: "testtube.2", tono: .neutro)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(examen.nombre).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                        if let observaciones = examen.observaciones {
                            Text(observaciones).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                        }
                    }
                    Spacer(minLength: 8)
                    EstadoChip(texto: "Borrador", tono: .neutro)
                    Button { viewModel.quitarExamenBorrador(examen) } label: {
                        Image(systemName: "xmark").font(.footnote.weight(.semibold)).foregroundStyle(Color.cmoTintaSuave)
                    }
                    .accessibilityLabel("Quitar \(examen.nombre)")
                }
                .tarjeta()
            }
            if !viewModel.cerrada && !viewModel.examenesBorrador.isEmpty {
                Text("Los exámenes llegan al laboratorio cuando finalizas la consulta. Hasta entonces puedes quitarlos.")
                    .font(.footnote)
                    .foregroundStyle(Color.cmoTintaSuave)
            }
        }
    }

    private func vacio(_ texto: String) -> some View {
        Text(texto)
            .font(.subheadline)
            .foregroundStyle(Color.cmoTintaSuave)
            .frame(maxWidth: .infinity, alignment: .leading)
            .tarjeta()
    }

    private func filaReceta(_ medicamento: String, _ dosis: String, _ frecuencia: String, _ duracion: String, _ indicaciones: String?, borrador: Bool, quitar: @escaping () -> Void) -> some View {
        HStack(alignment: .top, spacing: 12) {
            IconoCirculo(sistema: "pills", tono: borrador ? .neutro : .info)
            VStack(alignment: .leading, spacing: 2) {
                Text(medicamento).font(.subheadline.weight(.semibold)).foregroundStyle(Color.cmoTinta)
                Text("\(dosis) · \(frecuencia) · \(duracion)").font(.caption).foregroundStyle(Color.cmoTintaSuave)
                if let indicaciones, !indicaciones.isEmpty {
                    Text(indicaciones).font(.caption).foregroundStyle(Color.cmoTintaSuave)
                }
            }
            Spacer(minLength: 8)
            if borrador { EstadoChip(texto: "Borrador", tono: .neutro) }
            if !viewModel.cerrada {
                Button(action: quitar) {
                    Image(systemName: "xmark").font(.footnote.weight(.semibold)).foregroundStyle(Color.cmoTintaSuave)
                }
                .accessibilityLabel("Quitar \(medicamento)")
            }
        }
        .tarjeta()
    }

    /// Campo de texto en tarjeta con su título siempre visible (el marcador desaparece al escribir).
    private func campo(_ titulo: String, texto: Binding<String>, lineas: ClosedRange<Int>, identificador: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(titulo)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.cmoTintaSuave)
            TextField(titulo, text: texto, axis: .vertical)
                .lineLimit(lineas)
                .foregroundStyle(Color.cmoTinta)
                .accessibilityIdentifier(identificador)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .tarjeta()
    }
}

/// Formulario de recetas: medicamento, dosis, frecuencia y duración son obligatorios.
/// Se pueden dejar listos varios medicamentos y agregarlos todos de una vez.
struct RecetaFormView: View {
    @Environment(\.dismiss) private var cerrar
    let agregar: (SolicitudReceta) -> Bool

    @State private var medicamento = ""
    @State private var dosis = ""
    @State private var frecuencia = ""
    @State private var duracion = ""
    @State private var indicaciones = ""
    @State private var listos: [SolicitudReceta] = []

    private var completo: Bool {
        ![medicamento, dosis, frecuencia, duracion].contains { $0.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    private var actual: SolicitudReceta {
        let limpias = indicaciones.trimmingCharacters(in: .whitespacesAndNewlines)
        return SolicitudReceta(
            medicamento: medicamento.trimmingCharacters(in: .whitespaces),
            dosis: dosis.trimmingCharacters(in: .whitespaces),
            frecuencia: frecuencia.trimmingCharacters(in: .whitespaces),
            duracion: duracion.trimmingCharacters(in: .whitespaces),
            indicaciones: limpias.isEmpty ? nil : limpias
        )
    }

    private var total: Int { listos.count + (completo ? 1 : 0) }

    private func limpiar() {
        medicamento = ""; dosis = ""; frecuencia = ""; duracion = ""; indicaciones = ""
    }

    var body: some View {
        NavigationStack {
            Form {
                if !listos.isEmpty {
                    Section("Medicamentos de esta receta") {
                        ForEach(Array(listos.enumerated()), id: \.offset) { posicion, receta in
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(receta.medicamento).font(.subheadline.weight(.semibold))
                                    Text("\(receta.dosis) · \(receta.frecuencia) · \(receta.duracion)")
                                        .font(.caption)
                                        .foregroundStyle(Color.cmoTintaSuave)
                                }
                                Spacer()
                                Button { listos.remove(at: posicion) } label: {
                                    Image(systemName: "xmark").font(.footnote.weight(.semibold)).foregroundStyle(Color.cmoTintaSuave)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Quitar \(receta.medicamento)")
                            }
                        }
                    }
                }
                Section(listos.isEmpty ? "Medicamento" : "Otro medicamento") {
                    TextField("Medicamento", text: $medicamento).accessibilityIdentifier("receta.medicamento")
                    TextField("Dosis (ej. 500 mg)", text: $dosis).accessibilityIdentifier("receta.dosis")
                    TextField("Frecuencia (ej. cada 8 horas)", text: $frecuencia).accessibilityIdentifier("receta.frecuencia")
                    TextField("Duración (ej. 7 días)", text: $duracion).accessibilityIdentifier("receta.duracion")
                    TextField("Indicaciones (opcional)", text: $indicaciones, axis: .vertical).lineLimit(1...4)
                }
                Section {
                    Button {
                        listos.append(actual)
                        limpiar()
                    } label: {
                        Label("Agregar otro medicamento", systemImage: "plus")
                    }
                    .disabled(!completo)
                    .accessibilityIdentifier("receta.otro")
                }
            }
            .navigationTitle("Nueva receta")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { cerrar() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(total > 1 ? "Agregar (\(total))" : "Agregar") {
                        let todas = listos + (completo ? [actual] : [])
                        for receta in todas { _ = agregar(receta) }
                        cerrar()
                    }
                    .disabled(total == 0)
                    .accessibilityIdentifier("receta.agregar")
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

/// Elegir uno o varios exámenes del catálogo; quedan como borrador hasta finalizar la consulta.
struct OrdenFormView: View {
    @Environment(\.dismiss) private var cerrar
    let cargarExamenes: () async -> [Examen]
    let solicitar: (Examen, String) -> Bool

    @State private var examenes: [Examen] = []
    @State private var cargando = true
    @State private var elegidos: [Examen] = []
    @State private var observaciones = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Exámenes") {
                    if cargando {
                        ProgressView()
                    } else if examenes.isEmpty {
                        Text("No se pudo cargar el catálogo de exámenes.").foregroundStyle(Color.cmoTintaSuave)
                    }
                    ForEach(examenes) { examen in
                        Button {
                            if let posicion = elegidos.firstIndex(of: examen) {
                                elegidos.remove(at: posicion)
                            } else {
                                elegidos.append(examen)
                            }
                        } label: {
                            HStack {
                                Text(examen.nombre).foregroundStyle(Color.cmoTinta)
                                Spacer()
                                if elegidos.contains(examen) {
                                    Image(systemName: "checkmark").foregroundStyle(Color.cmoMarca)
                                }
                            }
                        }
                        .accessibilityIdentifier("examen.\(examen.nombre)")
                    }
                }
                Section("Observaciones (opcional)") {
                    TextField("Indicaciones para el laboratorio", text: $observaciones, axis: .vertical).lineLimit(2...5)
                }
            }
            .navigationTitle("Pedir examen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { cerrar() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(elegidos.count > 1 ? "Agregar (\(elegidos.count))" : "Agregar") {
                        for examen in elegidos { _ = solicitar(examen, observaciones) }
                        cerrar()
                    }
                    .disabled(elegidos.isEmpty)
                    .accessibilityIdentifier("examen.solicitar")
                }
            }
            .task {
                examenes = await cargarExamenes()
                cargando = false
            }
        }
        .presentationDetents([.medium, .large])
    }
}
