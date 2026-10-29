package com.cmo.hospital.features.doctor.consulta.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.databinding.DialogEditarSignosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditarSignosDialogFragment : DialogFragment() {

    var signosIniciales: SignosVitales = SignosVitales()
    var onGuardarSignos: ((SignosVitales) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogEditarSignosBinding.inflate(LayoutInflater.from(requireContext()))

        signosIniciales.presionSistolica?.let { binding.etDocSistolica.setText(it.toString()) }
        signosIniciales.presionDiastolica?.let { binding.etDocDiastolica.setText(it.toString()) }
        signosIniciales.frecuenciaCardiaca?.let { binding.etDocFc.setText(it.toString()) }
        signosIniciales.frecuenciaRespiratoria?.let { binding.etDocFr.setText(it.toString()) }
        signosIniciales.temperaturaC?.let { binding.etDocTemp.setText(it.toString()) }
        signosIniciales.saturacionOxigeno?.let { binding.etDocSpo2.setText(it.toString()) }
        signosIniciales.pesoKg?.let { binding.etDocPeso.setText(it.toString()) }
        signosIniciales.tallaCm?.let { binding.etDocTalla.setText(it.toString()) }
        signosIniciales.observaciones?.let { binding.etDocObsSignos.setText(it) }

        fun actualizarPreviewImc() {
            val peso = binding.etDocPeso.text?.toString()?.toDoubleOrNull()
            val talla = binding.etDocTalla.text?.toString()?.toDoubleOrNull()
            val sv = SignosVitales(pesoKg = peso, tallaCm = talla)
            binding.tvDocImcPreview.text = if (sv.imc != null) {
                "IMC Calculado: ${sv.imc} kg/m² (${sv.imcClasificacion})"
            } else {
                "IMC Calculado: Ingrese Peso y Talla"
            }
        }

        actualizarPreviewImc()
        binding.etDocPeso.doAfterTextChanged { actualizarPreviewImc() }
        binding.etDocTalla.doAfterTextChanged { actualizarPreviewImc() }

        binding.btnDocSignosCancelar.setOnClickListener { dismiss() }
        binding.btnDocSignosGuardar.setOnClickListener {
            val nuevos = SignosVitales(
                presionSistolica = binding.etDocSistolica.text?.toString()?.toIntOrNull(),
                presionDiastolica = binding.etDocDiastolica.text?.toString()?.toIntOrNull(),
                frecuenciaCardiaca = binding.etDocFc.text?.toString()?.toIntOrNull(),
                frecuenciaRespiratoria = binding.etDocFr.text?.toString()?.toIntOrNull(),
                temperaturaC = binding.etDocTemp.text?.toString()?.toDoubleOrNull(),
                saturacionOxigeno = binding.etDocSpo2.text?.toString()?.toIntOrNull(),
                pesoKg = binding.etDocPeso.text?.toString()?.toDoubleOrNull(),
                tallaCm = binding.etDocTalla.text?.toString()?.toDoubleOrNull(),
                observaciones = binding.etDocObsSignos.text?.toString()?.trim()?.ifBlank { null }
            )
            onGuardarSignos?.invoke(nuevos)
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
