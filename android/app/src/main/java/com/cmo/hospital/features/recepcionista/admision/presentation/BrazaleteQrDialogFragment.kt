package com.cmo.hospital.features.recepcionista.admision.presentation

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.core.domain.model.BrazaleteQrPayload
import com.cmo.hospital.databinding.DialogBrazaleteQrBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BrazaleteQrDialogFragment(
    private val payload: BrazaleteQrPayload
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBrazaleteQrBinding.inflate(layoutInflater)
        binding.tvQrUuid.text = "UUID: ${payload.uuidBrazalete}"
        binding.tvQrPaciente.text = payload.pacienteNombre
        binding.tvQrDatosFiliacion.text =
            "DNI: ${payload.dni} • Exp: ${payload.numeroExpediente}"
        binding.tvQrCamaYTriage.text =
            "Ubicación: ${payload.codigoCama} • ${payload.nivelTriage.titulo}"
        binding.tvQrFirma.text = "Firma Criptográfica: ${payload.firmaCriptografica}"

        binding.btnCerrarQrDialog.setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}
