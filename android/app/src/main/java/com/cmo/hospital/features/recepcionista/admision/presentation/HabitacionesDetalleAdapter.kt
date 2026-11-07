package com.cmo.hospital.features.recepcionista.admision.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.Cama
import com.cmo.hospital.core.domain.model.HabitacionDetalleUi
import com.cmo.hospital.core.domain.model.Paciente
import com.cmo.hospital.databinding.ItemHabitacionDetalleBinding

class HabitacionesDetalleAdapter(
    private val onAsignarCama: (Cama) -> Unit
) : RecyclerView.Adapter<HabitacionesDetalleAdapter.HabitacionViewHolder>() {

    private var items: List<HabitacionDetalleUi> = emptyList()

    fun submitList(nuevos: List<HabitacionDetalleUi>) {
        items = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitacionViewHolder {
        val binding = ItemHabitacionDetalleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitacionViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HabitacionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class HabitacionViewHolder(
        private val binding: ItemHabitacionDetalleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hab: HabitacionDetalleUi) {
            val ctx = binding.root.context

            binding.tvHabitacionCodigo.text = hab.codigo
            binding.tvHabitacionTipoYPlanta.text = "${hab.tipo} (${hab.totalCamas} Camas) • ${hab.planta}"

            val esVip = hab.tipo.contains("VIP", ignoreCase = true)
            binding.ivHabitacionIcon.setImageResource(if (esVip) R.drawable.ic_star else R.drawable.ic_meeting_room)

            when {
                hab.estaCompletamenteDisponible -> {
                    binding.tvHabitacionEstadoBadge.text = "100% Disponible"
                    binding.tvHabitacionEstadoBadge.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvHabitacionEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))
                }
                hab.estaParcialmenteOcupada -> {
                    binding.tvHabitacionEstadoBadge.text = "${hab.camasDisponibles} de ${hab.totalCamas} Disp."
                    binding.tvHabitacionEstadoBadge.setBackgroundResource(R.drawable.bg_pill_tertiary_fixed)
                    binding.tvHabitacionEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_tertiary_fixed))
                }
                else -> {
                    binding.tvHabitacionEstadoBadge.text = "Ocupada (${hab.totalCamas}/${hab.totalCamas})"
                    binding.tvHabitacionEstadoBadge.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvHabitacionEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))
                }
            }

            // 1. Cama A (o Cama única)
            val camaA = hab.camas.firstOrNull()
            if (camaA != null) {
                binding.cardCamaA.visibility = View.VISIBLE
                binding.tvCamaACodigoBadge.text = "Cama ${camaA.codigo}"

                if (camaA.estaDisponible) {
                    binding.tvCamaAEstadoBadge.text = "DISPONIBLE"
                    binding.tvCamaAEstadoBadge.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvCamaAEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))

                    binding.containerCamaADisponible.visibility = View.VISIBLE
                    binding.containerCamaAOcupada.visibility = View.GONE
                    binding.btnAsignarCamaA.setOnClickListener { onAsignarCama(camaA) }
                } else {
                    binding.tvCamaAEstadoBadge.text = "OCUPADA"
                    binding.tvCamaAEstadoBadge.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvCamaAEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))

                    binding.containerCamaADisponible.visibility = View.GONE
                    binding.containerCamaAOcupada.visibility = View.VISIBLE

                    val p = camaA.paciente
                    val nombre = if (p?.protegido == true) {
                        Paciente.anonimizarNombre(p.nombreCompleto)
                    } else {
                        p?.nombreCompleto ?: "Paciente Internado"
                    }
                    binding.tvPacienteANombre.text = nombre
                    binding.tvPacienteAExpediente.text = "Exp: ${p?.numeroExpediente ?: "S/E"}"
                    binding.tvPacienteADocumento.text = "DUI: ${p?.dui ?: "04829101-1"}"
                    binding.tvPacienteASexoEdad.text = "${p?.sexo ?: "M"} • ${p?.edad ?: 35} años"
                    binding.tvPacienteATriage.text = "Triage: Estable"
                    binding.tvPacienteADiagnostico.text = "Motivo: Hospitalización clínica en ${camaA.codigo}."
                }
            } else {
                binding.cardCamaA.visibility = View.GONE
            }

            // 2. Cama B (si la habitación es semiprivada)
            if (hab.camas.size > 1) {
                val camaB = hab.camas[1]
                binding.cardCamaB.visibility = View.VISIBLE
                binding.tvCamaBCodigoBadge.text = "Cama ${camaB.codigo}"

                if (camaB.estaDisponible) {
                    binding.tvCamaBEstadoBadge.text = "DISPONIBLE"
                    binding.tvCamaBEstadoBadge.setBackgroundResource(R.drawable.bg_pill_primary_fixed)
                    binding.tvCamaBEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_primary_fixed))

                    binding.containerCamaBDisponible.visibility = View.VISIBLE
                    binding.containerCamaBOcupada.visibility = View.GONE
                    binding.btnAsignarCamaB.setOnClickListener { onAsignarCama(camaB) }
                } else {
                    binding.tvCamaBEstadoBadge.text = "OCUPADA"
                    binding.tvCamaBEstadoBadge.setBackgroundResource(R.drawable.bg_pill_error_container)
                    binding.tvCamaBEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_error_container))

                    binding.containerCamaBDisponible.visibility = View.GONE
                    binding.containerCamaBOcupada.visibility = View.VISIBLE

                    val p = camaB.paciente
                    val nombre = if (p?.protegido == true) {
                        Paciente.anonimizarNombre(p.nombreCompleto)
                    } else {
                        p?.nombreCompleto ?: "Paciente Internado"
                    }
                    binding.tvPacienteBNombre.text = nombre
                    binding.tvPacienteBExpediente.text = "Exp: ${p?.numeroExpediente ?: "S/E"}"
                    binding.tvPacienteBDocumento.text = "DUI: ${p?.dui ?: "02391020-4"}"
                    binding.tvPacienteBSexoEdad.text = "${p?.sexo ?: "F"} • ${p?.edad ?: 28} años"
                    binding.tvPacienteBTriage.text = "Triage: Observación"
                    binding.tvPacienteBDiagnostico.text = "Motivo: Hospitalización clínica en ${camaB.codigo}."
                }
            } else {
                binding.cardCamaB.visibility = View.GONE
            }
        }
    }
}
