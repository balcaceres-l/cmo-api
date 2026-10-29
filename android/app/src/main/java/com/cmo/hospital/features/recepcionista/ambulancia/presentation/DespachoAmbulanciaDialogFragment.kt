package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.databinding.DialogDespachoAmbulanciaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DespachoAmbulanciaDialogFragment(
    private val onDespachar: (String, String, String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogDespachoAmbulanciaBinding.inflate(layoutInflater)

        binding.btnDespachoCancelar.setOnClickListener { dismiss() }
        binding.btnDespachoConfirmar.setOnClickListener {
            val dir = binding.etDespachoDireccion.text?.toString().orEmpty()
            val pac = binding.etDespachoPaciente.text?.toString().orEmpty()
            val desc = binding.etDespachoDescripcion.text?.toString().orEmpty()
            if (dir.isBlank()) {
                binding.etDespachoDireccion.error = "Ingrese la dirección exacta"
                return@setOnClickListener
            }
            onDespachar(dir, pac, desc)
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
