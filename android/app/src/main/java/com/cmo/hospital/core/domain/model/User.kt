package com.cmo.hospital.core.domain.model

import com.google.gson.annotations.SerializedName

data class EspecialidadResumen(
    @SerializedName("id_especialidad") val id: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("es_sistema") val esSistema: Boolean = false,
    val activo: Boolean = true
)

/**
 * Entidad de dominio del usuario autenticado vía Laravel Sanctum.
 */
data class User(
    val id: String?,
    @SerializedName("nombre_completo") val nombreCompleto: String,
    val usuario: String,
    val email: String?,
    val rol: String,
    val cargo: String?,
    val telefono: String?,
    val estado: String,
    @SerializedName("id_especialidad") val idEspecialidad: String? = null,
    @SerializedName("especialidad_nombre") val especialidadNombre: String? = null,
    val especialidad: EspecialidadResumen? = null
) {
    val cargoPerfil: CargoPerfil
        get() = CargoAccessPolicy.resolverPerfil(this)

    /** Especialidad visible del médico (nuevo campo o cargo heredado para compatibilidad). */
    val especialidadVisible: String?
        get() = especialidadNombre?.takeIf { it.isNotBlank() }
            ?: especialidad?.nombre?.takeIf { it.isNotBlank() }
            ?: cargo?.takeIf { it.isNotBlank() }
}

enum class CargoPerfil(val etiquetaBadge: String) {
    RECEPCIONISTA("Recepción"),
    ENFERMERA("Enfermería"),
    MEDICO_CONSULTA("Médico"),
    MEDICO_EMERGENCIA("Emergencia"),
    NO_PERMITIDO("Sin acceso")
}

/**
 * Política de control de acceso por rol base (`rol`) y cargo (`cargo`) en Android.
 */
object RoleAccessPolicy {
    val ALLOWED_ANDROID_ROLES = setOf("MEDICO", "RECEPCIONISTA")

    fun isAllowedInMobileApp(rol: String?): Boolean {
        if (rol.isNullOrBlank()) return false
        return rol.uppercase() in ALLOWED_ANDROID_ROLES
    }
}

object CargoAccessPolicy {
    fun resolverPerfil(user: User?): CargoPerfil {
        if (user == null) return CargoPerfil.NO_PERMITIDO
        val rolNorm = user.rol.trim().uppercase()
        val cargoNorm = (user.cargo ?: "").trim().lowercase()
        val usuarioNorm = user.usuario.trim().lowercase()

        return when (rolNorm) {
            "RECEPCIONISTA" -> {
                val esEnfermeria = cargoNorm.contains("enfermer") ||
                    cargoNorm.contains("piso") ||
                    cargoNorm.contains("asistente") ||
                    cargoNorm.contains("paramed") ||
                    usuarioNorm.contains("enfermer")
                if (esEnfermeria) CargoPerfil.ENFERMERA else CargoPerfil.RECEPCIONISTA
            }
            "MEDICO" -> {
                val espNorm = (user.especialidadVisible ?: "").trim().lowercase()
                val esEmergencia = espNorm.contains("emergen") ||
                    espNorm.contains("urgenc") ||
                    cargoNorm.contains("emergen") ||
                    cargoNorm.contains("urgenc") ||
                    usuarioNorm.contains("emergen")
                if (esEmergencia) CargoPerfil.MEDICO_EMERGENCIA else CargoPerfil.MEDICO_CONSULTA
            }
            else -> CargoPerfil.NO_PERMITIDO
        }
    }

    fun esEnfermeria(user: User?): Boolean = resolverPerfil(user) == CargoPerfil.ENFERMERA
    fun esRecepcionista(user: User?): Boolean = resolverPerfil(user) == CargoPerfil.RECEPCIONISTA
    fun esMedicoEmergencia(user: User?): Boolean = resolverPerfil(user) == CargoPerfil.MEDICO_EMERGENCIA
}
