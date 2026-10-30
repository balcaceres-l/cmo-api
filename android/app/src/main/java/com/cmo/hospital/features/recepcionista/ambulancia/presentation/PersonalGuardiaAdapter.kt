package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.core.domain.model.PersonalGuardia
import com.cmo.hospital.databinding.ItemPersonalGuardiaBinding

class PersonalGuardiaAdapter :
    ListAdapter<PersonalGuardia, PersonalGuardiaAdapter.GuardiaViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<PersonalGuardia>() {
        override fun areItemsTheSame(oldItem: PersonalGuardia, newItem: PersonalGuardia): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PersonalGuardia, newItem: PersonalGuardia): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardiaViewHolder {
        val binding = ItemPersonalGuardiaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GuardiaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuardiaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GuardiaViewHolder(
        private val binding: ItemPersonalGuardiaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PersonalGuardia) {
            binding.tvGuardiaNombre.text = item.nombreCompleto
            binding.tvGuardiaCargoYArea.text = "${item.cargo} • ${item.area}"
            val relevo = item.relevoEn?.let { " • Relevo: $it" }.orEmpty()
            binding.tvGuardiaHorario.text =
                "Turno: ${item.horaInicio} - ${item.horaFin}$relevo"
            binding.tvGuardiaEstadoBadge.text = item.estado
        }
    }
}
