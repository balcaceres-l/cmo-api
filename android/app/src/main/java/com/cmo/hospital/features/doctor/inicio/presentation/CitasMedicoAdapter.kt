package com.cmo.hospital.features.doctor.inicio.presentation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Cita
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.ItemCitaMedicoBinding

class CitasMedicoAdapter(
    private val onIniciarORetomarClick: (Cita) -> Unit,
    private val onVerExpedienteClick: (Cita) -> Unit,
    private val onAgendarControlClick: (Cita) -> Unit
) : ListAdapter<Cita, CitasMedicoAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Cita>() {
        override fun areItemsTheSame(oldItem: Cita, newItem: Cita): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Cita, newItem: Cita): Boolean =
            oldItem == newItem
    }

    inner class ViewHolder(
        private val binding: ItemCitaMedicoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {
            val ctx = binding.root.context
            val fin = cita.horaFin ?: Cita.calcularFin30Minutos(cita.horaInicio)
            binding.tvCitaDocHorario.text =
                if (cita.horaInicio == "Ahora") "URGENCIA • Ahora" else "${cita.horaInicio} - $fin"
            binding.tvCitaDocFechaBox.text =
                "${cita.fecha} • ${cita.consultorio?.codigo ?: "Box 03"}"

            val pac = cita.paciente
            val nombre = pac?.nombreCompleto ?: "Paciente HUC"
            val exp = pac?.numeroExpediente ?: Paciente.generarNumeroExpediente(nombre)
            val dni = pac?.dui ?: "S/D"
            binding.tvCitaDocPacienteNombre.text = nombre
            binding.tvCitaDocLeyBadge.visibility =
                if (pac?.protegido == true) View.VISIBLE else View.GONE
            binding.tvCitaDocExpedienteMeta.text = "EXP: $exp • DNI: $dni"
            binding.tvCitaDocMotivo.text =
                "Motivo: ${cita.motivo ?: "Evaluación médica programada"}"

            val sv = cita.signosVitales
            if (sv != null && sv.hasAnyData) {
                val fc = sv.frecuenciaCardiaca?.let { "FC $it lpm" } ?: "FC N/D"
                val spo2 = sv.saturacionOxigeno?.let { "SpO2 $it%" } ?: "SpO2 N/D"
                binding.tvCitaDocTriajeResumen.text =
                    "✓ Triaje registrado: PA ${sv.presionTexto} • $fc • $spo2"
                binding.tvCitaDocTriajeResumen.setTextColor(
                    ContextCompat.getColor(ctx, R.color.huc_primary_container)
                )
            } else {
                binding.tvCitaDocTriajeResumen.text =
                    "• Sin triaje previo registrado (puede tomar signos en Box)"
                binding.tvCitaDocTriajeResumen.setTextColor(
                    ContextCompat.getColor(ctx, R.color.huc_outline)
                )
            }

            when (cita.estado) {
                "EN_ATENCION" -> {
                    binding.tvCitaDocEstadoPill.text = "● EN CONSULTA"
                    binding.tvCitaDocEstadoPill.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvCitaDocEstadoPill.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed)
                    )

                    binding.btnCitaDocAccionPrincipal.isEnabled = true
                    binding.btnCitaDocAccionPrincipal.text = "Continuar Consulta"
                    binding.btnCitaDocAccionPrincipal.setIconResource(R.drawable.ic_medical_services)
                    binding.btnCitaDocAccionPrincipal.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_primary_container)
                    )
                    binding.btnCitaDocAccionPrincipal.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_primary)
                    )
                    binding.btnCitaDocAccionPrincipal.setOnClickListener {
                        onIniciarORetomarClick(cita)
                    }
                }
                "EN_ESPERA" -> {
                    binding.tvCitaDocEstadoPill.text = "EN SALA DE ESPERA"
                    binding.tvCitaDocEstadoPill.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvCitaDocEstadoPill.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed)
                    )

                    binding.btnCitaDocAccionPrincipal.isEnabled = true
                    binding.btnCitaDocAccionPrincipal.text = "Iniciar Consulta"
                    binding.btnCitaDocAccionPrincipal.setIconResource(R.drawable.ic_play)
                    binding.btnCitaDocAccionPrincipal.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_primary_container)
                    )
                    binding.btnCitaDocAccionPrincipal.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_primary)
                    )
                    binding.btnCitaDocAccionPrincipal.setOnClickListener {
                        onIniciarORetomarClick(cita)
                    }
                }
                "ATENDIDA" -> {
                    binding.tvCitaDocEstadoPill.text = "✓ ATENDIDA"
                    binding.tvCitaDocEstadoPill.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvCitaDocEstadoPill.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_semaforo_verde)
                    )

                    binding.btnCitaDocAccionPrincipal.isEnabled = true
                    binding.btnCitaDocAccionPrincipal.text = "Agendar Control"
                    binding.btnCitaDocAccionPrincipal.setIconResource(R.drawable.ic_event)
                    binding.btnCitaDocAccionPrincipal.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_secondary_fixed)
                    )
                    binding.btnCitaDocAccionPrincipal.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed)
                    )
                    binding.btnCitaDocAccionPrincipal.setOnClickListener {
                        onAgendarControlClick(cita)
                    }
                }
                "CANCELADA" -> {
                    binding.tvCitaDocEstadoPill.text = "CANCELADA"
                    binding.tvCitaDocEstadoPill.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvCitaDocEstadoPill.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_error_container)
                    )

                    binding.btnCitaDocAccionPrincipal.isEnabled = false
                    binding.btnCitaDocAccionPrincipal.text = "Cancelada"
                    binding.btnCitaDocAccionPrincipal.setOnClickListener(null)
                }
                else -> {
                    binding.tvCitaDocEstadoPill.text = "AGENDADA"
                    binding.tvCitaDocEstadoPill.setBackgroundResource(R.drawable.bg_pill_secondary_fixed)
                    binding.tvCitaDocEstadoPill.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_secondary_fixed)
                    )

                    binding.btnCitaDocAccionPrincipal.isEnabled = true
                    binding.btnCitaDocAccionPrincipal.text = "Iniciar Consulta"
                    binding.btnCitaDocAccionPrincipal.setIconResource(R.drawable.ic_play)
                    binding.btnCitaDocAccionPrincipal.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.huc_primary_container)
                    )
                    binding.btnCitaDocAccionPrincipal.setTextColor(
                        ContextCompat.getColor(ctx, R.color.huc_on_primary)
                    )
                    binding.btnCitaDocAccionPrincipal.setOnClickListener {
                        onIniciarORetomarClick(cita)
                    }
                }
            }

            binding.btnCitaDocVerExpediente.setOnClickListener {
                onVerExpedienteClick(cita)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCitaMedicoBinding.inflate(
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
