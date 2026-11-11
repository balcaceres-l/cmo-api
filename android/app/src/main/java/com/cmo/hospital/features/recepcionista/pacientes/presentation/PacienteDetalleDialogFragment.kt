package com.cmo.hospital.features.recepcionista.pacientes.presentation

import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.databinding.DialogPacienteDetalleBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Vista ampliada de la ficha del paciente en Recepción.
 * Muestra la información básica de filiación y centraliza las acciones:
 * Editar datos, generar Código App y Solicitar expediente completo.
 */
class PacienteDetalleDialogFragment(
    pacienteInicial: Paciente,
    private val onEditar: (Paciente) -> Unit,
    private val onCodigoActivacion: (Paciente) -> Unit,
    private val onSolicitarExpedienteCompleto: (Paciente) -> Unit
) : DialogFragment() {

    private var pacienteActual: Paciente = pacienteInicial
    private var _binding: DialogPacienteDetalleBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPacienteDetalleBinding.inflate(layoutInflater)
        _binding = binding

        enlazarPaciente(binding, pacienteActual)

        binding.btnDetPacCerrar.setOnClickListener { dismiss() }

        binding.btnDetPacEditar.setOnClickListener {
            onEditar(pacienteActual)
        }

        binding.btnDetPacCodigoApp.setOnClickListener {
            onCodigoActivacion(pacienteActual)
        }

        binding.btnDetPacSolicitarExpediente.setOnClickListener {
            onSolicitarExpedienteCompleto(pacienteActual)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    fun actualizarPaciente(actualizado: Paciente) {
        pacienteActual = actualizado
        _binding?.let { enlazarPaciente(it, actualizado) }
    }

    private fun enlazarPaciente(binding: DialogPacienteDetalleBinding, p: Paciente) {
        val ctx = binding.root.context
        binding.tvDetPacIniciales.text = Paciente.extraerIniciales(p.nombreCompleto)
        binding.tvDetPacNombre.text = p.nombreCompleto
        binding.tvDetPacExpediente.text = p.numeroExpediente ?: "Sin expediente"

        val tieneCuenta = p.idUsuario != null
        if (tieneCuenta) {
            binding.tvDetPacEstadoApp.text = "App vinculada"
            binding.tvDetPacEstadoApp.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
            binding.tvDetPacEstadoApp.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
        } else {
            binding.tvDetPacEstadoApp.text = "Sin vincular App"
            binding.tvDetPacEstadoApp.setBackgroundResource(R.drawable.bg_pill_secondary_fixed)
            binding.tvDetPacEstadoApp.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed))
        }

        val resp = p.responsableEfectivo
        binding.tvDetPacDui.text = when {
            !p.dui.isNullOrBlank() -> p.dui
            resp != null -> "Menor (${resp.tipoDocumento}: ${resp.documento})"
            else -> "No registrado"
        }
        binding.tvDetPacSexo.text = when (p.sexo?.uppercase()) {
            "F" -> "Femenino"
            "M" -> "Masculino"
            else -> "No especificado"
        }
        binding.tvDetPacFechaNacimiento.text = p.fechaNacimiento?.takeIf { it.isNotBlank() }
            ?.let { DateUtils.toUiDate(it) } ?: "No registrada"
        binding.tvDetPacEdad.text = if (!p.fechaNacimiento.isNullOrBlank()) {
            if (p.esMenorDeEdad) "${p.edad} años (Menor)" else "${p.edad} años"
        } else "N/D"
        binding.tvDetPacTelefono.text = p.telefono?.takeIf { it.isNotBlank() }
            ?: resp?.telefono?.takeIf { it.isNotBlank() }
            ?: "Sin teléfono registrado"
        val dirLimpia = p.direccionLimpia ?: "Sin dirección registrada"
        binding.tvDetPacDireccion.text = if (resp != null) {
            "$dirLimpia\nResponsable: ${resp.nombreCompleto} (${resp.parentesco} • Tel: ${resp.telefono})"
        } else {
            dirLimpia
        }

        binding.btnDetPacCodigoApp.isEnabled = !tieneCuenta
        binding.btnDetPacCodigoApp.text = if (tieneCuenta) "App vinculada" else "Código App"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
