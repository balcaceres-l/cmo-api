package com.cmo.hospital.components

import android.view.View
import android.widget.PopupMenu
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.databinding.CompHucTopBarBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object HucTopBarBinder {
    fun bind(
        binding: CompHucTopBarBinding,
        moduleSubtitle: String,
        currentUser: User?,
        guardiaLabel: String = "En Turno",
        secondaryIconRes: Int? = null,
        onSecondaryClick: (() -> Unit)? = null,
        onGuardiaClick: (() -> Unit)? = null,
        onLogoutClick: () -> Unit
    ) {
        binding.tvModuleSubtitle.text = moduleSubtitle
        binding.tvGuardiaLabel.text = guardiaLabel

        val cargoLabel = currentUser?.cargoPerfil?.etiquetaBadge?.uppercase()
            ?: currentUser?.cargo?.uppercase()
            ?: "CMO"
        binding.tvBrandCmo.text = cargoLabel

        val inicial = currentUser?.nombreCompleto
            ?.trim()
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "U"
        binding.tvProfileInitial.text = inicial

        if (secondaryIconRes != null && onSecondaryClick != null) {
            binding.btnSecondaryAction.visibility = View.VISIBLE
            binding.ivSecondaryAction.setImageResource(secondaryIconRes)
            binding.btnSecondaryAction.setOnClickListener { onSecondaryClick() }
        } else {
            binding.btnSecondaryAction.visibility = View.GONE
        }

        binding.btnGuardiaPill.setOnClickListener {
            if (onGuardiaClick != null) {
                onGuardiaClick()
            } else {
                val popup = PopupMenu(binding.root.context, binding.btnGuardiaPill)
                val nombre = currentUser?.nombreCompleto ?: "Personal CMO"
                val cargo = currentUser?.cargo ?: currentUser?.cargoPerfil?.etiquetaBadge ?: "Turno Activo"
                popup.menu.add(0, 1, 0, "● $nombre ($cargo)").isEnabled = false
                popup.menu.add(0, 2, 1, "Hospital CMO • Sesión Sanctum Activa").isEnabled = false
                popup.show()
            }
        }

        binding.btnProfileAction.setOnClickListener {
            val ctx = binding.root.context
            val nombre = currentUser?.nombreCompleto ?: "Usuario CMO"
            val cargo = currentUser?.cargo ?: currentUser?.cargoPerfil?.etiquetaBadge ?: "Personal"
            val rol = currentUser?.rol ?: "OPERADOR"
            MaterialAlertDialogBuilder(ctx)
                .setTitle(nombre)
                .setMessage("Cargo asignado: $cargo\nRol base: $rol\n\n¿Deseas cerrar tu sesión activa en Hospital CMO?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    onLogoutClick()
                }
                .show()
        }
    }

    fun updateSubtitle(binding: CompHucTopBarBinding, subtitle: String) {
        binding.tvModuleSubtitle.text = subtitle
    }
}
