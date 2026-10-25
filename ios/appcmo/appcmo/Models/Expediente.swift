//
//  Expediente.swift
//  appcmo
//

import Foundation

nonisolated struct HistorialClinico: Decodable, Sendable, Equatable {
    let antecedentesPersonales: String?
    let antecedentesFamiliares: String?

    enum CodingKeys: String, CodingKey {
        case antecedentesPersonales = "antecedentes_personales"
        case antecedentesFamiliares = "antecedentes_familiares"
    }
}

nonisolated struct ConsultaExpediente: Decodable, Sendable, Equatable, Identifiable {
    let idConsulta: String
    let diagnostico: String?
    let motivo: String?
    let notas: String?
    let fechaHoraInicio: String
    let fechaHoraFin: String?
    let recetas: [Receta]?

    var id: String { idConsulta }

    enum CodingKeys: String, CodingKey {
        case idConsulta = "id_consulta"
        case diagnostico, motivo, notas
        case fechaHoraInicio = "fecha_hora_inicio"
        case fechaHoraFin = "fecha_hora_fin"
        case recetas
    }
}

/// Una cita del expediente, con su consulta si ya se atendió.
nonisolated struct CitaExpediente: Decodable, Sendable, Equatable, Identifiable {
    let idCita: String
    let fecha: String?
    let horaInicio: String?
    let motivo: String
    let estado: EstadoCita
    let consulta: ConsultaExpediente?
    /// Solo en `GET /mi/expediente`: nombre y cargo del médico (nada más).
    let medico: MedicoResumen?

    var id: String { idCita }

    enum CodingKeys: String, CodingKey {
        case idCita = "id_cita"
        case fecha
        case horaInicio = "hora_inicio"
        case motivo, estado, consulta, medico
    }
}

nonisolated struct IngresoExpediente: Decodable, Sendable, Equatable, Identifiable {
    let idIngreso: String
    let fechaIngreso: String
    let fechaAlta: String?
    let motivo: String?
    let cama: CamaResumen?
    /// Solo en `GET /mi/expediente`: médico tratante (nombre y cargo).
    let medico: MedicoResumen?

    var id: String { idIngreso }
    var activo: Bool { fechaAlta == nil }

    enum CodingKeys: String, CodingKey {
        case idIngreso = "id_ingreso"
        case fechaIngreso = "fecha_ingreso"
        case fechaAlta = "fecha_alta"
        case motivo, cama, medico
    }
}

/// `GET /pacientes/{id}/expediente`: solo para el médico tratante (403 para los demás).
nonisolated struct Expediente: Decodable, Sendable, Equatable {
    let idPaciente: String
    let numeroExpediente: String
    let nombreCompleto: String
    let fechaNacimiento: String
    let dui: String?
    let sexo: String
    let telefono: String?
    let direccion: String?
    let tipoSangre: String?
    let alergias: String?
    let historial: HistorialClinico?
    let citas: [CitaExpediente]
    let ordenesLaboratorio: [OrdenLaboratorio]
    let ingresos: [IngresoExpediente]

    var sexoTexto: String { sexo == "F" ? "Femenino" : "Masculino" }

    /// Consultas ya atendidas, de la más reciente a la más antigua.
    var consultas: [CitaExpediente] {
        citas.filter { $0.consulta != nil }.sorted { ($0.fecha ?? "", $0.horaInicio ?? "") > ($1.fecha ?? "", $1.horaInicio ?? "") }
    }

    /// Todas las recetas del expediente, con la fecha de su consulta.
    var recetas: [(receta: Receta, fecha: String?)] {
        consultas.flatMap { cita in (cita.consulta?.recetas ?? []).map { ($0, cita.fecha) } }
    }

    var ingresoActivo: IngresoExpediente? { ingresos.first { $0.activo } }

    enum CodingKeys: String, CodingKey {
        case idPaciente = "id_paciente"
        case numeroExpediente = "numero_expediente"
        case nombreCompleto = "nombre_completo"
        case fechaNacimiento = "fecha_nacimiento"
        case dui, sexo, telefono, direccion
        case tipoSangre = "tipo_sangre"
        case alergias, historial, citas
        case ordenesLaboratorio = "ordenes_laboratorio"
        case ingresos
    }

    static func == (izquierda: Expediente, derecha: Expediente) -> Bool {
        izquierda.idPaciente == derecha.idPaciente && izquierda.citas == derecha.citas
            && izquierda.ordenesLaboratorio == derecha.ordenesLaboratorio && izquierda.historial == derecha.historial
    }
}

/// Cuerpo de `PUT /pacientes/{id}/historial`.
nonisolated struct CambiosHistorial: Encodable, Sendable {
    let antecedentesPersonales: String
    let antecedentesFamiliares: String

    enum CodingKeys: String, CodingKey {
        case antecedentesPersonales = "antecedentes_personales"
        case antecedentesFamiliares = "antecedentes_familiares"
    }
}
