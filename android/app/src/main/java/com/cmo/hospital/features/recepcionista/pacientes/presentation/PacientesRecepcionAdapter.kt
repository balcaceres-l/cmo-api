package com.cmo.hospital.features.recepcionista.pacientes.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.ItemPacienteRecepcionBinding

class PacientesRecepcionAdapter(
    private val onEditar: (Paciente) -> Unit,
    private val onCodigoActivacion: (Paciente) -> Unit
) : ListAdapter<Paciente, PacientesRecepcionAdapter.PacienteViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Paciente>() {
        override fun areItemsTheSame(oldItem: Paciente, newItem: Paciente): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Paciente, newItem: Paciente): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val binding = ItemPacienteRecepcionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PacienteViewHolder(
        private val binding: ItemPacienteRecepcionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {
            binding.tvPacRecIniciales.text = iniciales(paciente.nombreCompleto)
            binding.tvPacRecNombre.text = paciente.nombreCompleto
            binding.tvPacRecExpediente.text = paciente.numeroExpediente ?: "Sin expediente"

            val sexo = when (paciente.sexo) {
                "F" -> "Femenino"
                "M" -> "Masculino"
                else -> null
            }
            binding.tvPacRecFiliacion.text = listOfNotNull(
                paciente.fechaNacimiento?.let { "${paciente.edad} años" },
                sexo,
                paciente.dui?.let { "DUI $it" }
            ).joinToString(" • ").ifBlank { "Sin datos de filiación" }

            val contacto = listOfNotNull(paciente.telefono, paciente.direccion).joinToString(" • ")
            binding.tvPacRecContacto.text = contacto
            binding.tvPacRecContacto.visibility = if (contacto.isBlank()) View.GONE else View.VISIBLE

            // Con cuenta ya vinculada la API rechaza un código nuevo (409).
            val tieneCuenta = paciente.idUsuario != null
            binding.btnPacRecCodigo.isEnabled = !tieneCuenta
            binding.btnPacRecCodigo.text = if (tieneCuenta) "App vinculada" else "Código App"

            binding.btnPacRecEditar.setOnClickListener { onEditar(paciente) }
            binding.btnPacRecCodigo.setOnClickListener { onCodigoActivacion(paciente) }
        }

        private fun iniciales(nombre: String): String =
            nombre.split(" ").filter { it.isNotBlank() }.take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "?" }
    }
}
