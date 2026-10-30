package com.cmo.hospital.features.doctor.laboratorio.presentation

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.DialogIndicarExamenLabBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Diálogo para indicar exámenes del catálogo del laboratorio al paciente en consulta.
 * La orden siempre nace Pendiente: el laboratorio es quien la avanza.
 */
class IndicarExamenLabDialogFragment : DialogFragment() {

    var pacienteFijo: Paciente? = null
    var pacientesDisponibles: List<Paciente> = emptyList()
    /** Nombres del catálogo real del laboratorio (GET /examenes). */
    var examenesDisponibles: List<String> = emptyList()
    var onConfirmarOrden: ((
        examenes: List<String>,
        prioridad: String,
        observaciones: String,
        paciente: Paciente?
    ) -> Unit)? = null

    private var prioridadSeleccionada: String = "RUTINA"
    private var pacienteSeleccionado: Paciente? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogIndicarExamenLabBinding.inflate(LayoutInflater.from(requireContext()))
        val ctx = requireContext()

        val fijo = pacienteFijo
        if (fijo != null) {
            pacienteSeleccionado = fijo
            binding.sectionSelectorPacienteLab.visibility = View.GONE
            binding.tvLabDialogSubtitle.text =
                "Paciente: ${fijo.nombreCompleto} • Exp: ${fijo.numeroExpediente ?: "N/D"}"
        } else if (pacientesDisponibles.isNotEmpty()) {
            binding.sectionSelectorPacienteLab.visibility = View.VISIBLE
            pacienteSeleccionado = pacientesDisponibles.first()
            pacientesDisponibles.forEachIndexed { index, pac ->
                val chip = Chip(ctx).apply {
                    id = View.generateViewId()
                    text = "${pac.nombreCompleto} (${pac.numeroExpediente ?: "S/E"})"
                    isCheckable = true
                    isChecked = index == 0
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) pacienteSeleccionado = pac
                    }
                }
                binding.chipGroupLabPacientes.addView(chip)
            }
        } else {
            binding.sectionSelectorPacienteLab.visibility = View.GONE
        }

        fun actualizarEstiloPrioridad() {
            val esStat = prioridadSeleccionada == "STAT"
            binding.btnPrioridadRutina.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    ctx,
                    if (!esStat) R.color.huc_secondary_fixed else R.color.huc_surface_lowest
                )
            )
            binding.btnPrioridadRutina.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (!esStat) R.color.huc_on_secondary_fixed else R.color.huc_on_surface_variant
                )
            )

            binding.btnPrioridadStat.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    ctx,
                    if (esStat) R.color.huc_error_container else R.color.huc_surface_lowest
                )
            )
            binding.btnPrioridadStat.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (esStat) R.color.huc_on_error_container else R.color.huc_on_surface_variant
                )
            )
        }

        actualizarEstiloPrioridad()
        binding.btnPrioridadRutina.setOnClickListener {
            prioridadSeleccionada = "RUTINA"
            actualizarEstiloPrioridad()
        }
        binding.btnPrioridadStat.setOnClickListener {
            prioridadSeleccionada = "STAT"
            actualizarEstiloPrioridad()
        }

        val checkBoxes = mutableListOf<MaterialCheckBox>()
        examenesDisponibles.forEach { nombreExamen ->
            val cb = MaterialCheckBox(ctx).apply {
                text = nombreExamen
                setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                textSize = 13f
            }
            checkBoxes.add(cb)
            binding.containerCheckboxesExamenes.addView(cb)
        }
        binding.tvLabCatalogoVacio.visibility = if (examenesDisponibles.isEmpty()) View.VISIBLE else View.GONE
        binding.btnLabConfirmar.isEnabled = examenesDisponibles.isNotEmpty()

        binding.btnLabCancelar.setOnClickListener { dismiss() }
        binding.btnLabConfirmar.setOnClickListener {
            val seleccionados = checkBoxes
                .filter { it.isChecked }
                .map { it.text.toString() }
            val obs = binding.etLabObservaciones.text?.toString()?.trim().orEmpty()
            onConfirmarOrden?.invoke(
                seleccionados,
                prioridadSeleccionada,
                obs,
                pacienteSeleccionado
            )
            if (seleccionados.isNotEmpty()) {
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(ctx)
            .setView(binding.root)
            .create()
    }
}
