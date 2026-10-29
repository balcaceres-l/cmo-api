package com.cmo.hospital.core.config

import com.cmo.hospital.BuildConfig

/**
 * Configuración global de la aplicación Android HUC Móvil / CMO.
 * Los valores provienen del archivo `.env` ubicado en la raíz de `cmo-api`
 * (o sobreescrituras en `local.properties`), inyectados a través de `BuildConfig`.
 */
object AppConfig {
    val API_BASE_URL: String = BuildConfig.API_BASE_URL
    val APP_NAME: String = BuildConfig.APP_NAME
    val APP_ENV: String = BuildConfig.APP_ENV
    val REVERB_APP_KEY: String = BuildConfig.REVERB_APP_KEY
    val REVERB_HOST: String = BuildConfig.REVERB_HOST
    val REVERB_PORT: Int = BuildConfig.REVERB_PORT
    val REVERB_SCHEME: String = BuildConfig.REVERB_SCHEME
    val IS_DEBUG: Boolean = BuildConfig.DEBUG
    val DEV_FALLBACK_HOSTS: List<String> = BuildConfig.DEV_FALLBACK_HOSTS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
