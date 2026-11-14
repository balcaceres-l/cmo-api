package com.cmo.hospital.core.network.dto

import com.cmo.hospital.core.domain.model.EspecialidadTextoAdapter
import com.cmo.hospital.core.domain.model.PacienteInfo
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.core.domain.model.User
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val usuario: String,
    val password: String
)

data class AuthResponseData(
    val usuario: User,
    @SerializedName("id_paciente") val idPaciente: String?,
    val token: String,
    @SerializedName("token_type") val tokenType: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val errors: Map<String, List<String>>?
)

data class PaginatedData<T>(
    val data: List<T>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int
)

/** Consulta tal como la devuelve la API (`recetas` solo viene en GET consultas/{id}). */
data class ConsultaDto(
    @SerializedName("id_consulta") val id: String,
    @SerializedName("id_cita") val idCita: String?,
    val motivo: String?,
    val diagnostico: String?,
    val notas: String?,
    @SerializedName("fecha_hora_inicio") val fechaHoraInicio: String?,
    @SerializedName("fecha_hora_fin") val fechaHoraFin: String?,
    val recetas: List<RecetaDto>?,
    @SerializedName("ordenes_laboratorio") val ordenesLaboratorio: List<OrdenLaboratorioDto>? = null
)

data class ExamenDto(
    @SerializedName("id_examen") val id: String,
    val nombre: String
)

/** `paciente` y `examen` vienen cargados en GET ordenes-laboratorio. */
data class OrdenLaboratorioDto(
    @SerializedName("id_orden") val id: String,
    @SerializedName("id_consulta") val idConsulta: String?,
    @SerializedName("id_paciente") val idPaciente: String?,
    @SerializedName("id_examen") val idExamen: String?,
    val estado: String?,
    val observaciones: String?,
    @SerializedName("fecha_solicitud") val fechaSolicitud: String?,
    @SerializedName("fecha_resultado") val fechaResultado: String?,
    val paciente: PacienteInfo?,
    val examen: ExamenDto?
)

data class RecetaDto(
    @SerializedName("id_receta") val id: String,
    val medicamento: String,
    val dosis: String,
    val frecuencia: String,
    val duracion: String,
    val indicaciones: String?
)

data class CodigoActivacion(
    val codigo: String,
    @SerializedName("vence_en") val venceEn: String?
)

data class IngresoPacienteRequest(
    @SerializedName("id_paciente") val idPaciente: String,
    val tipo: String,
    @SerializedName("id_cama") val idCama: String? = null,
    @SerializedName("id_medico") val idMedico: String? = null,
    val motivo: String,
    @SerializedName("signos_vitales") val signosVitales: SignosVitales? = null
)

data class RecursoDto(
    @SerializedName("id_recurso") val id: String,
    val nombre: String,
    val categoria: String,
    val unidad: String,
    val cantidad: Int,
    @SerializedName("stock_minimo") val stockMinimo: Int,
    val ubicacion: String? = null
)

/** GET /pacientes/{id}/expediente: paciente con historial, citas/consultas, laboratorio e ingresos. */
data class ExpedienteDto(
    @SerializedName("id_paciente") val id: String,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("numero_expediente") val numeroExpediente: String?,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String?,
    val dui: String?,
    val sexo: String?,
    val telefono: String?,
    @SerializedName("tipo_sangre") val tipoSangre: String?,
    val alergias: String?,
    val historial: HistorialDto?,
    val citas: List<CitaExpedienteDto>?,
    @SerializedName("ordenes_laboratorio") val ordenesLaboratorio: List<OrdenLaboratorioDto>?,
    val ingresos: List<IngresoExpedienteDto>?
)

data class HistorialDto(
    @SerializedName("antecedentes_personales") val antecedentesPersonales: String?,
    @SerializedName("antecedentes_familiares") val antecedentesFamiliares: String?
)

data class CitaExpedienteDto(
    @SerializedName("id_cita") val id: String,
    val fecha: String?,
    @SerializedName("hora_inicio") val horaInicio: String?,
    val estado: String?,
    val motivo: String?,
    @SerializedName("id_especialidad") val idEspecialidad: String? = null,
    @JsonAdapter(EspecialidadTextoAdapter::class) val especialidad: String? = null,
    val medico: User?,
    val consulta: ConsultaDto?
)

data class IngresoExpedienteDto(
    @SerializedName("id_ingreso") val id: String,
    @SerializedName("fecha_ingreso") val fechaIngreso: String?,
    @SerializedName("fecha_alta") val fechaAlta: String?,
    val motivo: String?,
    val cama: CamaIngresoDto?
)

data class CamaIngresoDto(
    val codigo: String,
    val habitacion: HabitacionIngresoDto?
)

data class HabitacionIngresoDto(
    val tipo: String?,
    val planta: Int?
)

/** Turno del usuario (GET /mi/turnos). */
data class TurnoDto(
    @SerializedName("id_turno") val id: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String,
    val area: String?
)

/** Personal disponible en acceso rápido demo (GET /publico/personal-demo). */
data class DemoPersonalItem(
    @SerializedName("id_usuario") val idUsuario: String? = null,
    val usuario: String,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    val rol: String,
    val cargo: String? = null,
    val especialidad: String? = null,
    val categoria: String,
    val subgrupo: String,
    @SerializedName("en_turno") val enTurno: Boolean = false,
    @SerializedName("area_turno") val areaTurno: String? = null,
    @SerializedName("horario_turno") val horarioTurno: String? = null
)
