package com.cmo.hospital.features.recepcionista.admision.domain.usecase

import com.cmo.hospital.core.domain.model.*
import com.cmo.hospital.features.recepcionista.admision.domain.repository.AdmisionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ObserveCapacidadCamasUseCase(
    private val repository: AdmisionRepository
) {
    operator fun invoke(): Flow<CapacidadHospitalaria> =
        repository.camasFlow.map { repository.obtenerCapacidadActual() }
}

class SimularEscanerDocumentoUseCase {
    suspend operator fun invoke(delayMs: Long = 650L): DatosDocumentoEscaneado {
        if (delayMs > 0) delay(delayMs)
        val edad = 34
        return DatosDocumentoEscaneado(
            dni = "17.489.201-3",
            nombres = "Francisca Elena",
            apellidos = "Navarro Silva",
            edad = edad,
            fechaNacimiento = Paciente.calcularFechaNacimientoDesdeEdad(edad),
            contactoEmergencia = "Carlos Navarro (Hermano) +56 9 7654 3210"
        )
    }
}

class RegistrarIngresoPacienteUseCase(
    private val repository: AdmisionRepository
) {
    suspend operator fun invoke(
        dni: String,
        nombres: String,
        apellidos: String,
        edadTexto: String,
        prevision: String,
        contactoEmergencia: String,
        protegidoLey19628: Boolean,
        motivoIngreso: String,
        nivelTriage: NivelTriageManchester,
        esEmergenciaDirecta: Boolean,
        codigoCamaSeleccionada: String?,
        signosVitales: SignosVitales?
    ): Result<Paciente> {
        val nombreCompleto = listOf(nombres.trim(), apellidos.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (nombreCompleto.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Complete los nombres y apellidos del paciente en Datos de Filiación.")
            )
        }

        if (!esEmergenciaDirecta && codigoCamaSeleccionada.isNullOrBlank()) {
            val disp = repository.obtenerCapacidadActual().disponibles
            return Result.failure(
                IllegalStateException(
                    "Debe seleccionar una de las $disp camas disponibles del sistema (o activar Pase de Emergencia Directa)."
                )
            )
        }

        val edadInt = edadTexto.trim().toIntOrNull()
        val fechaNac = edadInt?.let { Paciente.calcularFechaNacimientoDesdeEdad(it) } ?: "1990-01-01"
        val motivoFinal = motivoIngreso.trim().ifBlank {
            "Ingreso clínico (${nivelTriage.titulo})"
        }

        val paciente = Paciente(
            nombreCompleto = nombreCompleto,
            fechaNacimiento = fechaNac,
            dui = dni.trim().ifBlank { "S/D" },
            protegido = protegidoLey19628,
            contactoEmergencia = contactoEmergencia.trim().ifBlank { null },
            prevision = prevision,
            nivelTriage = nivelTriage
        )

        return repository.registrarEIngresarPaciente(
            paciente = paciente,
            signosVitales = signosVitales?.takeIf { it.hasAnyData },
            codigoCama = if (esEmergenciaDirecta) null else codigoCamaSeleccionada,
            motivoIngreso = motivoFinal,
            esEmergenciaDirecta = esEmergenciaDirecta
        )
    }
}

class GenerarBrazaleteQrUseCase {
    operator fun invoke(
        dni: String,
        nombres: String,
        apellidos: String,
        codigoCama: String?,
        esEmergenciaDirecta: Boolean,
        nivelTriage: NivelTriageManchester,
        protegidoLey19628: Boolean
    ): Result<BrazaleteQrPayload> {
        val ubicacion = when {
            !codigoCama.isNullOrBlank() -> codigoCama
            esEmergenciaDirecta -> "BOX-03-URGENCIAS"
            else -> return Result.failure(
                IllegalStateException("Seleccione primero una cama para estampar la ubicación en el brazalete QR.")
            )
        }
        val nombreCompleto = listOf(nombres.trim(), apellidos.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Paciente En Admisión" }

        val uuid = UUID.randomUUID().toString().uppercase()
        val exp = Paciente.generarNumeroExpediente(nombreCompleto, 2026, (10..99).random())
        return Result.success(
            BrazaleteQrPayload(
                uuidBrazalete = uuid.take(18),
                pacienteNombre = if (protegidoLey19628) {
                    Paciente.anonimizarNombre(nombreCompleto)
                } else {
                    nombreCompleto
                },
                dni = dni.trim().ifBlank { "PENDIENTE" },
                numeroExpediente = exp,
                codigoCama = ubicacion,
                nivelTriage = nivelTriage,
                protegidoLey19628 = protegidoLey19628,
                firmaCriptografica = "SHA256-HUC-${uuid.takeLast(10)}"
            )
        )
    }
}
