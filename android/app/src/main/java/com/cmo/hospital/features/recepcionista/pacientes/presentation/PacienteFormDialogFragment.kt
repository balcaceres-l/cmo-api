package com.cmo.hospital.features.recepcionista.pacientes.presentation

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.DialogPacienteFormBinding
import com.cmo.hospital.features.recepcionista.pacientes.domain.repository.PacienteForm
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate

/**
 * Registro (paciente nulo) o edición de los datos de filiación.
 * El diálogo solo se cierra cuando la API confirma el guardado; los errores
 * se muestran dentro del propio formulario.
 */
class PacienteFormDialogFragment(
    private val paciente: Paciente?,
    private val onGuardar: (PacienteForm, onTerminado: (ErrorFormulario?) -> Unit) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPacienteFormBinding.inflate(layoutInflater)
        val esNuevo = paciente == null

        binding.tvPacFormTitulo.text = if (esNuevo) "Registrar Paciente" else "Editar Paciente"
        binding.tvPacFormSubtitulo.text = paciente?.numeroExpediente?.let { "Expediente $it" }
            ?: "El número de expediente se asigna al guardar."
        // Recepción no ve los datos clínicos de vuelta, así que solo los captura al registrar.
        binding.containerPacFormClinicos.visibility = if (esNuevo) View.VISIBLE else View.GONE
        binding.btnPacFormGuardar.text = if (esNuevo) "Registrar" else "Guardar Cambios"

        binding.etPacFormDui.addTextChangedListener(DuiTextWatcher(binding.etPacFormDui))

        paciente?.let { p ->
            binding.etPacFormNombre.setText(p.nombreCompleto)
            binding.etPacFormNacimiento.setText(p.fechaNacimiento.orEmpty())
            binding.etPacFormDui.setText(p.dui.orEmpty())
            binding.etPacFormTelefono.setText(p.telefono.orEmpty())
            binding.etPacFormDireccion.setText(p.direccion.orEmpty())
        }
        binding.toggleSexo.check(if (paciente?.sexo == "M") R.id.btnSexoM else R.id.btnSexoF)

        val camposPorNombre: Map<String, TextInputLayout> = mapOf(
            "nombre_completo" to binding.tilPacFormNombre,
            "fecha_nacimiento" to binding.tilPacFormNacimiento,
            "dui" to binding.tilPacFormDui,
            "telefono" to binding.tilPacFormTelefono,
            "direccion" to binding.tilPacFormDireccion,
            "tipo_sangre" to binding.tilPacFormTipoSangre,
            "alergias" to binding.tilPacFormAlergias
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

        // Al corregir un campo se quita su marca de error.
        camposPorNombre.values.forEach { campo ->
            campo.editText?.doAfterTextChanged { campo.error = null }
        }

        binding.etPacFormNacimiento.setOnClickListener {
            val actual = runCatching { LocalDate.parse(binding.etPacFormNacimiento.text.toString()) }
                .getOrDefault(LocalDate.now().minusYears(30))
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.etPacFormNacimiento.setText(LocalDate.of(y, m + 1, d).toString())
            }, actual.year, actual.monthValue - 1, actual.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }

        binding.btnPacFormGuardar.setOnClickListener {
            limpiarErrores()
            val form = PacienteForm(
                nombreCompleto = binding.etPacFormNombre.text?.toString().orEmpty(),
                fechaNacimiento = binding.etPacFormNacimiento.text?.toString().orEmpty(),
                sexo = if (binding.toggleSexo.checkedButtonId == R.id.btnSexoM) "M" else "F",
                dui = binding.etPacFormDui.text?.toString(),
                telefono = binding.etPacFormTelefono.text?.toString(),
                direccion = binding.etPacFormDireccion.text?.toString(),
                tipoSangre = if (esNuevo) binding.etPacFormTipoSangre.text?.toString() else null,
                alergias = if (esNuevo) binding.etPacFormAlergias.text?.toString() else null
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
