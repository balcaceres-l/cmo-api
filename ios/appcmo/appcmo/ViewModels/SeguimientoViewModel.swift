//
//  SeguimientoViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// Agendar un seguimiento: el médico elige un bloque libre de su propia agenda (HU-36).
@Observable
final class SeguimientoViewModel {
    let paciente: Paciente
    private let idMedico: String

    var fecha = Date()
    var bloque: BloqueDisponible?
    var motivo = ""
    /// Lo que el médico escribe para buscar un consultorio.
    var textoConsultorio = ""
    private(set) var consultorio: Consultorio?

    private(set) var bloques: Carga<[BloqueDisponible]> = .inicial
    private(set) var consultorios: [Consultorio] = []
    private(set) var guardando = false
    private(set) var agendada = false
    var aviso: AvisoMensaje?

    @ObservationIgnored private let repositorio: CitasRepository

    init(paciente: Paciente, idMedico: String, repositorio: CitasRepository = CitasRepository()) {
        self.paciente = paciente
        self.idMedico = idMedico
        self.repositorio = repositorio
    }

    var idConsultorio: String? { consultorio?.idConsultorio }

    /// Hay texto escrito pero no se eligió ninguna opción: no se agenda hasta resolverlo, para no perder la elección en silencio.
    var consultorioPendiente: Bool {
        consultorio == nil && !textoConsultorio.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var puedeAgendar: Bool {
        bloque != nil && !motivo.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !consultorioPendiente && !guardando
    }

    /// Consultorios que coinciden con lo escrito (código o nombre, sin importar mayúsculas, tildes ni guiones).
    /// Sin texto devuelve todos. Los consultorios los define el hospital: no se pueden inventar.
    var sugerencias: [Consultorio] {
        guard consultorio == nil else { return [] }
        let palabras = textoConsultorio.split(whereSeparator: \.isWhitespace).map { Self.normalizar(String($0)) }.filter { !$0.isEmpty }
        guard !palabras.isEmpty else { return consultorios }
        return consultorios.filter { opcion in
            let texto = Self.normalizar("\(opcion.codigo) \(opcion.nombre)")
            return palabras.allSatisfy { texto.contains($0) }
        }
    }

    static func normalizar(_ texto: String) -> String {
        texto.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: nil).filter { $0.isLetter || $0.isNumber }
    }

    func elegir(_ opcion: Consultorio) {
        consultorio = opcion
        textoConsultorio = ""
    }

    func quitarConsultorio() {
        consultorio = nil
        textoConsultorio = ""
    }

    func cargarConsultorios() async {
        consultorios = (try? await repositorio.consultorios()) ?? []
    }

    /// Bloques libres del médico para la fecha elegida (turnos menos citas activas).
    func cargarBloques() async {
        bloque = nil
        bloques = .cargando
        do {
            bloques = .listo(try await repositorio.disponibilidad(idMedico: idMedico, fecha: Formato.fechaAPI(fecha)))
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            bloques = .fallo(mensaje)
        }
    }

    func agendar() async {
        guard let bloque else { return }
        guardando = true
        aviso = nil
        defer { guardando = false }
        let solicitud = SolicitudCita(
            idPaciente: paciente.idPaciente,
            fecha: Formato.fechaAPI(fecha),
            horaInicio: Formato.hora(bloque.horaInicio),
            motivo: motivo.trimmingCharacters(in: .whitespacesAndNewlines),
            idConsultorio: idConsultorio
        )
        do {
            _ = try await repositorio.agendar(solicitud)
            agendada = true
        } catch let error as APIError {
            aviso = .error(error.localizedDescription)
            // Si el horario se ocupó mientras tanto, la disponibilidad se refresca.
            if case .conflicto = error { await cargarBloques() }
        } catch {
            aviso = mensajeDeError(error).map(AvisoMensaje.error)
        }
    }
}
