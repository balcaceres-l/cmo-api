package com.cmo.hospital.features.recepcionista.pacientes.domain.usecase

import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.network.dto.CodigoActivacion
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteInvalidoException
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacientesRepository
import java.time.LocalDate
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

    private fun normalizar(f: PacienteForm) = f.copy(
        nombreCompleto = f.nombreCompleto.trim(),
        fechaNacimiento = f.fechaNacimiento.trim(),
        sexo = f.sexo.trim().uppercase(),
        dui = f.dui?.trim()?.ifBlank { null },
        telefono = f.telefono?.trim()?.ifBlank { null },
        direccion = f.direccion?.trim()?.ifBlank { null },
        tipoSangre = f.tipoSangre?.trim()?.uppercase()?.ifBlank { null },
        alergias = f.alergias?.trim()?.ifBlank { null }
    )

    companion object {
        private val FORMATO_DUI = Regex("^\\d{8}-\\d$")

        fun validar(f: PacienteForm, hoy: LocalDate = LocalDate.now()): String? = validarCampo(f, hoy)?.second

        /** Primer problema del formulario como (campo de la API, mensaje), o null si es válido. */
        fun validarCampo(f: PacienteForm, hoy: LocalDate = LocalDate.now()): Pair<String, String>? {
            if (f.nombreCompleto.isBlank()) return "nombre_completo" to "Ingrese el nombre completo del paciente."
            if (f.nombreCompleto.length > 150) return "nombre_completo" to "El nombre no puede superar 150 caracteres."
            if (f.fechaNacimiento.isBlank()) return "fecha_nacimiento" to "Seleccione la fecha de nacimiento."
            val nacimiento = try {
                LocalDate.parse(f.fechaNacimiento)
            } catch (_: DateTimeParseException) {
                return "fecha_nacimiento" to "La fecha de nacimiento debe tener el formato AAAA-MM-DD."
            }
            if (!nacimiento.isBefore(hoy)) return "fecha_nacimiento" to "La fecha de nacimiento debe ser anterior a hoy."
            if (f.sexo !in setOf("M", "F")) return "sexo" to "Seleccione el sexo del paciente."
            if (f.dui != null && !FORMATO_DUI.matches(f.dui)) return "dui" to "El DUI debe tener 9 dígitos (00000000-0)."
            if ((f.telefono?.length ?: 0) > 25) return "telefono" to "El teléfono no puede superar 25 caracteres."
            if ((f.tipoSangre?.length ?: 0) > 5) return "tipo_sangre" to "Tipo de sangre inválido (ej. O+)."
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
