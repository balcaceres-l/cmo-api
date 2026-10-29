package com.cmo.hospital.components

import android.view.View
import android.widget.PopupMenu
import com.cmo.hospital.R
import com.cmo.hospital.core.domain.model.User
import com.cmo.hospital.databinding.CompHucTopBarBinding

object HucTopBarBinder {
    fun bind(
        binding: CompHucTopBarBinding,
        moduleSubtitle: String,
        currentUser: User?,
        guardiaLabel: String = "Guardia Activa",
        secondaryIconRes: Int? = null,
        onSecondaryClick: (() -> Unit)? = null,
        onGuardiaClick: (() -> Unit)? = null,
        onLogoutClick: () -> Unit
    ) {
        binding.tvModuleSubtitle.text = moduleSubtitle
        binding.tvGuardiaLabel.text = guardiaLabel

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
                val nombre = currentUser?.nombreCompleto ?: "Personal HUC"
                val cargo = currentUser?.cargo ?: currentUser?.rol ?: "Turno Activo"
                popup.menu.add(0, 1, 0, "● $nombre ($cargo)").isEnabled = false
                popup.menu.add(0, 2, 1, "Turno Matutino 07:00 - 15:00").isEnabled = false
                popup.show()
            }
        }

        binding.btnProfileAction.setOnClickListener {
            val popup = PopupMenu(binding.root.context, binding.btnProfileAction)
            val nombre = currentUser?.nombreCompleto ?: "Usuario HUC"
            val rol = currentUser?.rol ?: "OPERADOR"
            popup.menu.add(0, 1, 0, "$nombre • $rol").isEnabled = false
            popup.menu.add(0, 2, 1, "Cerrar sesión Sanctum")
            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == 2) {
                    onLogoutClick()
                    true
                } else false
            }
            popup.show()
        }
    }

    fun updateSubtitle(binding: CompHucTopBarBinding, subtitle: String) {
        binding.tvModuleSubtitle.text = subtitle
    }
}
