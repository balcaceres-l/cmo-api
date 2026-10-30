package com.cmo.hospital.features.doctor.inicio.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.ItemPacienteMedicoBinding
import com.cmo.hospital.features.doctor.inicio.domain.usecase.PacienteMedicoResumen

class PacientesMedicoAdapter(
    private val onAbrirExpedienteClick: (Paciente) -> Unit,
    private val onAgendarControlClick: (Paciente) -> Unit
) : ListAdapter<PacienteMedicoResumen, PacientesMedicoAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<PacienteMedicoResumen>() {
        override fun areItemsTheSame(
            oldItem: PacienteMedicoResumen,
            newItem: PacienteMedicoResumen
        ): Boolean = oldItem.paciente.numeroExpediente == newItem.paciente.numeroExpediente

        override fun areContentsTheSame(
            oldItem: PacienteMedicoResumen,
            newItem: PacienteMedicoResumen
        ): Boolean = oldItem == newItem
    }

    inner class ViewHolder(
        private val binding: ItemPacienteMedicoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PacienteMedicoResumen) {
            val pac = item.paciente
            binding.tvPacDocIniciales.text = Paciente.extraerIniciales(pac.nombreCompleto)
            binding.tvPacDocNombre.text = pac.nombreCompleto
            binding.tvPacDocExpBadge.text = pac.numeroExpediente ?: "S/E"

            val edadTxt = if (pac.edad > 0) "${pac.edad} años" else "Edad N/D"
            val dniTxt = pac.dui ?: "S/D"
            val sangreTxt = pac.tipoSangre ?: "O+"
            binding.tvPacDocDatosFiliacion.text =
                "$edadTxt • DNI: $dniTxt • Sangre: $sangreTxt"

            val estadoExtra = item.estadoUltimaCita?.let { " • Cita: $it" }.orEmpty()
            val motivoExtra = item.ultimoMotivo?.takeIf { it.isNotBlank() }?.let { "\nÚltimo motivo: $it" }.orEmpty()
            binding.tvPacDocUbicacionMotivo.text =
                "Ubicación: ${item.ubicacionActual}$estadoExtra$motivoExtra"

            if (!item.alertaAlergia.isNullOrBlank()) {
                binding.tvPacDocAlertaAlergia.visibility = View.VISIBLE
                binding.tvPacDocAlertaAlergia.text = "⚠ Alerta Alergia: ${item.alertaAlergia}"
            } else {
                binding.tvPacDocAlertaAlergia.visibility = View.GONE
            }

            binding.btnPacDocAbrirExpediente.setOnClickListener {
                onAbrirExpedienteClick(pac)
            }
            binding.btnPacDocAgendarControl.setOnClickListener {
                onAgendarControlClick(pac)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPacienteMedicoBinding.inflate(
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
