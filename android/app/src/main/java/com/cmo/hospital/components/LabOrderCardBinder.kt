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
        onCancelar: (OrdenLaboratorioItem) -> Unit
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
        binding.tvLabTubeBadge.text = orden.tipoTubo?.let { "$it • $fecha" } ?: "Solicitada $fecha"
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

        // Solo el estado: el resultado del examen no pasa por el sistema.
        if (orden.estado == OrdenLaboratorioItem.COMPLETADO && !orden.fechaResultado.isNullOrBlank()) {
            binding.tvLabResultDate.visibility = View.VISIBLE
            binding.tvLabResultDate.text = "✓ Completado: ${orden.fechaResultado}"
        } else {
            binding.tvLabResultDate.visibility = View.GONE
        }

        binding.tvLabStatusValue.text = orden.estado
        val statusColor = when (orden.estado) {
            OrdenLaboratorioItem.COMPLETADO -> R.color.huc_semaforo_verde
            OrdenLaboratorioItem.EN_PROCESO -> R.color.huc_secondary
            OrdenLaboratorioItem.CANCELADA -> R.color.huc_error
            else -> R.color.huc_tertiary_container
        }
        binding.btnLabStatusSelector.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(ctx, statusColor))

        // El laboratorio avanza los estados; el médico solo puede cancelar una orden Pendiente.
        binding.btnLabStatusSelector.isClickable = orden.cancelable
        binding.btnLabStatusSelector.setOnClickListener(
            if (!orden.cancelable) null else View.OnClickListener {
                val popup = PopupMenu(ctx, binding.btnLabStatusSelector)
                popup.menu.add(0, 0, 0, "Cancelar orden")
                popup.setOnMenuItemClickListener {
                    onCancelar(orden)
                    true
                }
                popup.show()
            }
        )
    }
}
