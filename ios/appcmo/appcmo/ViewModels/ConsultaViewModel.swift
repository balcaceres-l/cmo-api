//
//  ConsultaViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Consulta general: diagnóstico, recetas y exámenes de laboratorio (HU-40 y HU-47).
///
/// Lo que el médico escribe (motivo, diagnóstico, notas, recetas y exámenes) es un **borrador** que se guarda
/// cifrado en este iPhone y se restaura si sale de la consulta. Recién al finalizar se envía a la API, en este
/// orden: datos de la consulta, recetas, exámenes y cierre. Así un examen pedido por error se puede quitar antes
/// de que llegue al laboratorio (la API no permite cancelar una orden).
@Observable
final class ConsultaViewModel {
    let idConsulta: String
    private let idUsuario: String

    private(set) var carga: Carga<Consulta> = .inicial
    var diagnostico = ""
    var motivo = ""
    var notas = ""
    /// Ya están en el servidor (por ejemplo, de un envío que se cortó a medias).
    private(set) var recetas: [Receta] = []
    private(set) var ordenes: [OrdenLaboratorio] = []
    /// Solo existen en este iPhone hasta finalizar.
    private(set) var recetasBorrador: [RecetaBorrador] = []
    private(set) var examenesBorrador: [ExamenBorrador] = []
    private(set) var cerrada = false
    private(set) var ocupado = false
    var aviso: AvisoMensaje?

    @ObservationIgnored private var servidor = (diagnostico: "", motivo: "", notas: "")
    @ObservationIgnored private let consultas: ConsultasRepository
    @ObservationIgnored private let laboratorio: LaboratorioRepository
    @ObservationIgnored private let almacen: BorradorStore

    init(
        idConsulta: String,
        idUsuario: String,
        consultas: ConsultasRepository = ConsultasRepository(),
        laboratorio: LaboratorioRepository = LaboratorioRepository(),
        almacen: BorradorStore = BorradorStore()
    ) {
        self.idConsulta = idConsulta
        self.idUsuario = idUsuario
        self.consultas = consultas
        self.laboratorio = laboratorio
        self.almacen = almacen
    }

    /// Hay algo escrito que todavía no está en el servidor.
    var hayBorrador: Bool {
        !recetasBorrador.isEmpty || !examenesBorrador.isEmpty
            || diagnostico != servidor.diagnostico || motivo != servidor.motivo || notas != servidor.notas
    }

    /// La vista observa esto para guardar el borrador cada vez que el médico escribe.
    var textos: [String] { [motivo, diagnostico, notas] }

    // MARK: - Carga

    func cargar() async {
        carga = carga.alRecargar
        do {
            let consulta = try await consultas.ver(id: idConsulta)
            aplicarServidor(consulta)
            if consulta.estaCerrada {
                almacen.borrar(usuario: idUsuario, consulta: idConsulta)
            } else if let borrador = almacen.leer(usuario: idUsuario, consulta: idConsulta) {
                restaurar(borrador)
            }
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }

    private func aplicarServidor(_ consulta: Consulta) {
        carga = .listo(consulta)
        recetas = consulta.recetas ?? []
        ordenes = consulta.ordenesLaboratorio ?? []
        cerrada = consulta.estaCerrada
        servidor = (consulta.diagnostico ?? "", consulta.motivo ?? "", consulta.notas ?? "")
        diagnostico = servidor.diagnostico
        motivo = servidor.motivo
        notas = servidor.notas
        recetasBorrador = []
        examenesBorrador = []
    }

    private func restaurar(_ borrador: BorradorConsulta) {
        motivo = borrador.motivo
        diagnostico = borrador.diagnostico
        notas = borrador.notas
        recetasBorrador = borrador.recetas
        examenesBorrador = borrador.examenes
    }

    /// Vuelve a pedir lo que ya está en el servidor sin tocar el borrador.
    private func refrescarServidor() async {
        guard let consulta = try? await consultas.ver(id: idConsulta) else { return }
        carga = .listo(consulta)
        recetas = consulta.recetas ?? []
        ordenes = consulta.ordenesLaboratorio ?? []
    }

    // MARK: - Borrador

    /// Guarda el borrador (o lo borra si ya no hay nada distinto del servidor).
    func persistir() {
        guard !cerrada, carga.valor != nil else { return }
        if hayBorrador {
            let borrador = BorradorConsulta(motivo: motivo, diagnostico: diagnostico, notas: notas, recetas: recetasBorrador, examenes: examenesBorrador)
            try? almacen.guardar(borrador, usuario: idUsuario, consulta: idConsulta)
        } else {
            almacen.borrar(usuario: idUsuario, consulta: idConsulta)
        }
    }

    func agregarReceta(_ receta: SolicitudReceta) -> Bool {
        recetasBorrador.append(RecetaBorrador(receta))
        persistir()
        return true
    }

    func quitarRecetaBorrador(_ receta: RecetaBorrador) {
        recetasBorrador.removeAll { $0.id == receta.id }
        persistir()
    }

    func agregarExamen(_ examen: Examen, observaciones: String) -> Bool {
        let limpio = observaciones.trimmingCharacters(in: .whitespacesAndNewlines)
        examenesBorrador.append(ExamenBorrador(examen: examen, observaciones: limpio.isEmpty ? nil : limpio))
        persistir()
        return true
    }

    func quitarExamenBorrador(_ examen: ExamenBorrador) {
        examenesBorrador.removeAll { $0.id == examen.id }
        persistir()
    }

    /// Tira lo escrito en este iPhone y vuelve a lo que hay en el servidor.
    func descartarBorrador() {
        almacen.borrar(usuario: idUsuario, consulta: idConsulta)
        diagnostico = servidor.diagnostico
        motivo = servidor.motivo
        notas = servidor.notas
        recetasBorrador = []
        examenesBorrador = []
        aviso = .informacion("Borrador descartado.")
    }

    // MARK: - API

    /// Una receta que ya está en el servidor (la API sí permite borrarla mientras la consulta esté abierta).
    func eliminarReceta(_ receta: Receta) async {
        aviso = nil
        do {
            try await consultas.eliminarReceta(id: receta.idReceta)
            recetas.removeAll { $0.idReceta == receta.idReceta }
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
        }
    }

    func examenes() async -> [Examen] {
        do {
            return try await laboratorio.examenes()
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
            return []
        }
    }

    /// Envía el borrador y cierra la consulta. Cada receta y examen se quita del borrador apenas la API lo
    /// acepta; si algo falla, lo ya enviado queda en el servidor, el resto sigue como borrador y un nuevo
    /// intento no duplica nada. La cita pasa a ATENDIDA y todo queda de solo lectura.
    func finalizar() async {
        ocupado = true
        aviso = nil
        defer { ocupado = false }

        var paso = "guardar la consulta"
        do {
            _ = try await consultas.guardar(id: idConsulta, cambios: CambiosConsulta(diagnostico: diagnostico, motivo: motivo, notas: notas))
            servidor = (diagnostico, motivo, notas)

            for receta in recetasBorrador {
                paso = "enviar la receta de \(receta.medicamento)"
                _ = try await consultas.agregarReceta(idConsulta: idConsulta, receta: receta.solicitud)
                recetasBorrador.removeAll { $0.id == receta.id }
                persistir()
            }
            for examen in examenesBorrador {
                paso = "enviar el examen \(examen.nombre)"
                _ = try await laboratorio.solicitar(idConsulta: idConsulta, orden: examen.solicitud)
                examenesBorrador.removeAll { $0.id == examen.id }
                persistir()
            }

            paso = "finalizar la consulta"
            _ = try await consultas.finalizar(id: idConsulta)
            almacen.borrar(usuario: idUsuario, consulta: idConsulta)
            cerrada = true
            if let consulta = try? await consultas.ver(id: idConsulta) { aplicarServidor(consulta) }
            aviso = .exito("Consulta finalizada. Ahora es de solo lectura.")
        } catch {
            await refrescarServidor()
            let motivoError = mensajeDeError(error) ?? "se canceló la operación"
            aviso = .error("No se pudo \(paso): \(motivoError) Lo que ya se envió quedó en el servidor y el resto sigue como borrador en este iPhone. Vuelve a intentar.")
        }
    }
}
