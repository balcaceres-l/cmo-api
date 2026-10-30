//
//  CitaDetalleViewModel.swift
//  appcmo
//

import Foundation
import Observation

@Observable
final class CitaDetalleViewModel {
    private(set) var cita: Cita
    /// Id de la consulta a la que se navega.
    var consultaDestino: String?
    private(set) var trabajando = false
    var error: String?
    /// Otra consulta del mismo médico que sigue abierta: impide iniciar una segunda.
    var enAtencion: Cita?

    @ObservationIgnored private let citas: CitasRepository
    @ObservationIgnored private let consultas: ConsultasRepository

    init(cita: Cita, citas: CitasRepository = CitasRepository(), consultas: ConsultasRepository = ConsultasRepository()) {
        self.cita = cita
        self.citas = citas
        self.consultas = consultas
    }

    /// La consulta solo se inicia el día de la cita (la API también lo exige).
    var esDeHoy: Bool { cita.fecha == Formato.hoyAPI }

    /// Vuelve a pedir la cita: cambia de estado cuando se finaliza o se abandona su consulta.
    func recargar() async {
        guard let actual = try? await citas.delMedico().first(where: { $0.id == cita.id }) else { return }
        cita = actual
    }

    /// Abre la consulta de una cita AGENDADA o EN_ESPERA; la cita pasa a EN_ATENCION.
    /// Un médico no atiende a dos pacientes a la vez: antes de abrirla se mira su agenda actual y, si ya tiene
    /// otra consulta en atención, no se inicia. (La API todavía no lo valida; esta revisión es solo de la app.)
    func iniciarConsulta() async {
        guard esDeHoy else {
            error = "La consulta solo se inicia el día de la cita."
            return
        }
        trabajando = true
        error = nil
        enAtencion = nil
        defer { trabajando = false }
        do {
            let citasDelMedico = try await citas.delMedico()
            if let otra = citasDelMedico.first(where: { $0.estado == .enAtencion && $0.id != cita.id }) {
                enAtencion = otra
                return
            }
            consultaDestino = try await citas.abrirConsulta(idCita: cita.idCita).idConsulta
        } catch APIError.conflicto(let mensaje) where mensaje.contains("ya tiene una consulta") {
            // Ya tenía consulta (por ejemplo, se abrió desde otro equipo): se continúa esa.
            await abrirExistente(de: cita)
        } catch {
            self.error = mensajeDeError(error)
        }
    }

    /// Continúa o revisa la consulta de una cita EN_ATENCION o ATENDIDA.
    func verConsulta() async {
        await continuarConsulta(de: cita)
    }

    /// Abre la consulta de `otra` cita (por ejemplo, la que ya estaba en atención).
    func continuarConsulta(de otra: Cita) async {
        trabajando = true
        error = nil
        enAtencion = nil
        defer { trabajando = false }
        await abrirExistente(de: otra)
    }

    private func abrirExistente(de cita: Cita) async {
        do {
            if let id = try await consultas.idDeConsulta(cita: cita) {
                consultaDestino = id
            } else {
                error = "No se encontró la consulta de esta cita."
            }
        } catch {
            self.error = mensajeDeError(error)
        }
    }
}
