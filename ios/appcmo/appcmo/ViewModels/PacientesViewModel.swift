//
//  PacientesViewModel.swift
//  appcmo
//

import Foundation
import Observation

/// "Mis pacientes": la API solo devuelve a los que el médico trata (HU-29).
/// Además de la búsqueda, se pueden filtrar por ingresados o ambulatorios y se muestra la fecha de la última consulta.
@Observable
final class PacientesViewModel {
    enum Filtro: String, CaseIterable, Identifiable {
        case todos = "Todos", ingresados = "Ingresados", ambulatorios = "Ambulatorios"
        var id: String { rawValue }
    }

    var texto = ""
    var filtro: Filtro = .todos
    private(set) var carga: Carga<[Paciente]> = .inicial
    /// `id_paciente` de los pacientes con un ingreso activo a cargo del médico.
    private(set) var ingresados: Set<String> = []
    /// `id_paciente` → fecha (`YYYY-MM-DD`) de su última cita atendida.
    private(set) var ultimaConsulta: [String: String] = [:]

    @ObservationIgnored private var complementoCargado = false
    @ObservationIgnored private let repositorio: PacientesRepository
    @ObservationIgnored private let citas: CitasRepository
    @ObservationIgnored private let hospitalizacion: HospitalizacionRepository

    init(
        repositorio: PacientesRepository = PacientesRepository(),
        citas: CitasRepository = CitasRepository(),
        hospitalizacion: HospitalizacionRepository = HospitalizacionRepository()
    ) {
        self.repositorio = repositorio
        self.citas = citas
        self.hospitalizacion = hospitalizacion
    }

    /// Los pacientes que se muestran según el filtro elegido.
    var pacientes: [Paciente] {
        let todos = carga.valor ?? []
        switch filtro {
        case .todos: return todos
        case .ingresados: return todos.filter { ingresados.contains($0.idPaciente) }
        case .ambulatorios: return todos.filter { !ingresados.contains($0.idPaciente) }
        }
    }

    func cuantos(_ filtro: Filtro) -> Int {
        let todos = carga.valor ?? []
        switch filtro {
        case .todos: return todos.count
        case .ingresados: return todos.filter { ingresados.contains($0.idPaciente) }.count
        case .ambulatorios: return todos.filter { !ingresados.contains($0.idPaciente) }.count
        }
    }

    func estaIngresado(_ paciente: Paciente) -> Bool { ingresados.contains(paciente.idPaciente) }

    func ultimaConsulta(de paciente: Paciente) -> String? { ultimaConsulta[paciente.idPaciente] }

    /// `completo` vuelve a pedir también los ingresos y las citas (al abrir y al deslizar para refrescar);
    /// al escribir en el buscador solo se repite la búsqueda.
    func buscar(completo: Bool = false) async {
        carga = carga.alRecargar
        do {
            carga = .listo(try await repositorio.buscar(texto))
        } catch {
            guard let mensaje = mensajeDeError(error) else { return }
            carga = .fallo(mensaje)
            return
        }
        if completo || !complementoCargado { await cargarComplemento() }
    }

    /// Ingresos y última consulta son un extra: si fallan, la lista se sigue viendo sin ellos.
    private func cargarComplemento() async {
        async let ingresos = try? hospitalizacion.ingresosActivos()
        async let historial = try? citas.delMedico()
        if let ingresos = await ingresos {
            ingresados = Set(ingresos.map(\.idPaciente))
        }
        if let historial = await historial {
            var ultimas: [String: String] = [:]
            for cita in historial where cita.estado == .atendida {
                guard let fecha = cita.fecha else { continue }
                if fecha > (ultimas[cita.idPaciente] ?? "") { ultimas[cita.idPaciente] = fecha }
            }
            ultimaConsulta = ultimas
        }
        complementoCargado = true
    }
}
