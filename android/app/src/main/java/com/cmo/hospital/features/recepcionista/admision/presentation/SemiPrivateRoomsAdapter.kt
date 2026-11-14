package com.cmo.hospital.features.recepcionista.admision.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.SalaSemiprivadaUi
import com.cmo.hospital.databinding.ItemSalaSemiprivadaBinding

class SemiPrivateRoomsAdapter(
    private val onSelectBed: (Cama) -> Unit
) : RecyclerView.Adapter<SemiPrivateRoomsAdapter.SalaViewHolder>() {

    private var salas: List<SalaSemiprivadaUi> = emptyList()
    private var selectedCode: String? = null

    fun submit(items: List<SalaSemiprivadaUi>, selectedBedCode: String?) {
        salas = items
        selectedCode = selectedBedCode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaViewHolder {
        val binding = ItemSalaSemiprivadaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SalaViewHolder(binding)
    }

    override fun getItemCount(): Int = salas.size

    override fun onBindViewHolder(holder: SalaViewHolder, position: Int) {
        holder.bind(salas[position])
    }

    inner class SalaViewHolder(
        private val binding: ItemSalaSemiprivadaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sala: SalaSemiprivadaUi) {
            val ctx = binding.root.context
            binding.tvSalaCodigo.text = "Habitación ${sala.codigoSala} • 2ª Planta"

            val dispCount = (if (sala.camaA.estaDisponible) 1 else 0) + (if (sala.camaB.estaDisponible) 1 else 0)
            when (dispCount) {
                2 -> {
                    binding.tvSalaResumenEstado.text = "Disponible (2/2 libres)"
                    binding.tvSalaResumenEstado.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvSalaResumenEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))
                }
                1 -> {
                    binding.tvSalaResumenEstado.text = "Parcial (1 libre • 1 ocupada)"
                    binding.tvSalaResumenEstado.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvSalaResumenEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
                }
                else -> {
                    binding.tvSalaResumenEstado.text = "Ocupada (2/2 ocupadas)"
                    binding.tvSalaResumenEstado.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvSalaResumenEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))
                }
            }

            bindSingleBedPill(
                cama = sala.camaA,
                letra = "A",
                container = binding.btnCamaA,
                dot = binding.dotCamaA,
                label = binding.tvCamaALabel,
                check = binding.ivCamaACheck
            )
            bindSingleBedPill(
                cama = sala.camaB,
                letra = "B",
                container = binding.btnCamaB,
                dot = binding.dotCamaB,
                label = binding.tvCamaBLabel,
                check = binding.ivCamaBCheck
            )
        }

        private fun bindSingleBedPill(
            cama: Cama,
            letra: String,
            container: LinearLayout,
            dot: View,
            label: TextView,
            check: ImageView
        ) {
            val ctx = container.context
            val isSelected = cama.codigo == selectedCode && cama.estaDisponible
            when {
                !cama.estaDisponible -> {
                    container.setBackgroundResource(R.drawable.bg_bed_semi_occupied)
                    container.alpha = 0.85f
                    dot.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error))
                    val nombre = cama.paciente?.nombreCompleto
                    label.text = if (!nombre.isNullOrBlank()) {
                        "Cama $letra • Ocupada\n$nombre"
                    } else {
                        "Cama $letra • Ocupada"
                    }
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.huc_error))
                    check.visibility = View.GONE
                    container.setOnClickListener { onSelectBed(cama) }
                }
                isSelected -> {
                    container.setBackgroundResource(R.drawable.bg_bed_semi_selected)
                    container.alpha = 1f
                    dot.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    label.text = "Cama $letra • Seleccionada"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    check.visibility = View.VISIBLE
                    container.setOnClickListener { onSelectBed(cama) }
                }
                else -> {
                    container.setBackgroundResource(R.drawable.bg_bed_semi_available)
                    container.alpha = 1f
                    dot.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary))
                    label.text = "Cama $letra • Disponible"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary))
                    check.visibility = View.GONE
                    container.setOnClickListener { onSelectBed(cama) }
                }
            }
        }
    }
}
