package com.cmo.hospital.features.doctor.consulta.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.RecetaItem
import com.cmo.hospital.databinding.ItemRecetaConsultaBinding

class RecetasConsultaAdapter(
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<RecetaItem, RecetasConsultaAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RecetaItem>() {
        override fun areItemsTheSame(oldItem: RecetaItem, newItem: RecetaItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecetaItem, newItem: RecetaItem): Boolean =
            oldItem == newItem
    }

    inner class ViewHolder(
        private val binding: ItemRecetaConsultaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecetaItem) {
            val ctx = binding.root.context
            binding.tvRecetaMedicamento.text = item.medicamento
            binding.tvRecetaTipoBadge.text = item.tipo
            when (item.tipo) {
                "SOS" -> {
                    binding.tvRecetaTipoBadge.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvRecetaTipoBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed)
                    )
                }
                "Agudo" -> {
                    binding.tvRecetaTipoBadge.setBackgroundResource(R.drawable.bg_pill_secondary_fixed)
                    binding.tvRecetaTipoBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed)
                    )
                }
                else -> {
                    binding.tvRecetaTipoBadge.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvRecetaTipoBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed)
                    )
                }
            }
            binding.tvRecetaPosologia.text =
                "${item.dosis} • ${item.frecuencia} • ${item.duracion}"
            if (!item.indicaciones.isNullOrBlank()) {
                binding.tvRecetaIndicaciones.visibility = View.VISIBLE
                binding.tvRecetaIndicaciones.text = item.indicaciones
            } else {
                binding.tvRecetaIndicaciones.visibility = View.GONE
            }
            binding.btnEliminarReceta.setOnClickListener {
                onDeleteClick(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecetaConsultaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
