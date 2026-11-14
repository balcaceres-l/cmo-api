package com.cmo.hospital.features.recepcionista.ambulancia.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.core.domain.model.MedicoDeTurno
import com.cmo.hospital.databinding.ItemPersonalGuardiaBinding

/** Médicos con turno en curso (GET /publico/medicos-de-turno). */
class PersonalGuardiaAdapter : ListAdapter<MedicoDeTurno, PersonalGuardiaAdapter.GuardiaViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<MedicoDeTurno>() {
        override fun areItemsTheSame(oldItem: MedicoDeTurno, newItem: MedicoDeTurno) = oldItem.nombre == newItem.nombre
        override fun areContentsTheSame(oldItem: MedicoDeTurno, newItem: MedicoDeTurno) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardiaViewHolder =
        GuardiaViewHolder(ItemPersonalGuardiaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: GuardiaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GuardiaViewHolder(
        private val binding: ItemPersonalGuardiaBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MedicoDeTurno) {
            binding.tvGuardiaNombre.text = item.nombre
            binding.tvGuardiaCargoYArea.text = item.cargo ?: "Médico"
            binding.tvGuardiaHorario.text = "Área: ${item.area ?: "Sin área asignada"}"
            binding.tvGuardiaEstadoBadge.text = "De turno"
        }
    }
}
