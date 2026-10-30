package com.cmo.hospital.features.doctor.consulta.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.CatalogoClinico
import com.cmo.hospital.databinding.DialogAgregarCie10Binding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AgregarCie10DialogFragment : DialogFragment() {

    var onSeleccionarCie10: ((codigo: String, descripcion: String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAgregarCie10Binding.inflate(LayoutInflater.from(requireContext()))
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density

        fun renderizarCatalogo(query: String) {
            binding.containerListaCie10.removeAllViews()
            val q = query.trim()
            val filtrados = CatalogoClinico.CATALOGO_CIE10.filter {
                q.isBlank() ||
                    it.codigo.contains(q, ignoreCase = true) ||
                    it.descripcion.contains(q, ignoreCase = true)
            }
            filtrados.forEach { cie ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.bg_surface_low_16)
                    val pad = (12 * density).toInt()
                    setPadding(pad, pad, pad, pad)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * density).toInt()
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        onSeleccionarCie10?.invoke(cie.codigo, cie.descripcion)
                        dismiss()
                    }
                }

                val badge = TextView(ctx).apply {
                    text = cie.codigo
                    setBackgroundResource(R.drawable.bg_pill_primary_container)
                    setTextColor(ContextCompat.getColor(ctx, R.color.huc_primary_fixed))
                    textSize = 12f
                    val ph = (10 * density).toInt()
                    val pv = (4 * density).toInt()
                    setPadding(ph, pv, ph, pv)
                }

                val desc = TextView(ctx).apply {
                    text = cie.descripcion
                    setTextColor(ContextCompat.getColor(ctx, R.color.huc_on_surface))
                    textSize = 13.5f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginStart = (10 * density).toInt()
                    }
                }

                row.addView(badge)
                row.addView(desc)
                binding.containerListaCie10.addView(row)
            }
        }

        renderizarCatalogo("")
        binding.etFiltroCie10.doAfterTextChanged { editable ->
            renderizarCatalogo(editable?.toString().orEmpty())
        }
        binding.btnCerrarCie10.setOnClickListener { dismiss() }

        return MaterialAlertDialogBuilder(ctx)
            .setView(binding.root)
            .create()
    }
}
