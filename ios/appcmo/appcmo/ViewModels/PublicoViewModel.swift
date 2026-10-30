//
//  PublicoViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Información pública del hospital, sin iniciar sesión (HU-68).
@Observable
final class PublicoViewModel {
    private(set) var ocupacion: Carga<[OcupacionResumen]> = .inicial
    private(set) var medicos: Carga<[MedicoDeTurno]> = .inicial
    private(set) var avisos: Carga<[AvisoPublico]> = .inicial
    private(set) var ambulancia: Carga<AmbulanciaPublica> = .inicial

    @ObservationIgnored private let repositorio: PublicoRepository

    init(repositorio: PublicoRepository = PublicoRepository()) {
        self.repositorio = repositorio
    }

    func cargarTodo() async {
        ocupacion = ocupacion.alRecargar
        medicos = medicos.alRecargar
        avisos = avisos.alRecargar
        async let o: Void = cargarOcupacion()
        async let m: Void = cargarMedicos()
        async let a: Void = cargarAvisos()
        async let b: Void = refrescarAmbulancia()
        _ = await (o, m, a, b)
    }

    private func cargarOcupacion() async {
        do { ocupacion = .listo(OcupacionResumen.resumir(try await repositorio.ocupacion())) }
        catch { if let mensaje = mensajeDeError(error) { ocupacion = .fallo(mensaje) } }
    }

    private func cargarMedicos() async {
        do { medicos = .listo(try await repositorio.medicosDeTurno()) }
        catch { if let mensaje = mensajeDeError(error) { medicos = .fallo(mensaje) } }
    }

    private func cargarAvisos() async {
        do { avisos = .listo(try await repositorio.avisos()) }
        catch { if let mensaje = mensajeDeError(error) { avisos = .fallo(mensaje) } }
    }

    /// El estado de la ambulancia se vuelve a pedir cada 10 segundos mientras la pantalla está abierta.
    func refrescarAmbulancia() async {
        do { ambulancia = .listo(try await repositorio.ambulancia()) }
        catch {
            // Si ya había un estado se conserva; solo se muestra el error la primera vez.
            if ambulancia.valor == nil, let mensaje = mensajeDeError(error) { ambulancia = .fallo(mensaje) }
        }
    }

    func vigilarAmbulancia() async {
        while !Task.isCancelled {
            try? await Task.sleep(for: .seconds(10))
            if Task.isCancelled { return }
            await refrescarAmbulancia()
        }
    }
}
