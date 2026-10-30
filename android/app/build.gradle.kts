import java.io.File
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// ─────────────────────────────────────────────────────────────────────────────
// Carga de variables de entorno (.env) y configuración local (local.properties)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lee y parsea un archivo de variables de entorno (.env).
 * Soporta comentarios (#), líneas vacías, valores con comillas y expansión de variables ${VAR}.
 */
fun parseEnvFile(file: File): Map<String, String> {
    if (!file.exists() || !file.isFile) return emptyMap()

    val rawMap = mutableMapOf<String, String>()
    file.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isNotEmpty() && !line.startsWith("#")) {
            val delimiterIndex = line.indexOf('=')
            if (delimiterIndex != -1) {
                val key = line.substring(0, delimiterIndex).trim()
                var value = line.substring(delimiterIndex + 1).trim()
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length - 1)
                }
                rawMap[key] = value
            }
        }
    }

    // Resolución de referencias a otras variables (ej. VITE_REVERB_APP_KEY="${REVERB_APP_KEY}")
    val resolvedMap = mutableMapOf<String, String>()
    val placeholderRegex = Regex("""\$\{([A-Za-z0-9_]+)\}""")
    for ((k, v) in rawMap) {
        var resolved = v
        placeholderRegex.findAll(v).forEach { matchResult ->
            val refKey = matchResult.groupValues[1]
            val refVal = rawMap[refKey] ?: System.getenv(refKey) ?: ""
            resolved = resolved.replace(matchResult.value, refVal)
        }
        resolvedMap[k] = resolved
    }
    return resolvedMap
}

// 1. Buscar .env un nivel atrás (en cmo-api/.env) o en la raíz de android/ como fallback
val envFileCandidates = listOf(
    rootProject.projectDir.parentFile?.resolve(".env"), // cmo-api/.env
    rootProject.file(".env")                            // android/.env
)
val activeEnvFile = envFileCandidates.firstOrNull { it != null && it.exists() && it.isFile }
val envMap = if (activeEnvFile != null) parseEnvFile(activeEnvFile) else emptyMap()

if (activeEnvFile != null) {
    logger.lifecycle("-> Cargando configuración .env desde: ${activeEnvFile.canonicalPath}")
} else {
    logger.lifecycle("-> No se encontró archivo .env en candidatos, usando valores por defecto")
}

// 2. Cargar local.properties (para sobreescrituras locales del desarrollador / SDK)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

/**
 * Obtiene un parámetro considerando la prioridad:
 * 1. local.properties (sobreescritura local de máquina)
 * 2. .env (ubicado en cmo-api/.env)
 * 3. default (valor de contingencia)
 */
fun resolveProperty(envKey: String, localPropKey: String? = null, default: String = ""): String {
    val fromLocal = localPropKey?.let { localProperties.getProperty(it) }
        ?: localProperties.getProperty(envKey)
    if (!fromLocal.isNullOrBlank()) return fromLocal

    val fromEnv = envMap[envKey] ?: localPropKey?.let { envMap[it] }
    if (!fromEnv.isNullOrBlank()) return fromEnv

    return default
}

// Resolver URL base de la API REST:
val rawApiBaseUrl = resolveProperty("API_BASE_URL", "api.baseUrl", "")
val apiBaseUrl: String = when {
    rawApiBaseUrl.isNotBlank() -> {
        if (rawApiBaseUrl.endsWith("/")) rawApiBaseUrl else "$rawApiBaseUrl/"
    }
    envMap.containsKey("APP_URL") -> {
        val appUrl = envMap["APP_URL"]!!.trim().trimEnd('/')
        "$appUrl/api/v1/"
    }
    else -> "http://10.0.2.2:8000/api/v1/"
}

// Resolver configuración Reverb (WebSockets en tiempo real)
val reverbAppKey = resolveProperty("REVERB_APP_KEY", "reverb.appKey", "hospital-universitario-key")
val reverbHost = resolveProperty("REVERB_HOST", "reverb.host", "10.0.2.2")
val reverbPort = resolveProperty("REVERB_PORT", "reverb.port", "8080").toIntOrNull() ?: 8080
val reverbScheme = resolveProperty("REVERB_SCHEME", "reverb.scheme", "http")

// Resolver metadatos de la aplicación
val appName = resolveProperty("APP_NAME", "app.name", "Hospital Universitario API")
val appEnv = resolveProperty("APP_ENV", "app.env", "local")

// Detectar IPs LAN del equipo host como respaldo automático en depuración Wi-Fi (si no hay cable USB / adb reverse)
val detectedLanHosts: String = try {
    NetworkInterface.getNetworkInterfaces()
        ?.toList()
        .orEmpty()
        .filter { it.isUp && !it.isLoopback && !it.name.startsWith("docker") && !it.name.startsWith("br-") }
        .flatMap { iface ->
            iface.inetAddresses.toList()
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                .map { it.hostAddress }
        }
        .distinct()
        .sortedBy { ip ->
            when {
                ip.startsWith("192.168.") -> 0
                ip.startsWith("10.") && !ip.startsWith("100.") -> 1
                else -> 2
            }
        }
        .joinToString(",")
} catch (_: Exception) {
    ""
}

android {
    namespace = "com.cmo.hospital"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cmo.hospital"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Exponer variables de entorno en BuildConfig
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "APP_NAME", "\"$appName\"")
        buildConfigField("String", "APP_ENV", "\"$appEnv\"")
        buildConfigField("String", "REVERB_APP_KEY", "\"$reverbAppKey\"")
        buildConfigField("String", "REVERB_HOST", "\"$reverbHost\"")
        buildConfigField("int", "REVERB_PORT", "$reverbPort")
        buildConfigField("String", "REVERB_SCHEME", "\"$reverbScheme\"")
        buildConfigField("String", "DEV_FALLBACK_HOSTS", "\"$detectedLanHosts\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.setSrcDirs(
                listOf(
                    "src/main/res",
                    "src/main/res-components",
                    "src/main/res-features/auth",
                    "src/main/res-features/recepcionista/shell",
                    "src/main/res-features/recepcionista/citas",
                    "src/main/res-features/recepcionista/pacientes",
                    "src/main/res-features/recepcionista/laboratorio",
                    "src/main/res-features/recepcionista/admision",
                    "src/main/res-features/recepcionista/ambulancia",
                    "src/main/res-features/doctor/shell",
                    "src/main/res-features/doctor/inicio",
                    "src/main/res-features/doctor/consulta",
                    "src/main/res-features/doctor/expedientes",
                    "src/main/res-features/doctor/laboratorio"
                )
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Vistas XML (Material 3, AppCompat, Fragments, RecyclerView, ConstraintLayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Red y Serialización (Retrofit, OkHttp, Gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Asincronía y Almacenamiento seguro
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Automatizar reenvío de puertos USB (API 8000 y Reverb 8080) tanto en assemble* (botón Run ▶ de Android Studio) como en install*
tasks.matching { it.name.startsWith("install") || it.name.startsWith("assemble") }.configureEach {
    doLast {
        try {
            val adb = android.adbExecutable
            if (adb.exists()) {
                project.exec {
                    commandLine(adb.absolutePath, "reverse", "tcp:8000", "tcp:8000")
                    isIgnoreExitValue = true
                }
                project.exec {
                    commandLine(adb.absolutePath, "reverse", "tcp:8080", "tcp:8080")
                    isIgnoreExitValue = true
                }
                project.logger.lifecycle("-> adb reverse tcp:8000 / tcp:8080 aplicado para depuración USB")
            }
        } catch (_: Exception) {}
    }
}
