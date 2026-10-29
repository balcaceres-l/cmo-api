package com.cmo.hospital.features.recepcionista.citas.presentation

import android.app.Dialog
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.databinding.DialogTriajeCitaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TriajeCitaDialogFragment(
    private val cita: Cita,
    private val onDecision: (SignosVitales?, Boolean) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTriajeCitaBinding.inflate(layoutInflater)
        binding.tvTriajePacienteHeader.text =
            "Paciente: ${cita.paciente?.nombreCompleto ?: "Paciente"} (${cita.consultorio?.codigo ?: "Box 03"})"

        // Prellenar si la cita ya tiene signos registrados
        cita.signosVitales?.let { sv ->
            binding.etTriajeSistolica.setText(sv.presionSistolica?.toString().orEmpty())
            binding.etTriajeDiastolica.setText(sv.presionDiastolica?.toString().orEmpty())
            binding.etTriajeFc.setText(sv.frecuenciaCardiaca?.toString().orEmpty())
            binding.etTriajeFr.setText(sv.frecuenciaRespiratoria?.toString().orEmpty())
            binding.etTriajeTemp.setText(sv.temperaturaC?.toString().orEmpty())
            binding.etTriajeSpo2.setText(sv.saturacionOxigeno?.toString().orEmpty())
            binding.etTriajePeso.setText(sv.pesoKg?.toString().orEmpty())
            binding.etTriajeTalla.setText(sv.tallaCm?.toString().orEmpty())
            binding.etTriajeObs.setText(sv.observaciones.orEmpty())
        }

        fun updateLiveImc() {
            val peso = binding.etTriajePeso.text?.toString()?.toDoubleOrNull()
            val talla = binding.etTriajeTalla.text?.toString()?.toDoubleOrNull()
            val tempSv = SignosVitales(pesoKg = peso, tallaCm = talla)
            val imc = tempSv.imc
            binding.tvTriajeImcLive.text = if (imc != null) {
                "IMC Calculado: $imc kg/m² (${tempSv.imcClasificacion})"
            } else {
                "IMC Calculado: Ingrese peso y talla (opcional)"
            }
        }

        updateLiveImc()
        binding.etTriajePeso.doAfterTextChanged { updateLiveImc() }
        binding.etTriajeTalla.doAfterTextChanged { updateLiveImc() }

        fun buildSignos(): SignosVitales? {
            val sv = SignosVitales(
                presionSistolica = binding.etTriajeSistolica.text?.toString()?.toIntOrNull(),
                presionDiastolica = binding.etTriajeDiastolica.text?.toString()?.toIntOrNull(),
                frecuenciaCardiaca = binding.etTriajeFc.text?.toString()?.toIntOrNull(),
                frecuenciaRespiratoria = binding.etTriajeFr.text?.toString()?.toIntOrNull(),
                temperaturaC = binding.etTriajeTemp.text?.toString()?.toDoubleOrNull(),
                saturacionOxigeno = binding.etTriajeSpo2.text?.toString()?.toIntOrNull(),
                pesoKg = binding.etTriajePeso.text?.toString()?.toDoubleOrNull(),
                tallaCm = binding.etTriajeTalla.text?.toString()?.toDoubleOrNull(),
                observaciones = binding.etTriajeObs.text?.toString()?.trim()?.ifBlank { null }
            )
            return if (sv.hasAnyData) sv else null
        }

        binding.btnTriajePasarAConsulta.setOnClickListener {
            onDecision(buildSignos(), true)
            dismiss()
        }
        binding.btnTriajeSoloGuardar.setOnClickListener {
            onDecision(buildSignos(), false)
            dismiss()
        }
        binding.btnTriajeCerrar.setOnClickListener {
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
