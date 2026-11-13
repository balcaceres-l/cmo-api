package com.cmo.hospital.features.recepcionista.insumos.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.network.dto.RecursoDto
import com.cmo.hospital.databinding.ItemRecursoInsumoBinding

class InsumosAdapter(
    private val onDescargarClick: (RecursoDto) -> Unit
) : ListAdapter<RecursoDto, InsumosAdapter.InsumoViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsumoViewHolder {
        val binding = ItemRecursoInsumoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InsumoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InsumoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InsumoViewHolder(
        private val binding: ItemRecursoInsumoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecursoDto) {
            val ctx = binding.root.context
            binding.tvInsumoNombre.text = item.nombre
            binding.tvInsumoMeta.text = "${item.categoria} • ${item.ubicacion ?: "Botiquín Central"}"
            binding.tvInsumoStockPill.text = "${item.cantidad} ${item.unidad}"
            binding.tvInsumoMinimo.text = "Stock mínimo: ${item.stockMinimo} ${item.unidad}"

            val bajoStock = item.cantidad <= item.stockMinimo
            if (bajoStock) {
                binding.tvInsumoStockPill.setBackgroundResource(R.drawable.bg_pill_error_container)
                binding.tvInsumoStockPill.setTextColor(ContextCompat.getColor(ctx, R.color.triage_critico_text))
            } else {
                binding.tvInsumoStockPill.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                binding.tvInsumoStockPill.setTextColor(ContextCompat.getColor(ctx, R.color.cmo_primary))
            }

            binding.btnDescargarInsumo.isEnabled = item.cantidad > 0
            binding.btnDescargarInsumo.setOnClickListener { onDescargarClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<RecursoDto>() {
        override fun areItemsTheSame(oldItem: RecursoDto, newItem: RecursoDto): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecursoDto, newItem: RecursoDto): Boolean =
            oldItem == newItem
    }
}

