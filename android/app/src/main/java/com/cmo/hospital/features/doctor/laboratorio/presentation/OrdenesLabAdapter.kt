package com.cmo.hospital.features.doctor.laboratorio.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.components.LabOrderCardBinder
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.databinding.CompLabOrderCardBinding

class OrdenesLabAdapter(
    private val showPatientLine: Boolean = true,
    private val onCancelar: (OrdenLaboratorioItem) -> Unit
) : ListAdapter<OrdenLaboratorioItem, OrdenesLabAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<OrdenLaboratorioItem>() {
        override fun areItemsTheSame(
            oldItem: OrdenLaboratorioItem,
            newItem: OrdenLaboratorioItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: OrdenLaboratorioItem,
            newItem: OrdenLaboratorioItem
        ): Boolean = oldItem == newItem
    }

    inner class ViewHolder(
        private val binding: CompLabOrderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OrdenLaboratorioItem) {
            LabOrderCardBinder.bind(
                binding = binding,
                orden = item,
                showPatientLine = showPatientLine,
                onCancelar = onCancelar
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CompLabOrderCardBinding.inflate(
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
