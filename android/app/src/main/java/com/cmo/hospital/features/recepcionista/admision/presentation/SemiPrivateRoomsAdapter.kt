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
            binding.tvSalaCodigo.text = sala.codigoSala
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
                    container.alpha = 0.76f
                    dot.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error))
                    label.text = "Cama $letra (Ocup.)"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.huc_error))
                    check.visibility = View.GONE
                    container.isClickable = false
                    container.setOnClickListener(null)
                }
                isSelected -> {
                    container.setBackgroundResource(R.drawable.bg_bed_semi_selected)
                    container.alpha = 1f
                    dot.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    label.text = "Cama $letra"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    check.visibility = View.VISIBLE
                    container.setOnClickListener { onSelectBed(cama) }
                }
                else -> {
                    container.setBackgroundResource(R.drawable.bg_bed_semi_available)
                    container.alpha = 1f
                    dot.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary))
                    label.text = "Cama $letra (Disp.)"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary))
                    check.visibility = View.GONE
                    container.setOnClickListener { onSelectBed(cama) }
                }
            }
        }
    }
}
