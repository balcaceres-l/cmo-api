package com.cmo.hospital.core.domain.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import java.util.UUID

/**
 * Signos vitales tomados en el triaje clínico o actualizados por el Doctor en consulta.
 * Todos los campos son opcionales para permitir pase rápido en emergencias.
 */
data class SignosVitales(
    @SerializedName("presion_sistolica") val presionSistolica: Int? = null,
    @SerializedName("presion_diastolica") val presionDiastolica: Int? = null,
    @SerializedName("frecuencia_cardiaca") val frecuenciaCardiaca: Int? = null,
    @SerializedName("frecuencia_respiratoria") val frecuenciaRespiratoria: Int? = null,
    @SerializedName("temperatura_c") val temperaturaC: Double? = null,
    @SerializedName("peso_kg") val pesoKg: Double? = null,
    @SerializedName("talla_cm") val tallaCm: Double? = null,
    @SerializedName("saturacion_oxigeno") val saturacionOxigeno: Int? = null,
    val observaciones: String? = null
) {
    val imc: Double?
        get() {
            val p = pesoKg ?: return null
            val t = tallaCm ?: return null
            if (t <= 0.0 || p <= 0.0) return null
            val alturaM = t / 100.0
            val valor = p / (alturaM * alturaM)
            return Math.round(valor * 10.0) / 10.0
        }

    val imcClasificacion: String
        get() {
            val valor = imc ?: return "N/D"
            return when {
                valor < 18.5 -> "Bajo peso"
                valor < 25.0 -> "Normal"
                valor < 30.0 -> "Sobrepeso"
                else -> "Obesidad"
            }
        }

    val presionTexto: String
        get() = when {
            presionSistolica != null && presionDiastolica != null -> "$presionSistolica/$presionDiastolica mmHg"
            presionSistolica != null -> "$presionSistolica mmHg"
            else -> "N/D"
        }

    val hasAnyData: Boolean
        get() = presionSistolica != null || presionDiastolica != null ||
            frecuenciaCardiaca != null || frecuenciaRespiratoria != null ||
            temperaturaC != null || pesoKg != null || tallaCm != null ||
            saturacionOxigeno != null || !observaciones.isNullOrBlank()
}

/**
 * Clasificación de Triage Manchester del prototipo institucional HUC Móvil.
 */
enum class NivelTriageManchester(
    val nivel: Int,
    val titulo: String,
    val tiempoRespuesta: String
) {
    NIVEL_I_ROJO(1, "Nivel I: Rojo", "Inmediato (0 min)"),
    NIVEL_II_NARANJA(2, "Nivel II: Naranja", "Muy Urgente (10 min)"),
    NIVEL_III_AMARILLO(3, "Nivel III: Amarillo", "Urgente (60 min)"),
    NIVEL_IV_VERDE(4, "Nivel IV: Verde", "Estándar (120 min)");

    companion object {
        fun fromNivel(nivel: Int): NivelTriageManchester =
            entries.firstOrNull { it.nivel == nivel } ?: NIVEL_III_AMARILLO
    }
}

/**
 * Entidad de dominio Paciente con cálculo dinámico de edad, generación de expediente
 * (LB-2026-001) y anonimización para monitores públicos (Ley 19.628).
 */
data class Paciente(
    @SerializedName(value = "id", alternate = ["id_paciente"]) val id: String? = null,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val dui: String? = null,
    val sexo: String? = "M",
    val telefono: String? = null,
    val direccion: String? = null,
    @SerializedName("tipo_sangre") val tipoSangre: String? = null,
    val alergias: String? = null,
    @SerializedName("numero_expediente") val numeroExpediente: String? = null,
    val protegido: Boolean = false,
    @SerializedName("contacto_emergencia") val contactoEmergencia: String? = null,
    val prevision: String? = null,
    val nivelTriage: NivelTriageManchester = NivelTriageManchester.NIVEL_III_AMARILLO
) {
    val edad: Int
        get() {
            val fn = fechaNacimiento ?: return 0
            return try {
                val parts = fn.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toIntOrNull() ?: 1990
                    val month = parts[1].toIntOrNull() ?: 1
                    val day = parts[2].toIntOrNull() ?: 1
                    val birthDate = LocalDate.of(year, month, day)
                    val today = LocalDate.now()
                    Period.between(birthDate, today).years.coerceAtLeast(0)
                } else 0
            } catch (_: Exception) {
                0
            }
        }

    companion object {
        fun extraerIniciales(nombreCompleto: String): String {
            val tokens = nombreCompleto.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return "XX"
            val ini1 = tokens[0].firstOrNull()?.uppercaseChar() ?: 'X'
            val ini2 = if (tokens.size >= 2) {
                tokens[1].firstOrNull()?.uppercaseChar() ?: 'X'
            } else if (tokens[0].length >= 2) {
                tokens[0][1].uppercaseChar()
            } else {
                ini1
            }
            return "$ini1$ini2"
        }

        fun generarNumeroExpediente(
            nombreCompleto: String,
            year: Int = 2026,
            correlativo: Int = 1
        ): String {
            val iniciales = extraerIniciales(nombreCompleto)
            return String.format(Locale.US, "%s-%d-%03d", iniciales, year, correlativo)
        }

        /**
         * Anonimización Ley 19.628 para pantallas y llamador de sala: "C. M**** (Protegido)".
         */
        fun anonimizarNombre(nombreCompleto: String): String {
            val tokens = nombreCompleto.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return "P. **** (Protegido)"
            val i1 = tokens[0].first().uppercaseChar()
            val i2 = tokens.getOrNull(1)?.first()?.uppercaseChar() ?: 'X'
            return "$i1. ${i2}**** (Protegido)"
        }

        fun calcularFechaNacimientoDesdeEdad(edadAnios: Int): String {
            val safeAge = edadAnios.coerceIn(0, 120)
            return LocalDate.now().minusYears(safeAge.toLong()).toString()
        }
    }
}

data class PacienteInfo(
    @SerializedName(value = "id", alternate = ["id_paciente"]) val id: String?,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("numero_expediente") val numeroExpediente: String?,
    val dui: String?,
    val sexo: String?,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val telefono: String? = null,
    val alergias: String? = null,
    @SerializedName("tipo_sangre") val tipoSangre: String? = null,
    val protegido: Boolean = false
)

data class ConsultorioInfo(
    @SerializedName(value = "id", alternate = ["id_consultorio"]) val id: String?,
    val nombre: String?,
    val codigo: String?
)

/**
 * Cita clínica en bloques fijos obligatorios de 30 minutos con regla estricta anti-solapamiento.
 */
data class Cita(
    @SerializedName(value = "id", alternate = ["id_cita"]) val id: String = UUID.randomUUID().toString(),
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String?,
    val estado: String,
    val motivo: String? = null,
    val paciente: PacienteInfo? = null,
    val consultorio: ConsultorioInfo? = null,
    val medico: User? = null,
    @SerializedName("id_medico") val idMedico: String? = null,
    @SerializedName("medico_nombre") val medicoNombre: String? = "Dr. Alejandro Restrepo",
    @SerializedName("especialidad") val especialidad: String? = "Medicina General",
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
) {
    companion object {
        val BLOQUES_30_MINUTOS = listOf(
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
        )

        fun calcularFin30Minutos(horaInicio: String): String {
            return try {
                val parts = horaInicio.split(":")
                if (parts.size >= 2) {
                    var h = parts[0].trim().toInt()
                    var m = parts[1].trim().toInt() + 30
                    if (m >= 60) {
                        h = (h + m / 60) % 24
                        m %= 60
                    }
                    String.format(Locale.US, "%02d:%02d", h, m)
                } else horaInicio
            } catch (_: Exception) {
                horaInicio
            }
        }

        fun hayConflictoHorario(
            citasExistentes: List<Cita>,
            medicoNombre: String?,
            fecha: String,
            horaInicio: String,
            citaIdExcluir: String? = null
        ): Boolean {
            return citasExistentes.any { cita ->
                if (cita.id == citaIdExcluir || cita.estado == "CANCELADA") return@any false
                val coincideFecha = cita.fecha == fecha
                val coincideHora = cita.horaInicio == horaInicio
                val coincideMedico = medicoNombre.isNullOrBlank() ||
                    cita.medicoNombre.isNullOrBlank() ||
                    cita.medicoNombre.equals(medicoNombre, ignoreCase = true)
                coincideFecha && coincideHora && coincideMedico
            }
        }
    }
}

/**
 * Infraestructura hospitalaria con regla estricta de 17 camas (5 VIP + 12 Semiprivadas).
 */
data class Cama(
    @SerializedName(value = "id", alternate = ["id_cama"]) val id: String = "",
    val codigo: String,
    val estado: String = "DISPONIBLE", // DISPONIBLE, OCUPADA
    val paciente: PacienteInfo? = null
) {
    val esVip: Boolean get() = codigo.startsWith("VIP-")
    val estaDisponible: Boolean get() = estado == "DISPONIBLE"
}

data class Habitacion(
    val codigo: String,
    val tipo: String, // VIP, SEMIPRIVADA
    val capacidad: Int,
    val camas: List<Cama> = emptyList()
)

data class SalaSemiprivadaUi(
    val codigoSala: String, // ej. HAB-201
    val camaA: Cama,
    val camaB: Cama
)

data class CapacidadHospitalaria(
    val totalCamas: Int = 17,
    val ocupadas: Int,
    val disponibles: Int,
    val vipCamas: List<Cama>,
    val salasSemiprivadas: List<SalaSemiprivadaUi>
) {
    val porcentajeOcupacion: Int
        get() = if (totalCamas == 0) 0 else ((ocupadas * 100.0) / totalCamas).toInt()
    val vipDisponibles: Int get() = vipCamas.count { it.estaDisponible }
    val semiDisponibles: Int
        get() = salasSemiprivadas.sumOf {
            (if (it.camaA.estaDisponible) 1 else 0) + (if (it.camaB.estaDisponible) 1 else 0)
        }
    val sobrecupoBloqueado: Boolean get() = true
}

data class Cie10Diagnostico(
    val codigo: String,
    val descripcion: String
)

data class RecetaItem(
    val id: String = UUID.randomUUID().toString(),
    val medicamento: String,
    val dosis: String,
    val frecuencia: String,
    val duracion: String,
    val indicaciones: String? = null,
    val tipo: String = "Crónico" // Crónico, SOS, Agudo
)

data class OrdenLaboratorioItem(
    @SerializedName(value = "id", alternate = ["id_orden"]) val id: String = UUID.randomUUID().toString(),
    @SerializedName("id_paciente") val pacienteId: String? = null,
    @SerializedName("paciente_nombre") val pacienteNombre: String? = null,
    @SerializedName("numero_expediente") val numeroExpediente: String? = null,
    @SerializedName("id_examen") val idExamen: String? = null,
    @SerializedName("examen_nombre") val examenNombre: String,
    @SerializedName("tipo_tubo") val tipoTubo: String? = "Tubo EDTA",
    val prioridad: String = "RUTINA", // STAT, RUTINA
    val estado: String = "En Espera",
    @SerializedName("fecha_solicitud") val fechaSolicitud: String? = null,
    @SerializedName("fecha_resultado") val fechaResultado: String? = null,
    val observaciones: String? = null,
    @SerializedName("medico_solicitante") val medicoSolicitante: String? = "Dr. Alejandro Restrepo"
) {
    companion object {
        val ESTADOS_VALIDOS = listOf(
            "En Espera",
            "Agendado",
            "En Proceso",
            "Realizado",
            "Cancelado"
        )
    }
}

enum class EstadoSemaforo {
    VERDE, // < 20 minutos
    AMBAR, // 20 - 25 minutos
    ROJO;  // > 25 minutos

    companion object {
        fun calcularPorMinutos(minutosTranscurridos: Int): EstadoSemaforo = when {
            minutosTranscurridos < 20 -> VERDE
            minutosTranscurridos in 20..25 -> AMBAR
            else -> ROJO
        }

        fun calcularPorSegundos(segundosTranscurridos: Int): EstadoSemaforo = when {
            segundosTranscurridos < 20 * 60 -> VERDE
            segundosTranscurridos <= 25 * 60 -> AMBAR
            else -> ROJO
        }
    }
}

data class ConsultaActiva(
    val id: String = "cons-01",
    val citaId: String = "c-101",
    val box: String = "Box 03",
    val medicoNombre: String = "Dr. Alejandro Restrepo",
    val especialidad: String = "Medicina Interna",
    val paciente: Paciente,
    val signosVitales: SignosVitales,
    val diagnosticosCie10: List<Cie10Diagnostico>,
    val anamnesis: String,
    val recetas: List<RecetaItem>,
    val ordenesLaboratorio: List<OrdenLaboratorioItem> = emptyList(),
    val segundosTranscurridos: Int = 18 * 60,
    val duracionTotalSegundos: Int = 30 * 60,
    val estadoSemaforo: EstadoSemaforo = EstadoSemaforo.VERDE,
    val finalizada: Boolean = false
) {
    val minutosRestantes: Int
        get() = ((duracionTotalSegundos - segundosTranscurridos) / 60).coerceAtLeast(0)

    val relojFormateado: String
        get() {
            val mins = segundosTranscurridos / 60
            val secs = segundosTranscurridos % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
}

data class AtencionHistorica(
    val fecha: String,
    val hora: String,
    val especialidad: String,
    val diagnostico: String,
    val medico: String,
    val notas: String
)

data class ExpedienteDetalle(
    val paciente: Paciente,
    val cama: String? = null,
    val signosVitales: SignosVitales,
    val alergiasCriticas: List<String> = emptyList(),
    val comorbilidades: List<String> = emptyList(),
    val atencionesPrevias: List<AtencionHistorica> = emptyList(),
    val ordenesLaboratorio: List<OrdenLaboratorioItem> = emptyList()
)

data class ReporteAmbulancia(
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("estado_paciente") val estadoPaciente: String = "ESTABLE",
    val observaciones: String? = null,
    @SerializedName("minutos_llegada") val minutosLlegada: Int? = null,
    @SerializedName("creado_en") val creadoEn: String? = null
)

data class ServicioAmbulanciaDetalle(
    val id: String,
    @SerializedName("paciente_nombre") val pacienteNombre: String? = null,
    val direccion: String? = null,
    val descripcion: String? = null,
    val prioridad: String = "ALTA",
    @SerializedName("hora_salida") val horaSalida: String? = null,
    @SerializedName("hora_llegada") val horaLlegada: String? = null,
    val estado: String = "EN_CURSO",
    @SerializedName("tripulacion_paramedico") val tripulacionParamedico: String? = "M. Peralta (Par.)",
    @SerializedName("tripulacion_chofer") val tripulacionChofer: String? = "G. Soto (Chofer)",
    val destino: String? = "Hosp. Universitario - Urgencias",
    @SerializedName("minutos_llegada") val minutosLlegada: Int? = null,
    val reportes: List<ReporteAmbulancia> = emptyList()
)

data class AmbulanciaRecurso(
    val id: String = "amb-1",
    val placa: String = "P-123456",
    val estado: String = "DISPONIBLE", // DISPONIBLE, EN_SERVICIO, EN_MANTENIMIENTO
    @SerializedName("servicio_activo") val servicioActivo: ServicioAmbulanciaDetalle? = null
)

data class PersonalGuardia(
    val id: String,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    val rol: String, // MEDICO, RECEPCIONISTA
    val cargo: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String,
    val area: String,
    val estado: String = "Activo",
    @SerializedName("relevo_en") val relevoEn: String? = null
)

data class DatosDocumentoEscaneado(
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val edad: Int,
    val fechaNacimiento: String,
    val contactoEmergencia: String
)

data class BrazaleteQrPayload(
    val uuidBrazalete: String,
    val pacienteNombre: String,
    val dni: String,
    val numeroExpediente: String,
    val codigoCama: String,
    val nivelTriage: NivelTriageManchester,
    val protegidoLey19628: Boolean,
    val firmaCriptografica: String
)

object CatalogoClinico {
    val BLOQUES_30_MINUTOS: List<String> = Cita.BLOQUES_30_MINUTOS

    val CATALOGO_ESPECIALIDADES: Map<String, List<String>> = mapOf(
        "Medicina General" to listOf("Dr. Alejandro Restrepo", "Dra. Sofía Valenzuela"),
        "Medicina Interna" to listOf("Dr. Alejandro Restrepo", "Dr. Marcos Peralta"),
        "Cardiología" to listOf("Dra. Camila Benítez", "Dr. Fernando Ruiz"),
        "Pediatría" to listOf("Dra. Elena Ramos", "Dr. Andrés Meza"),
        "Cirugía General" to listOf("Dr. Javier Morales"),
        "Ginecología y Obstetricia" to listOf("Dra. Lucía Domínguez")
    )

    val CATALOGO_CIE10: List<Cie10Diagnostico> = listOf(
        Cie10Diagnostico("I10", "Hipertensión esencial (primaria)"),
        Cie10Diagnostico("E11", "Diabetes mellitus tipo 2"),
        Cie10Diagnostico("J45", "Asma bronquial"),
        Cie10Diagnostico("R07.4", "Dolor en el pecho, no especificado"),
        Cie10Diagnostico("K29", "Gastritis y duodenitis"),
        Cie10Diagnostico("J06.9", "Infección respiratoria aguda, no especificada"),
        Cie10Diagnostico("N39.0", "Infección del tracto urinario, sitio no especificado"),
        Cie10Diagnostico("M54.5", "Lumbago no especificado")
    )

    val CATALOGO_EXAMENES_LAB: List<String> = listOf(
        "Hemograma Completo + Plaquetas",
        "Troponina I Alta Sensibilidad (hs-cTnI)",
        "Gasometría Arterial y Electrolitos",
        "Perfil Bioquímico & Renal",
        "Perfil Lipídico & Glucemia",
        "Urocultivo & Antibiograma",
        "Examen General de Orina (EGO)",
        "Dímero D Cuantitativo"
    )

    val OPCIONES_PREVISION: List<String> = listOf(
        "Fonasa Tramo D",
        "Isapre Colmena",
        "Isapre Banmédica",
        "Particular / Privado",
        "Convenio Mutual"
    )
}
