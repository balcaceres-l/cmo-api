package com.cmo.hospital.features.doctor.expedientes.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.core.domain.model.AtencionHistorica
import com.cmo.hospital.databinding.ItemAtencionHistoricaBinding

class AtencionesHistoricasAdapter :
    ListAdapter<AtencionHistorica, AtencionesHistoricasAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<AtencionHistorica>() {
        override fun areItemsTheSame(
            oldItem: AtencionHistorica,
            newItem: AtencionHistorica
        ): Boolean = oldItem.fecha == newItem.fecha && oldItem.hora == newItem.hora &&
            oldItem.diagnostico == newItem.diagnostico

        override fun areContentsTheSame(
            oldItem: AtencionHistorica,
            newItem: AtencionHistorica
        ): Boolean = oldItem == newItem
    }

    class ViewHolder(
        private val binding: ItemAtencionHistoricaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AtencionHistorica) {
            binding.tvHistEspecialidad.text = item.especialidad
            binding.tvHistFechaHora.text = "${item.fecha} • ${item.hora}"
            binding.tvHistDiagnostico.text = item.diagnostico
            binding.tvHistNotas.text = item.notas
            binding.tvHistMedico.text = "Firmado por: ${item.medico}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAtencionHistoricaBinding.inflate(
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
