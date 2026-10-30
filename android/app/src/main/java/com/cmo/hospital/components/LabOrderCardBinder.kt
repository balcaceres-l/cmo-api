package com.cmo.hospital.components

import android.content.res.ColorStateList
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.databinding.CompLabOrderCardBinding

object LabOrderCardBinder {
    fun bind(
        binding: CompLabOrderCardBinding,
        orden: OrdenLaboratorioItem,
        showPatientLine: Boolean = true,
        onStatusChanged: (String, String) -> Unit
    ) {
        val ctx = binding.root.context
        val isStat = orden.prioridad.equals("STAT", ignoreCase = true)

        binding.tvLabPriorityBadge.text = if (isStat) "⚡ STAT" else "RUTINA"
        binding.tvLabPriorityBadge.setBackgroundResource(
            if (isStat) R.drawable.bg_pill_error_container else R.drawable.bg_pill_secondary_fixed
        )
        binding.tvLabPriorityBadge.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (isStat) R.color.huc_on_error_container else R.color.huc_on_secondary_fixed
            )
        )

        val fecha = orden.fechaSolicitud ?: "Hoy"
        binding.tvLabTubeBadge.text = "${orden.tipoTubo ?: "Tubo EDTA"} • $fecha"
        binding.tvLabExamName.text = orden.examenNombre

        if (showPatientLine && !orden.pacienteNombre.isNullOrBlank()) {
            binding.tvLabPatientInfo.visibility = View.VISIBLE
            binding.tvLabPatientInfo.text =
                "${orden.pacienteNombre} • Exp: ${orden.numeroExpediente ?: "N/D"}"
        } else {
            binding.tvLabPatientInfo.visibility = View.GONE
        }

        if (!orden.observaciones.isNullOrBlank()) {
            binding.tvLabNotes.visibility = View.VISIBLE
            binding.tvLabNotes.text = orden.observaciones
        } else {
            binding.tvLabNotes.visibility = View.GONE
        }

        if (orden.estado == "Realizado" && !orden.fechaResultado.isNullOrBlank()) {
            binding.tvLabResultDate.visibility = View.VISIBLE
            binding.tvLabResultDate.text = "✓ Resultado validado: ${orden.fechaResultado}"
        } else {
            binding.tvLabResultDate.visibility = View.GONE
        }

        binding.tvLabStatusValue.text = orden.estado
        val statusColor = when (orden.estado) {
            "Realizado" -> R.color.huc_semaforo_verde
            "En Proceso" -> R.color.huc_secondary
            "Agendado" -> R.color.huc_primary_container
            "Cancelado" -> R.color.huc_error
            else -> R.color.huc_tertiary_container
        }
        binding.btnLabStatusSelector.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, statusColor))

        binding.btnLabStatusSelector.setOnClickListener {
            val popup = PopupMenu(ctx, binding.btnLabStatusSelector)
            OrdenLaboratorioItem.ESTADOS_VALIDOS.forEachIndexed { idx, est ->
                popup.menu.add(0, idx, idx, est)
            }
            popup.setOnMenuItemClickListener { item ->
                val nuevo = OrdenLaboratorioItem.ESTADOS_VALIDOS[item.itemId]
                if (nuevo != orden.estado) {
                    onStatusChanged(orden.id, nuevo)
                }
                true
            }
            popup.show()
        }
    }
}
