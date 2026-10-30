package com.cmo.hospital.features.recepcionista.laboratorio.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.OrdenLaboratorioItem
import com.cmo.hospital.databinding.ItemOrdenLabRecepcionBinding
import com.cmo.hospital.features.recepcionista.laboratorio.domain.usecase.AvanzarOrdenLaboratorioUseCase

class OrdenesRecepcionAdapter(
    private val onAvanzar: (OrdenLaboratorioItem) -> Unit
) : ListAdapter<OrdenLaboratorioItem, OrdenesRecepcionAdapter.ViewHolder>(DiffCallback) {

    /** Ids con una petición en curso: su botón queda deshabilitado. */
    var procesando: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    object DiffCallback : DiffUtil.ItemCallback<OrdenLaboratorioItem>() {
        override fun areItemsTheSame(oldItem: OrdenLaboratorioItem, newItem: OrdenLaboratorioItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OrdenLaboratorioItem, newItem: OrdenLaboratorioItem) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemOrdenLabRecepcionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemOrdenLabRecepcionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(orden: OrdenLaboratorioItem) {
            val ctx = binding.root.context
            binding.tvOrdRecExamen.text = orden.examenNombre
            binding.tvOrdRecPaciente.text =
                "${orden.pacienteNombre ?: "Paciente"} • Exp: ${orden.numeroExpediente ?: "N/D"}"
            binding.tvOrdRecFecha.text = listOfNotNull(
                orden.fechaSolicitud?.let { "Solicitada $it" },
                orden.fechaResultado?.takeIf { orden.estado == OrdenLaboratorioItem.COMPLETADO }?.let { "Completada $it" }
            ).joinToString(" • ")

            binding.tvOrdRecObservaciones.text = orden.observaciones
            binding.tvOrdRecObservaciones.visibility = if (orden.observaciones.isNullOrBlank()) View.GONE else View.VISIBLE

            val esStat = orden.prioridad.equals("STAT", ignoreCase = true)
            binding.tvOrdRecPrioridad.text = if (esStat) "⚡ STAT" else "RUTINA"
            binding.tvOrdRecPrioridad.setBackgroundResource(
                if (esStat) R.drawable.bg_pill_error_container else R.drawable.bg_pill_secondary_fixed
            )
            binding.tvOrdRecPrioridad.setTextColor(
                ContextCompat.getColor(ctx, if (esStat) R.color.huc_on_error_container else R.color.huc_on_secondary_fixed)
            )

            binding.tvOrdRecEstado.text = orden.estado
            val (fondo, texto) = when (orden.estado) {
                OrdenLaboratorioItem.COMPLETADO -> R.drawable.bg_pill_primary_fixed to R.color.huc_on_primary_fixed
                OrdenLaboratorioItem.EN_PROCESO -> R.drawable.bg_pill_tertiary_fixed to R.color.huc_on_tertiary_fixed
                OrdenLaboratorioItem.CANCELADA -> R.drawable.bg_pill_error_container to R.color.huc_on_error_container
                else -> R.drawable.bg_pill_secondary_fixed to R.color.huc_on_secondary_fixed
            }
            binding.tvOrdRecEstado.setBackgroundResource(fondo)
            binding.tvOrdRecEstado.setTextColor(ContextCompat.getColor(ctx, texto))

            val accion = AvanzarOrdenLaboratorioUseCase.accionPara(orden)
            binding.btnOrdRecAvanzar.visibility = if (accion == null) View.GONE else View.VISIBLE
            binding.btnOrdRecAvanzar.text = accion
            binding.btnOrdRecAvanzar.isEnabled = orden.id !in procesando
            binding.btnOrdRecAvanzar.setIconResource(
                if (orden.estado == OrdenLaboratorioItem.PENDIENTE) R.drawable.ic_play else R.drawable.ic_check_circle
            )
            binding.btnOrdRecAvanzar.setOnClickListener { onAvanzar(orden) }
        }
    }
}
