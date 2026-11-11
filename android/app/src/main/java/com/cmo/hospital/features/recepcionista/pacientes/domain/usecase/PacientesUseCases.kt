package com.cmo.hospital.features.recepcionista.pacientes.domain.usecase

import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.dto.CodigoActivacion
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteInvalidoException
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacientesRepository
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

class BuscarPacientesUseCase(private val repository: PacientesRepository) {
    suspend operator fun invoke(query: String?): Result<List<Paciente>> = repository.buscar(query)
}

/** Crea si [id] es nulo; si no, actualiza. Valida antes de llamar a la API. */
class GuardarPacienteUseCase(private val repository: PacientesRepository) {
    suspend operator fun invoke(id: String?, form: PacienteForm): Result<Paciente> {
        val limpio = normalizar(form)
        validarCampo(limpio)?.let { (campo, mensaje) ->
            return Result.failure(PacienteInvalidoException(campo, mensaje))
        }
        return if (id == null) repository.crear(limpio) else repository.actualizar(id, limpio)
    }

    private fun normalizar(f: PacienteForm): PacienteForm {
        val fn = f.fechaNacimiento.trim()
        val esMenor = esMenorDeEdad(fn)
        return f.copy(
            nombreCompleto = f.nombreCompleto.trim(),
            fechaNacimiento = fn,
            sexo = f.sexo.trim().uppercase(),
            dui = if (esMenor) null else f.dui?.trim()?.uppercase()?.ifBlank { null },
            telefono = f.telefono?.trim()?.ifBlank { null },
            direccion = f.direccion?.trim()?.ifBlank { null },
            tipoSangre = f.tipoSangre?.trim()?.uppercase()?.ifBlank { null },
            alergias = f.alergias?.trim()?.ifBlank { null },
            tipoDocumento = f.tipoDocumento.trim().uppercase().ifBlank { "DUI" },
            responsableNombre = if (esMenor) f.responsableNombre?.trim()?.ifBlank { null } else null,
            responsableFechaNacimiento = if (esMenor) f.responsableFechaNacimiento?.trim()?.ifBlank { null } else null,
            responsableTipoDocumento = f.responsableTipoDocumento.trim().uppercase().ifBlank { "DUI" },
            responsableDocumento = if (esMenor) f.responsableDocumento?.trim()?.uppercase()?.ifBlank { null } else null,
            responsableTelefono = if (esMenor) f.responsableTelefono?.trim()?.ifBlank { null } else null,
            responsableParentesco = if (esMenor) f.responsableParentesco?.trim()?.ifBlank { null } else null
        )
    }

    companion object {
        private val FORMATO_DUI = Regex("^\\d{8}-\\d$")
        private val FORMATO_PASAPORTE = Regex("^[A-Za-z0-9-]{6,12}$")
        private val TIPOS_SANGRE_VALIDOS = setOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

        fun calcularEdad(fechaNacimientoIso: String, hoy: LocalDate = LocalDate.now()): Int? {
            return try {
                val nac = LocalDate.parse(fechaNacimientoIso.trim())
                if (!nac.isBefore(hoy)) null else Period.between(nac, hoy).years.coerceAtLeast(0)
            } catch (_: DateTimeParseException) {
                null
            }
        }

        fun esMenorDeEdad(fechaNacimientoIso: String, hoy: LocalDate = LocalDate.now()): Boolean {
            val edad = calcularEdad(fechaNacimientoIso, hoy) ?: return false
            return edad < 18
        }

        fun esTelefonoValido(telefono: String?): Boolean {
            val limpio = telefono?.trim().orEmpty()
            if (limpio.isBlank() || limpio.length > 25) return false
            val digitos = limpio.count { it.isDigit() }
            return digitos in 8..15 && limpio.all { it.isDigit() || it == '-' || it == ' ' || it == '+' || it == '(' || it == ')' }
        }

        fun validarDocumento(valor: String?, tipoDocumento: String): String? {
            val doc = valor?.trim().orEmpty()
            if (doc.isBlank()) return "Ingrese el número de ${if (tipoDocumento.equals("PASAPORTE", true)) "pasaporte" else "DUI"}."
            return if (tipoDocumento.equals("PASAPORTE", ignoreCase = true)) {
                if (!FORMATO_PASAPORTE.matches(doc)) {
                    "El pasaporte debe tener entre 6 y 12 caracteres alfanuméricos."
                } else null
            } else {
                if (!FORMATO_DUI.matches(doc)) {
                    "El DUI debe tener 9 dígitos (00000000-0)."
                } else null
            }
        }

        fun validar(f: PacienteForm, hoy: LocalDate = LocalDate.now()): String? = validarCampo(f, hoy)?.second

        /** Primer problema del formulario como (campo de la API, mensaje), o null si es válido. */
        fun validarCampo(f: PacienteForm, hoy: LocalDate = LocalDate.now()): Pair<String, String>? {
            if (f.nombreCompleto.isBlank()) return "nombre_completo" to "Ingrese el nombre completo del paciente."
            if (f.nombreCompleto.length > 150) return "nombre_completo" to "El nombre no puede superar 150 caracteres."
            if (f.fechaNacimiento.isBlank()) return "fecha_nacimiento" to "Seleccione la fecha de nacimiento."
            val nacimiento = try {
                LocalDate.parse(f.fechaNacimiento.trim())
            } catch (_: DateTimeParseException) {
                return "fecha_nacimiento" to "La fecha de nacimiento debe tener el formato AAAA-MM-DD."
            }
            if (!nacimiento.isBefore(hoy)) return "fecha_nacimiento" to "La fecha de nacimiento debe ser anterior a hoy."
            if (f.sexo !in setOf("M", "F")) return "sexo" to "Seleccione el sexo del paciente."

            val edad = Period.between(nacimiento, hoy).years
            val esMenor = edad < 18

            if (esMenor) {
                if (!f.dui.isNullOrBlank()) {
                    return "dui" to "En menores de edad la casilla de DUI/pasaporte del paciente debe permanecer deshabilitada; ingréselo en los datos del responsable."
                }
                if (f.responsableNombre.isNullOrBlank()) {
                    return "responsable_nombre" to "Ingrese el nombre completo del responsable."
                }
                if (f.responsableNombre.trim().length > 150) {
                    return "responsable_nombre" to "El nombre del responsable no puede superar 150 caracteres."
                }
                if (f.responsableFechaNacimiento.isNullOrBlank()) {
                    return "responsable_fecha_nacimiento" to "Seleccione la fecha de nacimiento del responsable."
                }
                val nacResp = try {
                    LocalDate.parse(f.responsableFechaNacimiento.trim())
                } catch (_: DateTimeParseException) {
                    return "responsable_fecha_nacimiento" to "La fecha de nacimiento del responsable debe tener el formato AAAA-MM-DD."
                }
                if (!nacResp.isBefore(hoy)) {
                    return "responsable_fecha_nacimiento" to "La fecha de nacimiento del responsable debe ser anterior a hoy."
                }
                if (Period.between(nacResp, hoy).years < 18) {
                    return "responsable_fecha_nacimiento" to "El responsable debe ser mayor de edad (18 años o más)."
                }
                val errorDocResp = validarDocumento(f.responsableDocumento, f.responsableTipoDocumento)
                if (errorDocResp != null) {
                    return "responsable_documento" to errorDocResp
                }
                if (f.responsableTelefono.isNullOrBlank()) {
                    return "responsable_telefono" to "Ingrese el teléfono del responsable."
                }
                if (!esTelefonoValido(f.responsableTelefono)) {
                    return "responsable_telefono" to "Ingrese un teléfono válido del responsable (mínimo 8 dígitos)."
                }
                if (f.responsableParentesco.isNullOrBlank()) {
                    return "responsable_parentesco" to "Seleccione el parentesco del responsable."
                }
            } else {
                if (f.exigirDatosCompletos && f.dui.isNullOrBlank()) {
                    return "dui" to "Ingrese el DUI o pasaporte del paciente."
                }
                if (!f.dui.isNullOrBlank()) {
                    val errorDoc = validarDocumento(f.dui, f.tipoDocumento)
                    if (errorDoc != null) return "dui" to errorDoc
                }
            }

            if (f.exigirDatosCompletos) {
                if (f.telefono.isNullOrBlank()) return "telefono" to "Ingrese el teléfono del paciente."
                if (!esTelefonoValido(f.telefono)) return "telefono" to "Ingrese un teléfono válido (mínimo 8 dígitos, ej. 7000-0000)."
                if (f.direccion.isNullOrBlank()) return "direccion" to "Ingrese la dirección del paciente."
            } else {
                if ((f.telefono?.length ?: 0) > 25) return "telefono" to "El teléfono no puede superar 25 caracteres."
            }

            if (!f.tipoSangre.isNullOrBlank()) {
                val ts = f.tipoSangre.trim().uppercase()
                if (ts.length > 5 || ts !in TIPOS_SANGRE_VALIDOS) {
                    return "tipo_sangre" to "Tipo de sangre inválido (ej. O+, O-, A+, A-, B+, B-, AB+, AB-)."
                }
            }
            return null
        }
    }
}

class GenerarCodigoActivacionUseCase(private val repository: PacientesRepository) {
    suspend operator fun invoke(paciente: Paciente): Result<CodigoActivacion> {
        val id = paciente.id ?: return Result.failure(IllegalStateException("El paciente no tiene identificador."))
        return repository.generarCodigoActivacion(id)
    }
}
