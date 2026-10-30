//
//  Borrador.swift
//  appcmo
//

import Foundation

/// Receta que el médico escribió en una consulta abierta y todavía no se envió a la API.
nonisolated struct RecetaBorrador: Codable, Sendable, Equatable, Identifiable {
    let id: String
    let medicamento: String
    let dosis: String
    let frecuencia: String
    let duracion: String
    let indicaciones: String?

    init(_ receta: SolicitudReceta) {
        id = UUID().uuidString
        medicamento = receta.medicamento
        dosis = receta.dosis
        frecuencia = receta.frecuencia
        duracion = receta.duracion
        indicaciones = receta.indicaciones
    }

    var solicitud: SolicitudReceta {
        SolicitudReceta(medicamento: medicamento, dosis: dosis, frecuencia: frecuencia, duracion: duracion, indicaciones: indicaciones)
    }
}

/// Examen de laboratorio que el médico eligió y todavía no se envió a la API.
nonisolated struct ExamenBorrador: Codable, Sendable, Equatable, Identifiable {
    let id: String
    let idExamen: String
    let nombre: String
    let observaciones: String?

    init(examen: Examen, observaciones: String?) {
        id = UUID().uuidString
        idExamen = examen.idExamen
        nombre = examen.nombre
        self.observaciones = observaciones
    }

    var solicitud: SolicitudOrden {
        SolicitudOrden(idExamen: idExamen, observaciones: observaciones)
    }
}

/// Lo que el médico lleva escrito en una consulta abierta. Vive cifrado en el iPhone y recién se envía a la API
/// al finalizar la consulta.
nonisolated struct BorradorConsulta: Codable, Sendable, Equatable {
    var motivo: String
    var diagnostico: String
    var notas: String
    var recetas: [RecetaBorrador]
    var examenes: [ExamenBorrador]
}
