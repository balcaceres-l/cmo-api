package com.cmo.hospital.features.doctor.consulta.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.databinding.DialogAgregarRecetaBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AgregarRecetaDialogFragment : DialogFragment() {

    var onConfirmarReceta: ((
        medicamento: String,
        dosis: String,
        frecuencia: String,
        duracion: String,
        indicaciones: String?,
        tipo: String
    ) -> Boolean)? = null

    private var tipoSeleccionado: String = "Crónico"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAgregarRecetaBinding.inflate(LayoutInflater.from(requireContext()))
        val ctx = requireContext()

        listOf("Crónico", "SOS", "Agudo").forEachIndexed { idx, tipo ->
            val chip = Chip(ctx).apply {
                id = View.generateViewId()
                text = tipo
                isCheckable = true
                isChecked = idx == 0
                setOnCheckedChangeListener { _, checked ->
                    if (checked) tipoSeleccionado = tipo
                }
            }
            binding.chipGroupTipoReceta.addView(chip)
        }

        binding.btnRecetaCancelar.setOnClickListener { dismiss() }
        binding.btnRecetaAgregar.setOnClickListener {
            val med = binding.etRecetaMedicamento.text?.toString().orEmpty()
            val dosis = binding.etRecetaDosis.text?.toString().orEmpty()
            val frec = binding.etRecetaFrecuencia.text?.toString().orEmpty()
            val dur = binding.etRecetaDuracion.text?.toString().orEmpty()
            val ind = binding.etRecetaIndicaciones.text?.toString()
            val ok = onConfirmarReceta?.invoke(med, dosis, frec, dur, ind, tipoSeleccionado) ?: false
            if (ok) {
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(ctx)
            .setView(binding.root)
            .create()
    }
}
