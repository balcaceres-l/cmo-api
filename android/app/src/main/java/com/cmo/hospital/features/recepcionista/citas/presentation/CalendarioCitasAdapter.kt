package com.cmo.hospital.features.recepcionista.citas.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.EstadoCita
import com.cmo.hospital.core.ui.DateUtils
import com.cmo.hospital.databinding.ItemCitaCalendarioBinding

class CalendarioCitasAdapter(
    private val onAbrirSignosVitales: (Cita) -> Unit
) : ListAdapter<Cita, CalendarioCitasAdapter.CalendarioCitaViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Cita>() {
        override fun areItemsTheSame(oldItem: Cita, newItem: Cita): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Cita, newItem: Cita): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarioCitaViewHolder {
        val binding = ItemCitaCalendarioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarioCitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarioCitaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CalendarioCitaViewHolder(
        private val binding: ItemCitaCalendarioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {
            val ctx = binding.root.context
            val fechaUi = DateUtils.toUiDate(cita.fecha)
            binding.tvCalCitaDia.text = "Día: $fechaUi"

            val horaFin = cita.horaFin?.takeIf { it.isNotBlank() }
                ?: Cita.calcularFin30Minutos(cita.horaInicio)
            binding.tvCalCitaHora.text = if (cita.horaInicio.isNotBlank()) {
                "${cita.horaInicio} - $horaFin"
            } else {
                "Por definir"
            }

            // Estado de la cita
            val estadoNormalizado = cita.estado
            binding.tvCalCitaEstado.text = EstadoCita.etiqueta(estadoNormalizado)
            when (estadoNormalizado) {
                EstadoCita.EN_ATENCION -> {
                    binding.tvCalCitaEstado.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvCalCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))
                }
                EstadoCita.EN_ESPERA -> {
                    binding.tvCalCitaEstado.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvCalCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
                }
                EstadoCita.AGENDADA -> {
                    binding.tvCalCitaEstado.setBackgroundResource(R.drawable.bg_pill_secondary_fixed)
                    binding.tvCalCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed))
                }
                else -> {
                    binding.tvCalCitaEstado.setBackgroundResource(R.drawable.bg_surface_container_16)
                    binding.tvCalCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
                }
            }

            // Información del paciente y la cita
            binding.tvCalCitaPaciente.text = cita.paciente?.nombreCompleto ?: "Paciente registrado"
            val exp = cita.paciente?.numeroExpediente ?: "N/D"
            val dui = cita.paciente?.dui?.takeIf { it.isNotBlank() } ?: "Sin documento"
            binding.tvCalCitaExpediente.text = "Expediente: $exp • Doc: $dui"
            binding.tvCalCitaMotivo.text = "Motivo: ${cita.motivo?.takeIf { it.isNotBlank() } ?: "Consulta externa programada"}"

            // Doctor que atenderá
            val medicoNombre = cita.medicoNombre?.takeIf { it.isNotBlank() } ?: "Médico asignado"
            val especialidad = cita.especialidad?.takeIf { it.isNotBlank() } ?: "Consulta Externa"
            val consultorio = cita.consultorio?.codigo ?: cita.consultorio?.nombre ?: "Consultorio asignado"
            binding.tvCalCitaDoctor.text = "Médico que atenderá: $medicoNombre"
            binding.tvCalCitaEspecialidadYBox.text = "Especialidad: $especialidad • $consultorio"

            // Signos vitales (habilitados únicamente el día de la cita)
            val esHoy = cita.esDelDia()
            val sv = cita.signosVitales
            if (sv != null && sv.hasAnyData) {
                binding.tvCalCitaSignosInfo.text = "✓ Signos vitales registrados (PA ${sv.presionTexto})"
                binding.tvCalCitaSignosInfo.setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_container))
            } else if (esHoy) {
                binding.tvCalCitaSignosInfo.text = "Cita de hoy: toma de signos vitales habilitada"
                binding.tvCalCitaSignosInfo.setTextColor(ContextCompat.getColor(ctx, R.color.huc_secondary))
            } else {
                binding.tvCalCitaSignosInfo.text = "Signos vitales habilitados solo el día de la cita ($fechaUi)"
                binding.tvCalCitaSignosInfo.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
            }

            val permiteSignos = estadoNormalizado == EstadoCita.AGENDADA || estadoNormalizado == EstadoCita.EN_ESPERA
            binding.btnCalCitaSignos.visibility = if (permiteSignos) View.VISIBLE else View.GONE
            binding.btnCalCitaSignos.isEnabled = esHoy
            binding.btnCalCitaSignos.alpha = if (esHoy) 1.0f else 0.45f
            binding.btnCalCitaSignos.setOnClickListener {
                if (esHoy) onAbrirSignosVitales(cita)
            }
        }
    }
}
