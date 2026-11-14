package com.cmo.hospital.features.auth.presentation

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cmo.hospital.R
import com.cmo.hospital.core.network.dto.DemoPersonalItem
import com.cmo.hospital.databinding.DialogDemoPersonalPickerBinding
import com.cmo.hospital.databinding.ItemDemoPersonalBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip

class DemoPersonalPickerSheet(
    context: Context,
    private val categoria: String, // "MEDICO" o "APOYO"
    private val onSelect: (DemoPersonalItem) -> Unit
) {
    private val dialog = BottomSheetDialog(context)
    private val binding = DialogDemoPersonalPickerBinding.inflate(LayoutInflater.from(context))

    private enum class FiltroEstado { TODOS, EN_TURNO, FUERA_TURNO }

    private var allItems: List<DemoPersonalItem> = emptyList()
    private var filtroEstado: FiltroEstado = FiltroEstado.TODOS
    private var filtroSubgrupo: String? = null // null = Todos
    private var subgruposBuilt: List<String> = emptyList()

    private val adapter = DemoPersonalAdapter { item ->
        dialog.dismiss()
        onSelect(item)
    }

    init {
        dialog.setContentView(binding.root)
        binding.tvPickerTitle.text = if (categoria == "MEDICO") {
            "Seleccionar Médico"
        } else {
            "Seleccionar Personal de Apoyo"
        }
        binding.tvPickerSubtitle.text = if (categoria == "MEDICO") {
            "Filtra por especialidad o elige En turno / Fuera de turno para pruebas"
        } else {
            "Enfermería y Recepción — elige En turno o Fuera de turno para pruebas"
        }

        binding.rvDemoPersonal.layoutManager = LinearLayoutManager(context)
        binding.rvDemoPersonal.adapter = adapter

        binding.chipGroupEstado.setOnCheckedStateChangeListener { _, checkedIds ->
            val checked = checkedIds.firstOrNull()
            filtroEstado = when (checked) {
                R.id.chipEstadoEnTurno -> FiltroEstado.EN_TURNO
                R.id.chipEstadoFuera -> FiltroEstado.FUERA_TURNO
                else -> FiltroEstado.TODOS
            }
            applyFilters()
        }
    }

    fun show() {
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun isShowing(): Boolean = dialog.isShowing

    fun render(items: List<DemoPersonalItem>, isLoading: Boolean, errorMessage: String?) {
        allItems = items.filter { it.categoria == categoria }

        binding.progressPicker.visibility = if (isLoading && allItems.isEmpty()) View.VISIBLE else View.GONE

        if (!errorMessage.isNullOrBlank() && allItems.isEmpty() && !isLoading) {
            binding.tvPickerEmpty.visibility = View.VISIBLE
            binding.tvPickerEmpty.text = errorMessage
            binding.rvDemoPersonal.visibility = View.GONE
            return
        }

        rebuildSubgroupChipsIfNeeded()
        applyFilters()
    }

    private fun rebuildSubgroupChipsIfNeeded() {
        val subgrupos = allItems.map { it.subgrupo }.distinct().sorted()
        if (subgrupos == subgruposBuilt) return
        subgruposBuilt = subgrupos

        val group = binding.chipGroupSubgrupo
        group.removeAllViews()

        val ctx = group.context
        val chipTodos = Chip(ctx, null, com.google.android.material.R.attr.chipStyle).apply {
            id = View.generateViewId()
            text = if (categoria == "MEDICO") "Todas las especialidades" else "Todos"
            isCheckable = true
            isChecked = filtroSubgrupo == null
            textSize = 12f
            setOnClickListener {
                filtroSubgrupo = null
                applyFilters()
            }
        }
        group.addView(chipTodos)

        subgrupos.forEach { sub ->
            val chip = Chip(ctx, null, com.google.android.material.R.attr.chipStyle).apply {
                id = View.generateViewId()
                text = sub
                isCheckable = true
                isChecked = filtroSubgrupo == sub
                textSize = 12f
                setOnClickListener {
                    filtroSubgrupo = sub
                    applyFilters()
                }
            }
            group.addView(chip)
        }
    }

    private fun applyFilters() {
        val filtered = allItems.filter { item ->
            val matchEstado = when (filtroEstado) {
                FiltroEstado.TODOS -> true
                FiltroEstado.EN_TURNO -> item.enTurno
                FiltroEstado.FUERA_TURNO -> !item.enTurno
            }
            val matchSubgrupo = filtroSubgrupo == null || item.subgrupo == filtroSubgrupo
            matchEstado && matchSubgrupo
        }

        adapter.submitList(filtered)
        val showEmpty = filtered.isEmpty() && binding.progressPicker.visibility != View.VISIBLE
        binding.tvPickerEmpty.visibility = if (showEmpty) View.VISIBLE else View.GONE
        if (showEmpty) {
            binding.tvPickerEmpty.text = "No hay personal en este filtro."
        }
        binding.rvDemoPersonal.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private class DemoPersonalAdapter(
        private val onClick: (DemoPersonalItem) -> Unit
    ) : RecyclerView.Adapter<DemoPersonalAdapter.VH>() {

        private var items: List<DemoPersonalItem> = emptyList()

        fun submitList(newItems: List<DemoPersonalItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemDemoPersonalBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VH(binding, onClick)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class VH(
            private val binding: ItemDemoPersonalBinding,
            private val onClick: (DemoPersonalItem) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: DemoPersonalItem) {
                val ctx = binding.root.context
                binding.tvItemNombre.text = item.nombreCompleto
                binding.tvItemMeta.text = "${item.subgrupo} • @${item.usuario}"

                val badgeBg = GradientDrawable().apply {
                    cornerRadius = 999f
                }

                if (item.enTurno) {
                    badgeBg.setColor(ContextCompat.getColor(ctx, R.color.triage_estable_bg))
                    badgeBg.setStroke(2, ContextCompat.getColor(ctx, R.color.triage_estable_border))
                    binding.tvItemEstadoBadge.background = badgeBg
                    binding.tvItemEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.triage_estable_text))
                    binding.tvItemEstadoBadge.text = "● En turno"
                    binding.tvItemHorario.text = listOfNotNull(item.horarioTurno, item.areaTurno)
                        .joinToString(" · ")
                        .ifBlank { "Turno activo" }
                } else {
                    badgeBg.setColor(ContextCompat.getColor(ctx, R.color.cmo_surface))
                    badgeBg.setStroke(2, ContextCompat.getColor(ctx, R.color.cmo_border_strong))
                    binding.tvItemEstadoBadge.background = badgeBg
                    binding.tvItemEstadoBadge.setTextColor(ContextCompat.getColor(ctx, R.color.cmo_text_muted))
                    binding.tvItemEstadoBadge.text = "○ Fuera de turno"
                    binding.tvItemHorario.text = item.horarioTurno ?: "Sin turno ahora"
                }

                binding.root.setOnClickListener { onClick(item) }
            }
        }
    }
}
