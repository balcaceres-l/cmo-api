//
//  PacienteDatos.swift
//  appcmo
//

import Foundation
import Observation

/// Los datos del propio paciente que comparten todas sus pantallas: sus citas, sus exámenes y su expediente.
/// Cada pantalla pide lo suyo al abrirse y al deslizar para refrescar; nunca hay datos de otras personas.
@Observable
final class PacienteDatos {
    private(set) var citas: Carga<[Cita]> = .inicial
    private(set) var ordenes: Carga<[OrdenLaboratorio]> = .inicial
    private(set) var expediente: Carga<Expediente> = .inicial

    @ObservationIgnored private let repoCitas: CitasRepository
    @ObservationIgnored private let repoLaboratorio: LaboratorioRepository
    @ObservationIgnored private let repoPacientes: PacientesRepository
    /// La fecha de "hoy" (se puede cambiar en pruebas).
    @ObservationIgnored private let hoy: () -> String

    init(
        citas: CitasRepository = CitasRepository(),
        laboratorio: LaboratorioRepository = LaboratorioRepository(),
        pacientes: PacientesRepository = PacientesRepository(),
        hoy: @escaping () -> String = { Formato.hoyAPI }
    ) {
        self.repoCitas = citas
        self.repoLaboratorio = laboratorio
        self.repoPacientes = pacientes
        self.hoy = hoy
    }

    // MARK: - Carga

    func cargarTodo() async {
        async let a: Void = cargarCitas()
        async let b: Void = cargarOrdenes()
        async let c: Void = cargarExpediente()
        _ = await (a, b, c)
    }

    func cargarCitas() async {
        citas = citas.alRecargar
        do {
            citas = .listo(try await repoCitas.delPaciente())
        } catch {
            // Un fallo pasajero no borra lo que ya se estaba viendo.
            if citas.valor == nil, let mensaje = mensajeDeError(error) { citas = .fallo(mensaje) }
        }
    }

    func cargarOrdenes() async {
        ordenes = ordenes.alRecargar
        do {
            ordenes = .listo(try await repoLaboratorio.mias())
        } catch {
            if ordenes.valor == nil, let mensaje = mensajeDeError(error) { ordenes = .fallo(mensaje) }
        }
    }

    func cargarExpediente() async {
        expediente = expediente.alRecargar
        do {
            expediente = .listo(try await repoPacientes.miExpediente())
        } catch {
            if expediente.valor == nil, let mensaje = mensajeDeError(error) { expediente = .fallo(mensaje) }
        }
    }

    // MARK: - Citas

    private var todas: [Cita] { citas.valor ?? [] }
    private static let vigentes: Set<EstadoCita> = [.agendada, .enEspera, .enAtencion]

    nonisolated private static func antes(_ a: Cita, _ b: Cita) -> Bool {
        ((a.fecha ?? ""), (a.horaInicio ?? "")) < ((b.fecha ?? ""), (b.horaInicio ?? ""))
    }

    /// Solicitudes que recepción todavía no confirma, la más reciente primero.
    var solicitudes: [Cita] {
        todas.filter { $0.estado == .solicitada }.sorted { ($0.createdAt ?? "") > ($1.createdAt ?? "") }
    }

    /// Todas las citas de hoy, sea cual sea su estado (menos las solicitudes, que aún no tienen fecha).
    var citasDeHoy: [Cita] {
        let hoy = hoy()
        return todas.filter { $0.estado != .solicitada && $0.fecha == hoy }.sorted(by: Self.antes)
    }

    private var vigentesDeHoy: [Cita] {
        citasDeHoy.filter { Self.vigentes.contains($0.estado) }
    }

    /// Citas vigentes agendadas para después de hoy.
    var citasProximas: [Cita] {
        let hoy = hoy()
        return todas.filter { Self.vigentes.contains($0.estado) && ($0.fecha ?? "") > hoy }.sorted(by: Self.antes)
    }

    /// Por estado: agendadas (incluye las que ya llegaron o están en consulta), de la más cercana a la más lejana.
    var agendadas: [Cita] {
        todas.filter { Self.vigentes.contains($0.estado) }.sorted(by: Self.antes)
    }

    /// Atendidas, de la más reciente a la más antigua.
    var atendidas: [Cita] {
        todas.filter { $0.estado == .atendida }.sorted { Self.antes($1, $0) }
    }

    /// Canceladas y a las que no asistió, de la más reciente a la más antigua.
    var canceladas: [Cita] {
        todas.filter { $0.estado == .cancelada || $0.estado == .noAsistio }.sorted { Self.antes($1, $0) }
    }

    /// La cita que sigue: la de hoy que no ha terminado o, si no hay, la más cercana.
    var proxima: Cita? { (vigentesDeHoy + citasProximas).first }

    func esHoy(_ cita: Cita) -> Bool { cita.fecha == hoy() }

    /// Cita vigente cuyo día ya pasó sin atenderse: se muestra como "No se atendió" en vez de "Agendada".
    func quedoSinAtender(_ cita: Cita) -> Bool {
        Self.vigentes.contains(cita.estado) && (cita.fecha ?? "") < hoy()
    }

    // MARK: - Exámenes

    func cuantas(_ estado: EstadoOrden) -> Int {
        (ordenes.valor ?? []).filter { $0.estado == estado }.count
    }

    // MARK: - Habitación

    /// El ingreso activo del paciente, si está hospitalizado.
    var ingresoActual: IngresoExpediente? { expediente.valor?.ingresoActivo }
}
