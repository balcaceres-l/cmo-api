//
//  ExpedienteViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Expediente clínico de un paciente: solo lo ve el médico tratante (403 para cualquier otro).
@Observable
final class ExpedienteViewModel {
    enum Seccion: String, CaseIterable, Identifiable {
        case resumen = "Resumen", consultas = "Consultas", recetas = "Recetas"
        var id: String { rawValue }
    }

    let paciente: Paciente
    var seccion: Seccion = .resumen
    private(set) var carga: Carga<Expediente> = .inicial
    /// El médico no trata a este paciente: se muestra la pantalla "Sin acceso" en vez de un error.
    private(set) var sinAcceso = false

    // Antecedentes (editables)
    var editando = false
    var personales = ""
    var familiares = ""
    private(set) var guardando = false
    var aviso: AvisoMensaje?

    @ObservationIgnored private let repositorio: PacientesRepository

    init(paciente: Paciente, repositorio: PacientesRepository = PacientesRepository()) {
        self.paciente = paciente
        self.repositorio = repositorio
    }

    func cargar() async {
        carga = carga.alRecargar
        sinAcceso = false
        do {
            carga = .listo(try await repositorio.expediente(id: paciente.idPaciente))
        } catch let error as APIError {
            if case .prohibido = error {
                sinAcceso = true
                carga = .inicial
            } else {
                carga = .fallo(error.localizedDescription)
            }
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
        }
    }

    func empezarEdicion() {
        let historial = carga.valor?.historial
        personales = historial?.antecedentesPersonales ?? ""
        familiares = historial?.antecedentesFamiliares ?? ""
        aviso = nil
        editando = true
    }

    func cancelarEdicion() {
        editando = false
        aviso = nil
    }

    func guardarAntecedentes() async {
        guard !guardando else { return }
        guardando = true
        defer { guardando = false }
        do {
            _ = try await repositorio.guardarHistorial(
                id: paciente.idPaciente,
                cambios: CambiosHistorial(
                    antecedentesPersonales: personales.trimmingCharacters(in: .whitespacesAndNewlines),
                    antecedentesFamiliares: familiares.trimmingCharacters(in: .whitespacesAndNewlines)
                )
            )
            editando = false
            await cargar()
            aviso = .exito("Antecedentes guardados.")
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            aviso = .error(mensaje)
        }
    }
}
