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
import com.cmo.hospital.databinding.ItemCitaRecepcionBinding

class CitasRecepcionAdapter(
    private val onRegistrarLlegada: (Cita) -> Unit,
    private val onAbrirTriaje: (Cita) -> Unit,
    private val onPasarDirectoAConsulta: (Cita) -> Unit,
    private val onCancelarCita: (Cita) -> Unit,
    private val onReprogramar: (Cita) -> Unit,
    private val onConfirmarSolicitud: (Cita) -> Unit
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
            val fechaUi = DateUtils.toUiDate(cita.fecha)
            binding.tvItemCitaHorario.text = when {
                cita.horaInicio.isBlank() -> fechaUi
                cita.horaFin.isNullOrBlank() -> "$fechaUi • ${cita.horaInicio}"
                else -> "$fechaUi • ${cita.horaInicio} - ${cita.horaFin}"
            }
            binding.tvItemCitaPaciente.text = cita.paciente?.nombreCompleto ?: "Paciente"
            binding.tvItemCitaMedicoYExp.text =
                "Exp: ${cita.paciente?.numeroExpediente ?: "N/D"} • ${cita.medicoNombre ?: "Médico asignado"} (${cita.especialidad ?: "Consulta"})"
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
            val estadoNormalizado = cita.estado
            binding.tvItemCitaEstado.text = EstadoCita.etiqueta(estadoNormalizado)
            when (estadoNormalizado) {
                EstadoCita.SOLICITADA -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
                }
                EstadoCita.EN_ATENCION -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))
                }
                EstadoCita.EN_ESPERA -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
                }
                EstadoCita.AGENDADA -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_secondary_fixed)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed))
                }
                EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))
                }
                else -> {
                    binding.tvItemCitaEstado.setBackgroundResource(R.drawable.bg_surface_container_16)
                    binding.tvItemCitaEstado.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface_variant))
                }
            }

            // Regla: la toma de signos vitales y llegada solo están habilitadas el día de la cita
            val esHoy = cita.esDelDia()
            val requiereDiaCita = estadoNormalizado == EstadoCita.AGENDADA || estadoNormalizado == EstadoCita.EN_ESPERA
            if (requiereDiaCita && !esHoy) {
                binding.tvCitaAvisoDia.visibility = View.VISIBLE
                binding.tvCitaAvisoDia.text =
                    "⏱ Toma de signos vitales y llegada habilitadas solo el día de la cita ($fechaUi)."
            } else {
                binding.tvCitaAvisoDia.visibility = View.GONE
            }

            binding.btnCitaRegistrarLlegada.isEnabled = esHoy
            binding.btnCitaRegistrarLlegada.alpha = if (esHoy) 1.0f else 0.45f
            binding.btnCitaTriajeAgendada.isEnabled = esHoy
            binding.btnCitaTriajeAgendada.alpha = if (esHoy) 1.0f else 0.45f
            binding.btnCitaTriajeSignos.isEnabled = esHoy
            binding.btnCitaTriajeSignos.alpha = if (esHoy) 1.0f else 0.45f
            // La consulta la abre el médico desde su app (POST /citas/{id}/consulta).
            binding.btnCitaPasarConsulta.visibility = View.GONE

            // Acciones dinámicas por estado
            binding.actionsCitaSolicitada.visibility =
                if (estadoNormalizado == EstadoCita.SOLICITADA) View.VISIBLE else View.GONE
            binding.btnCitaConfirmarSolicitud.setOnClickListener { onConfirmarSolicitud(cita) }
            binding.btnCitaRechazarSolicitud.setOnClickListener { onCancelarCita(cita) }
            binding.actionsCitaAgendada.visibility =
                if (estadoNormalizado == EstadoCita.AGENDADA) View.VISIBLE else View.GONE
            binding.actionsCitaEnEspera.visibility =
                if (estadoNormalizado == EstadoCita.EN_ESPERA) View.VISIBLE else View.GONE
            binding.bannerCitaEnAtencion.visibility =
                if (estadoNormalizado == EstadoCita.EN_ATENCION) View.VISIBLE else View.GONE

            binding.btnCitaRegistrarLlegada.setOnClickListener {
                if (esHoy) onRegistrarLlegada(cita)
            }
            binding.btnCitaTriajeAgendada.setOnClickListener {
                if (esHoy) onAbrirTriaje(cita)
            }
            binding.btnCitaCancelarAgendada.setOnClickListener { onCancelarCita(cita) }
            binding.btnCitaReprogramar.setOnClickListener { onReprogramar(cita) }
            binding.btnCitaTriajeSignos.setOnClickListener {
                if (esHoy) onAbrirTriaje(cita)
            }
            binding.btnCitaPasarConsulta.setOnClickListener {
                if (esHoy) onPasarDirectoAConsulta(cita)
            }
            binding.btnCitaCancelarEspera.setOnClickListener { onCancelarCita(cita) }
        }
    }
}
