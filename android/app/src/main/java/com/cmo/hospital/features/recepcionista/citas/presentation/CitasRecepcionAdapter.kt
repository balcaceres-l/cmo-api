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
import com.cmo.hospital.databinding.ItemCitaRecepcionBinding

class CitasRecepcionAdapter(
    private val onRegistrarLlegada: (Cita) -> Unit,
    private val onAbrirTriaje: (Cita) -> Unit,
    private val onPasarDirectoAConsulta: (Cita) -> Unit,
    private val onCancelarCita: (Cita) -> Unit
) : ListAdapter<Cita, CitasRecepcionAdapter.CitaViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Cita>() {
        override fun areItemsTheSame(oldItem: Cita, newItem: Cita): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Cita, newItem: Cita): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val binding = ItemCitaRecepcionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CitaViewHolder(
        private val binding: ItemCitaRecepcionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {
            val ctx = binding.root.context
            val horario = if (!cita.horaFin.isNullOrBlank()) {
                "${cita.horaInicio} - ${cita.horaFin}"
            } else {
                cita.horaInicio
            }
            binding.tvItemCitaHorario.text = horario
            binding.tvItemCitaBox.text = cita.consultorio?.codigo ?: "Box 03"
            binding.tvItemCitaPaciente.text = cita.paciente?.nombreCompleto ?: "Paciente HUC"
            binding.tvItemCitaMedicoYExp.text =
                "Exp: ${cita.paciente?.numeroExpediente ?: "N/D"} • ${cita.medicoNombre ?: "Médico"} (${cita.especialidad ?: "Consulta"})"
            binding.tvItemCitaMotivo.text = cita.motivo ?: "Consulta programada"

            // Resumen de signos vitales si existen
            val sv = cita.signosVitales
            if (sv != null && sv.hasAnyData) {
                binding.tvItemCitaSignosResumen.visibility = View.VISIBLE
                val partes = buildList {
                    if (sv.presionTexto != "N/D") add("PA ${sv.presionTexto}")
                    sv.frecuenciaCardiaca?.let { add("FC $it lpm") }
                    sv.temperaturaC?.let { add("T° $it°C") }
                    sv.saturacionOxigeno?.let { add("SpO2 $it%") }
                    sv.imc?.let { add("IMC $it (${sv.imcClasificacion})") }
                }
                binding.tvItemCitaSignosResumen.text =
                    "✓ Triaje: " + (partes.takeIf { it.isNotEmpty() }?.joinToString(" • ") ?: (sv.observaciones ?: "Registrado"))
            } else {
                binding.tvItemCitaSignosResumen.visibility = View.GONE
            }

            // Badge de estado
            binding.tvItemCitaEstado.text = cita.estado
            when (cita.estado) {
                "EN_ATENCION" -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))
                }
                "EN_ESPERA" -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
                }
                "AGENDADA" -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_secondary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed))
                }
                "CANCELADA" -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))
                }
                else -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_surface_container_16)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
                }
            }

            // Acciones dinámicas por estado
            binding.actionsCitaAgendada.visibility =
                if (cita.estado == "AGENDADA") View.VISIBLE else View.GONE
            binding.actionsCitaEnEspera.visibility =
                if (cita.estado == "EN_ESPERA") View.VISIBLE else View.GONE
            binding.bannerCitaEnAtencion.visibility =
                if (cita.estado == "EN_ATENCION") View.VISIBLE else View.GONE

            binding.btnCitaRegistrarLlegada.setOnClickListener { onRegistrarLlegada(cita) }
            binding.btnCitaCancelarAgendada.setOnClickListener { onCancelarCita(cita) }
            binding.btnCitaTriajeSignos.setOnClickListener { onAbrirTriaje(cita) }
            binding.btnCitaPasarConsulta.setOnClickListener { onPasarDirectoAConsulta(cita) }
            binding.btnCitaCancelarEspera.setOnClickListener { onCancelarCita(cita) }
        }
    }
}
