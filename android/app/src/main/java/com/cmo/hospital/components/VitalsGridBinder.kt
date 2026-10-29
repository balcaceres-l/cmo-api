package com.cmo.hospital.components

import android.view.View
import com.cmo.hospital.core.domain.model.SignosVitales
import com.cmo.hospital.databinding.CompVitalsChipBinding

object VitalsGridBinder {
    fun bindChip(
        binding: CompVitalsChipBinding,
        label: String,
        value: String,
        subLabel: String? = null
    ) {
        binding.tvVitalLabel.text = label
        binding.tvVitalValue.text = value
        if (!subLabel.isNullOrBlank()) {
            binding.tvVitalSub.text = subLabel
            binding.tvVitalSub.visibility = View.VISIBLE
        } else {
            binding.tvVitalSub.visibility = View.GONE
        }
    }

    fun bindAllSeven(
        paChip: CompVitalsChipBinding,
        pulsoChip: CompVitalsChipBinding,
        tempChip: CompVitalsChipBinding,
        spo2Chip: CompVitalsChipBinding,
        pesoChip: CompVitalsChipBinding,
        tallaChip: CompVitalsChipBinding,
        imcChip: CompVitalsChipBinding,
        signos: SignosVitales
    ) {
        bindChip(paChip, "PA", signos.presionTexto)
        bindChip(
            pulsoChip,
            "PULSO",
            signos.frecuenciaCardiaca?.let { "$it lpm" } ?: "N/D",
            signos.frecuenciaRespiratoria?.let { "FR: $it rpm" }
        )
        bindChip(tempChip, "TEMP", signos.temperaturaC?.let { "$it °C" } ?: "N/D")
        bindChip(spo2Chip, "SPO2", signos.saturacionOxigeno?.let { "$it %" } ?: "N/D")
        bindChip(pesoChip, "PESO", signos.pesoKg?.let { "$it kg" } ?: "N/D")
        bindChip(tallaChip, "TALLA", signos.tallaCm?.let { "$it cm" } ?: "N/D")
        bindChip(
            imcChip,
            "IMC",
            signos.imc?.toString() ?: "N/D",
            if (signos.imc != null) signos.imcClasificacion else null
        )
    }
}
