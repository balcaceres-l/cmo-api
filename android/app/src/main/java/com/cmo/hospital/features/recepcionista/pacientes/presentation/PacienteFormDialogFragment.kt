package com.cmo.hospital.features.recepcionista.pacientes.presentation

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.databinding.DialogPacienteFormBinding
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.cmo.hospital.features.recepcionista.pacientes.domain.usecase.GuardarPacienteUseCase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

/**
 * Registro (paciente nulo) o edición de los datos de filiación.
 * Soporta detección automática de menores de edad (< 18 años), deshabilitando la casilla
 * de DUI/Pasaporte del paciente y exigiendo los datos completos del adulto responsable.
 */
class PacienteFormDialogFragment(
    private val paciente: Paciente?,
    private val onGuardar: (PacienteForm, onTerminado: (ErrorFormulario?) -> Unit) -> Unit
) : DialogFragment() {

    companion object {
        val OPCIONES_PARENTESCO = listOf(
            "Madre",
            "Padre",
            "Abuelo/a",
            "Tío/a",
            "Hermano/a mayor",
            "Tutor legal"
        )
        val OPCIONES_TIPO_SANGRE = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPacienteFormBinding.inflate(layoutInflater)
        val esNuevo = paciente == null

        binding.tvPacFormTitulo.text = if (esNuevo) "Registrar Paciente" else "Editar Paciente"
        binding.tvPacFormSubtitulo.text = paciente?.numeroExpediente?.let { "Expediente $it" }
            ?: "El número de expediente se asigna al guardar."
        binding.containerPacFormClinicos.visibility = if (esNuevo) View.VISIBLE else View.GONE
        binding.btnPacFormGuardar.text = if (esNuevo) "Registrar Paciente" else "Guardar Cambios"

        // Catálogos desplegables
        binding.actvRespParentesco.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, OPCIONES_PARENTESCO)
        )
        binding.actvRespParentesco.setOnClickListener { binding.actvRespParentesco.showDropDown() }

        binding.etPacFormTipoSangre.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, OPCIONES_TIPO_SANGRE)
        )
        binding.etPacFormTipoSangre.setOnClickListener { binding.etPacFormTipoSangre.showDropDown() }

        // Selectores de tipo de documento (DUI / Pasaporte)
        binding.toggleTipoDocPaciente.check(R.id.btnDocDui)
        binding.toggleTipoDocResp.check(R.id.btnRespDocDui)

        fun tipoDocPaciente(): String =
            if (binding.toggleTipoDocPaciente.checkedButtonId == R.id.btnDocPasaporte) "PASAPORTE" else "DUI"

        fun tipoDocResponsable(): String =
            if (binding.toggleTipoDocResp.checkedButtonId == R.id.btnRespDocPasaporte) "PASAPORTE" else "DUI"

        // Formateo condicional para DUI en el campo del paciente y del responsable
        var formateandoDocPac = false
        binding.etPacFormDui.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (formateandoDocPac || s == null || !binding.etPacFormDui.isEnabled) return
                if (tipoDocPaciente() == "DUI") {
                    val f = DuiTextWatcher.formatear(s.toString())
                    if (f != s.toString()) {
                        formateandoDocPac = true
                        s.replace(0, s.length, f)
                        binding.etPacFormDui.setSelection(f.length)
                        formateandoDocPac = false
                    }
                }
            }
        })

        var formateandoDocResp = false
        binding.etRespDocumento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (formateandoDocResp || s == null) return
                if (tipoDocResponsable() == "DUI") {
                    val f = DuiTextWatcher.formatear(s.toString())
                    if (f != s.toString()) {
                        formateandoDocResp = true
                        s.replace(0, s.length, f)
                        binding.etRespDocumento.setSelection(f.length)
                        formateandoDocResp = false
                    }
                }
            }
        })

        fun actualizarEstadoMenorDeEdad() {
            val isoNac = DateUtils.toIsoDate(binding.etPacFormNacimiento.text?.toString().orEmpty())
            val edad = GuardarPacienteUseCase.calcularEdad(isoNac)
            val esMenor = edad != null && edad < 18

            if (edad != null) {
                binding.tvPacFormEdadBadge.visibility = View.VISIBLE
                binding.tvPacFormEdadBadge.text = if (esMenor) {
                    "Edad calculada: $edad años • Menor de edad (se requieren datos del responsable)"
                } else {
                    "Edad calculada: $edad años • Mayor de edad"
                }
            } else {
                binding.tvPacFormEdadBadge.visibility = View.GONE
            }

            if (esMenor) {
                // Deshabilitar casilla de DUI/Pasaporte del paciente para que se ingrese en el responsable
                binding.tilPacFormDui.error = null
                if (!binding.etPacFormDui.text.isNullOrBlank()) {
                    binding.etPacFormDui.setText("")
                }
                binding.etPacFormDui.isEnabled = false
                binding.tilPacFormDui.isEnabled = false
                binding.btnDocDui.isEnabled = false
                binding.btnDocPasaporte.isEnabled = false
                binding.tilPacFormDui.hint = "DUI o Pasaporte (deshabilitado por ser menor)"
                binding.tilPacFormDui.helperText =
                    "Menor de edad: ingrese el DUI o pasaporte en la sección Datos del Responsable."
                binding.containerPacFormResponsable.visibility = View.VISIBLE
            } else {
                binding.etPacFormDui.isEnabled = true
                binding.tilPacFormDui.isEnabled = true
                binding.btnDocDui.isEnabled = true
                binding.btnDocPasaporte.isEnabled = true
                val esPasaporte = tipoDocPaciente() == "PASAPORTE"
                binding.tilPacFormDui.hint = if (esPasaporte) "Pasaporte del paciente *" else "DUI del paciente *"
                binding.tilPacFormDui.helperText = if (esPasaporte) {
                    "Alfanumérico de 6 a 12 caracteres"
                } else {
                    "El guion se agrega solo: 00000000-0"
                }
                binding.containerPacFormResponsable.visibility = View.GONE
            }
        }

        binding.toggleTipoDocPaciente.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) {
                binding.etPacFormDui.setText("")
                binding.tilPacFormDui.error = null
                actualizarEstadoMenorDeEdad()
            }
        }

        binding.toggleTipoDocResp.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                binding.etRespDocumento.setText("")
                binding.tilRespDocumento.error = null
                val esPasaporte = checkedId == R.id.btnRespDocPasaporte
                binding.tilRespDocumento.hint =
                    if (esPasaporte) "Pasaporte del responsable *" else "DUI del responsable *"
                binding.tilRespDocumento.helperText = if (esPasaporte) {
                    "Alfanumérico de 6 a 12 caracteres"
                } else {
                    "Formato DUI: 00000000-0"
                }
            }
        }

        // Precargar datos si es edición
        paciente?.let { p ->
            binding.etPacFormNombre.setText(p.nombreCompleto)
            binding.etPacFormNacimiento.setText(DateUtils.toUiDate(p.fechaNacimiento))
            val docPac = p.dui.orEmpty()
            val esPasaportePac = p.tipoDocumento.equals("PASAPORTE", true) ||
                (docPac.isNotBlank() && !docPac.contains("-"))
            binding.toggleTipoDocPaciente.check(if (esPasaportePac) R.id.btnDocPasaporte else R.id.btnDocDui)
            binding.etPacFormDui.setText(docPac)
            binding.etPacFormTelefono.setText(p.telefono.orEmpty())
            binding.etPacFormDireccion.setText(p.direccionLimpia ?: p.direccion.orEmpty())

            p.responsableEfectivo?.let { resp ->
                binding.etRespNombre.setText(resp.nombreCompleto)
                resp.fechaNacimiento?.let { binding.etRespNacimiento.setText(DateUtils.toUiDate(it)) }
                val esPasaporteResp = resp.tipoDocumento.equals("PASAPORTE", true)
                binding.toggleTipoDocResp.check(if (esPasaporteResp) R.id.btnRespDocPasaporte else R.id.btnRespDocDui)
                binding.etRespDocumento.setText(resp.documento)
                binding.etRespTelefono.setText(resp.telefono)
                binding.actvRespParentesco.setText(resp.parentesco, false)
            }
        }
        binding.toggleSexo.check(if (paciente?.sexo == "M") R.id.btnSexoM else R.id.btnSexoF)
        actualizarEstadoMenorDeEdad()

        val camposPorNombre: Map<String, TextInputLayout> = mapOf(
            "nombre_completo" to binding.tilPacFormNombre,
            "fecha_nacimiento" to binding.tilPacFormNacimiento,
            "dui" to binding.tilPacFormDui,
            "telefono" to binding.tilPacFormTelefono,
            "direccion" to binding.tilPacFormDireccion,
            "tipo_sangre" to binding.tilPacFormTipoSangre,
            "alergias" to binding.tilPacFormAlergias,
            "responsable_nombre" to binding.tilRespNombre,
            "responsable_fecha_nacimiento" to binding.tilRespNacimiento,
            "responsable_documento" to binding.tilRespDocumento,
            "responsable_telefono" to binding.tilRespTelefono,
            "responsable_parentesco" to binding.tilRespParentesco
        )

        fun limpiarErrores() {
            binding.tvPacFormError.visibility = View.GONE
            camposPorNombre.values.forEach { it.error = null }
        }

        fun mostrarError(error: ErrorFormulario) {
            binding.tvPacFormError.text = error.mensaje
            binding.tvPacFormError.visibility = View.VISIBLE
            camposPorNombre[error.campo]?.let { campo ->
                campo.error = error.mensaje
                campo.editText?.requestFocus()
            }
            binding.scrollPacForm.post { binding.scrollPacForm.smoothScrollTo(0, 0) }
        }

        // Validaciones inmediatas al escribir en cada campo
        binding.etPacFormNombre.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilPacFormNombre.error = when {
                v.isEmpty() -> null
                v.length < 3 -> "Ingrese el nombre y apellido del paciente."
                else -> null
            }
        }
        binding.etPacFormDui.doAfterTextChanged { text ->
            if (!binding.etPacFormDui.isEnabled) {
                binding.tilPacFormDui.error = null
                return@doAfterTextChanged
            }
            val v = text?.toString()?.trim().orEmpty()
            binding.tilPacFormDui.error = if (v.isEmpty()) null else GuardarPacienteUseCase.validarDocumento(v, tipoDocPaciente())
        }
        binding.etPacFormTelefono.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilPacFormTelefono.error = if (v.isEmpty() || GuardarPacienteUseCase.esTelefonoValido(v)) {
                null
            } else {
                "Ingrese al menos 8 dígitos (ej. 7000-0000)."
            }
        }
        binding.etPacFormDireccion.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilPacFormDireccion.error = if (v.isNotEmpty() && v.length < 4) {
                "Ingrese una dirección válida."
            } else null
        }
        binding.etRespNombre.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilRespNombre.error = if (v.isNotEmpty() && v.length < 3) {
                "Ingrese el nombre completo del responsable."
            } else null
        }
        binding.etRespDocumento.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilRespDocumento.error = if (v.isEmpty()) null else GuardarPacienteUseCase.validarDocumento(v, tipoDocResponsable())
        }
        binding.etRespTelefono.doAfterTextChanged { text ->
            val v = text?.toString()?.trim().orEmpty()
            binding.tilRespTelefono.error = if (v.isEmpty() || GuardarPacienteUseCase.esTelefonoValido(v)) {
                null
            } else {
                "Ingrese un teléfono válido (mínimo 8 dígitos)."
            }
        }
        binding.actvRespParentesco.doAfterTextChanged { binding.tilRespParentesco.error = null }
        binding.etPacFormTipoSangre.doAfterTextChanged { binding.tilPacFormTipoSangre.error = null }

        // Selector de Fecha de Nacimiento del Paciente
        binding.etPacFormNacimiento.setOnClickListener {
            val isoActual = DateUtils.toIsoDate(binding.etPacFormNacimiento.text?.toString())
            val actual = runCatching { LocalDate.parse(isoActual) }
                .getOrDefault(LocalDate.now().minusYears(25))
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val seleccion = LocalDate.of(y, m + 1, d)
                binding.etPacFormNacimiento.setText(DateUtils.toUiDate(seleccion.toString()))
                binding.tilPacFormNacimiento.error = null
                actualizarEstadoMenorDeEdad()
            }, actual.year, actual.monthValue - 1, actual.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis() - 86_400_000L
            }.show()
        }

        // Selector de Fecha de Nacimiento del Responsable (debe ser mayor de 18 años)
        binding.etRespNacimiento.setOnClickListener {
            val isoActual = DateUtils.toIsoDate(binding.etRespNacimiento.text?.toString())
            val actual = runCatching { LocalDate.parse(isoActual) }
                .getOrDefault(LocalDate.now().minusYears(35))
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val seleccion = LocalDate.of(y, m + 1, d)
                binding.etRespNacimiento.setText(DateUtils.toUiDate(seleccion.toString()))
                val edadResp = GuardarPacienteUseCase.calcularEdad(seleccion.toString())
                binding.tilRespNacimiento.error = if (edadResp != null && edadResp < 18) {
                    "El responsable debe ser mayor de edad (18 años o más)."
                } else null
            }, actual.year, actual.monthValue - 1, actual.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis() - 86_400_000L
            }.show()
        }

        binding.btnPacFormGuardar.setOnClickListener {
            limpiarErrores()
            val isoNacPaciente = DateUtils.toIsoDate(binding.etPacFormNacimiento.text?.toString().orEmpty())
            val esMenor = GuardarPacienteUseCase.esMenorDeEdad(isoNacPaciente)
            val form = PacienteForm(
                nombreCompleto = binding.etPacFormNombre.text?.toString().orEmpty(),
                fechaNacimiento = isoNacPaciente,
                sexo = if (binding.toggleSexo.checkedButtonId == R.id.btnSexoM) "M" else "F",
                dui = if (esMenor) null else binding.etPacFormDui.text?.toString(),
                telefono = binding.etPacFormTelefono.text?.toString(),
                direccion = binding.etPacFormDireccion.text?.toString(),
                tipoSangre = if (esNuevo) binding.etPacFormTipoSangre.text?.toString() else null,
                alergias = if (esNuevo) binding.etPacFormAlergias.text?.toString() else null,
                tipoDocumento = tipoDocPaciente(),
                responsableNombre = if (esMenor) binding.etRespNombre.text?.toString() else null,
                responsableFechaNacimiento = if (esMenor) {
                    DateUtils.toIsoDate(binding.etRespNacimiento.text?.toString().orEmpty())
                } else null,
                responsableTipoDocumento = tipoDocResponsable(),
                responsableDocumento = if (esMenor) binding.etRespDocumento.text?.toString() else null,
                responsableTelefono = if (esMenor) binding.etRespTelefono.text?.toString() else null,
                responsableParentesco = if (esMenor) binding.actvRespParentesco.text?.toString() else null,
                exigirDatosCompletos = true
            )
            binding.btnPacFormGuardar.isEnabled = false
            onGuardar(form) { error ->
                binding.btnPacFormGuardar.isEnabled = true
                when {
                    error != null -> mostrarError(error)
                    isAdded -> dismissAllowingStateLoss()
                }
            }
        }
        binding.btnPacFormCancelar.setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
