package com.cmo.hospital.core.domain.model

import com.google.gson.annotations.SerializedName

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
    val estado: String
)

/**
 * Política exclusiva de control de acceso por rol en la aplicación móvil Android.
 * Únicamente se permite el acceso operativo a MEDICO y RECEPCIONISTA.
 */
object RoleAccessPolicy {
    val ALLOWED_ANDROID_ROLES = setOf("MEDICO", "RECEPCIONISTA")

    fun isAllowedInMobileApp(rol: String?): Boolean {
        if (rol.isNullOrBlank()) return false
        return rol.uppercase() in ALLOWED_ANDROID_ROLES
    }
}
