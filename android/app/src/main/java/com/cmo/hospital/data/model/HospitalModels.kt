package com.cmo.hospital.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

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
    val imc: Double? get() {
        val p = pesoKg ?: return null
        val t = tallaCm ?: return null
        if (t <= 0.0 || p <= 0.0) return null
        val alturaM = t / 100.0
        val valor = p / (alturaM * alturaM)
        return Math.round(valor * 10.0) / 10.0
    }

    val imcClasificacion: String get() {
        val valor = imc ?: return "N/D"
        return when {
            valor < 18.5 -> "Bajo peso"
            valor < 25.0 -> "Normal"
            valor < 30.0 -> "Sobrepeso"
            else -> "Obesidad"
        }
    }

    val presionTexto: String get() {
        return if (presionSistolica != null && presionDiastolica != null) {
            "$presionSistolica/$presionDiastolica mmHg"
        } else if (presionSistolica != null) {
            "$presionSistolica mmHg"
        } else "N/D"
    }

    val hasAnyData: Boolean get() = presionSistolica != null || presionDiastolica != null ||
            frecuenciaCardiaca != null || frecuenciaRespiratoria != null ||
            temperaturaC != null || pesoKg != null || tallaCm != null ||
            saturacionOxigeno != null || !observaciones.isNullOrBlank()
}

/**
 * Modelo de Paciente con fecha_nacimiento obligatoria para el backend.
 * La edad se calcula dinámicamente a partir de la fecha.
 */
data class Paciente(
    val id: String? = null,
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
    val prevision: String? = null
) {
    val edad: Int get() {
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
        } catch (e: Exception) {
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

        /**
         * Formato de expediente clínico: (LB-2026-001)
         * Primera letra del primer nombre + primera letra del primer apellido + año + correlativo
         */
        fun generarNumeroExpediente(nombreCompleto: String, year: Int = 2026, correlativo: Int = 1): String {
            val iniciales = extraerIniciales(nombreCompleto)
            return String.format(java.util.Locale.US, "%s-%d-%03d", iniciales, year, correlativo)
        }
    }
}

/**
 * Consulta y asignación lógica de espacios hospitalarios:
 * 5 VIP (Planta 3: VIP-301 a VIP-305) y 6 Semiprivadas (Planta 2: 12 camas en total).
 */
data class Cama(
    val id: String,
    val codigo: String,
    val estado: String = "DISPONIBLE", // DISPONIBLE, OCUPADA
    val paciente: PacienteInfo? = null
)

data class Habitacion(
    val codigo: String,
    val tipo: String, // VIP, SEMIPRIVADA
    val capacidad: Int,
    val camas: List<Cama> = emptyList()
)

data class IngresoPacienteRequest(
    @SerializedName("id_paciente") val idPaciente: String,
    val tipo: String, // VIP, SEMIPRIVADA
    @SerializedName("id_cama") val idCama: String? = null,
    @SerializedName("id_medico") val idMedico: String? = null,
    val motivo: String,
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
)

/**
 * Diagnóstico médico con codificación CIE-10.
 */
data class Cie10Diagnostico(
    val codigo: String,
    val descripcion: String
)

/**
 * Receta médica digital.
 */
data class RecetaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val medicamento: String,
    val dosis: String,
    val frecuencia: String,
    val duracion: String,
    val indicaciones: String? = null,
    val tipo: String = "Crónico" // Crónico, SOS, Agudo
)

/**
 * Órdenes de laboratorio clínico (LIS).
 */
data class OrdenLaboratorioItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("id_paciente") val pacienteId: String? = null,
    @SerializedName("paciente_nombre") val pacienteNombre: String? = null,
    @SerializedName("numero_expediente") val numeroExpediente: String? = null,
    @SerializedName("id_examen") val idExamen: String? = null,
    @SerializedName("examen_nombre") val examenNombre: String,
    @SerializedName("tipo_tubo") val tipoTubo: String? = "Tubo EDTA",
    val prioridad: String = "RUTINA", // STAT, RUTINA
    val estado: String = "En Espera", // En Espera, Agendado, En Proceso, Realizado, Cancelado
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

/**
 * Estado de semáforo para el tiempo de consulta médica de 30 minutos.
 */
enum class EstadoSemaforo {
    VERDE,  // < 20 minutos (Tiempo regular)
    AMBAR,  // 20 - 25 minutos (Próximo a terminar, advertencia 5m)
    ROJO    // > 25 minutos (Tiempo de consulta agotado / sobretiempo)
}

/**
 * Recurso único institucional: Ambulancia y bitácora de emergencias.
 */
data class ReporteAmbulancia(
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("estado_paciente") val estadoPaciente: String = "ESTABLE", // ESTABLE, DELICADO, CRITICO
    val observaciones: String? = null,
    @SerializedName("minutos_llegada") val minutosLlegada: Int? = null,
    @SerializedName("creado_en") val creadoEn: String? = null
)

data class ServicioAmbulanciaDetalle(
    val id: String,
    @SerializedName("paciente_nombre") val pacienteNombre: String? = null,
    val direccion: String? = null,
    val descripcion: String? = null,
    val prioridad: String = "ALTA", // BAJA, MEDIA, ALTA, CRITICA
    @SerializedName("hora_salida") val horaSalida: String? = null,
    @SerializedName("hora_llegada") val horaLlegada: String? = null,
    val estado: String = "EN_CURSO", // EN_CURSO, FINALIZADO
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

/**
 * Personal de guardia hospitalaria y turnos activos.
 */
data class PersonalGuardia(
    val id: String,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    val rol: String, // MEDICO, RECEPCIONISTA
    val cargo: String, // Urgencias, Medicina General, Enfermera Jefe, etc.
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String,
    val area: String,
    val estado: String = "Activo",
    @SerializedName("relevo_en") val relevoEn: String? = null
)
