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
            binding.tvVipBedCode.text = cama.codigo

            when {
                !cama.estaDisponible -> {
                    binding.vipBedCardRoot.setBackgroundResource(R.drawable.bg_bed_vip_occupied)
                    binding.vipBedCardRoot.alpha = 0.72f
                    binding.tvVipBedCode.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                    binding.dotVipBedStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_error))
                    binding.tvVipBedState.text = "Ocupada"
                    binding.tvVipBedState.setTextColor(ContextCompat.getColor(ctx, R.color.huc_error))
                    binding.ivVipBedCheck.visibility = View.GONE
                    binding.vipBedCardRoot.isClickable = false
                    binding.vipBedCardRoot.setOnClickListener(null)
                }
                isSelected -> {
                    binding.vipBedCardRoot.setBackgroundResource(R.drawable.bg_bed_vip_selected)
                    binding.vipBedCardRoot.alpha = 1f
                    binding.tvVipBedCode.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary))
                    binding.dotVipBedStatus.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    binding.tvVipBedState.text = "Asignada"
                    binding.tvVipBedState.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    binding.ivVipBedCheck.visibility = View.VISIBLE
                    binding.vipBedCardRoot.setOnClickListener { onSelectBed(cama) }
                }
                else -> {
                    binding.vipBedCardRoot.setBackgroundResource(R.drawable.bg_bed_vip_available)
                    binding.vipBedCardRoot.alpha = 1f
                    binding.tvVipBedCode.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
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
