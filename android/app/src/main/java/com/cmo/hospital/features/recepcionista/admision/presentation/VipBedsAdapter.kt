package com.cmo.hospital.features.recepcionista.admision.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.databinding.ItemCamaVipBinding

class VipBedsAdapter(
    private val onSelectBed: (Cama) -> Unit
) : RecyclerView.Adapter<VipBedsAdapter.VipBedViewHolder>() {

    private var items: List<Cama> = emptyList()
    private var selectedCode: String? = null

    fun submit(camas: List<Cama>, selectedBedCode: String?) {
        items = camas
        selectedCode = selectedBedCode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VipBedViewHolder {
        val binding = ItemCamaVipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VipBedViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VipBedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class VipBedViewHolder(
        private val binding: ItemCamaVipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cama: Cama) {
            val ctx = binding.root.context
            val isSelected = cama.codigo == selectedCode && cama.estaDisponible
            binding.tvVipBedCode.text = "Hab. ${cama.codigo}"
            binding.tvVipBedFloor.text = "3ª Planta • VIP Individual"

            when {
                !cama.estaDisponible -> {
                    binding.vipBedCardRoot.setBackgroundResource(R.drawable.bg_bed_vip_occupied)
                    binding.vipBedCardRoot.alpha = 0.85f
                    binding.tvVipBedCode.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                    binding.tvVipBedFloor.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
                    binding.dotVipBedStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error))
                    val nombre = cama.paciente?.nombreCompleto
                    binding.tvVipBedState.text = if (!nombre.isNullOrBlank()) {
                        "Ocupada • $nombre"
                    } else {
                        "Ocupada"
                    }
                    binding.tvVipBedState.setTextColor(ContextCompat.getColor(ctx, R.color.huc_error))
                    binding.ivVipBedCheck.visibility = View.GONE
                    binding.vipBedCardRoot.setOnClickListener { onSelectBed(cama) }
                }
                isSelected -> {
                    binding.vipBedCardRoot.setBackgroundResource(R.drawable.bg_bed_vip_selected)
                    binding.vipBedCardRoot.alpha = 1f
                    binding.tvVipBedCode.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary))
                    binding.tvVipBedFloor.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    binding.dotVipBedStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    binding.tvVipBedState.text = "Seleccionada"
                    binding.tvVipBedState.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    binding.ivVipBedCheck.visibility = View.VISIBLE
                    binding.vipBedCardRoot.setOnClickListener { onSelectBed(cama) }
                }
                else -> {
                    binding.vipBedCardRoot.setBackgroundResource(R.drawable.bg_bed_vip_available)
                    binding.vipBedCardRoot.alpha = 1f
                    binding.tvVipBedCode.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                    binding.tvVipBedFloor.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
                    binding.dotVipBedStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary))
                    binding.tvVipBedState.text = "Disponible"
                    binding.tvVipBedState.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary))
                    binding.ivVipBedCheck.visibility = View.GONE
                    binding.vipBedCardRoot.setOnClickListener { onSelectBed(cama) }
                }
            }
        }
    }
}
